package org.icroco.picture.ui.util;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.icroco.picture.ui.config.ImageInConfiguration;
import org.icroco.picture.ui.event.CatalogEvent;
import org.icroco.picture.ui.event.ExtractThumbnailEvent;
import org.icroco.picture.ui.event.GenerateThumbnailEvent;
import org.icroco.picture.ui.event.WarmThumbnailCacheEvent;
import org.icroco.picture.ui.model.Catalog;
import org.icroco.picture.ui.model.EThumbnailType;
import org.icroco.picture.ui.model.MediaFile;
import org.icroco.picture.ui.model.Thumbnail;
import org.icroco.picture.ui.model.mapper.ThumbnailMapper;
import org.icroco.picture.ui.persistence.PersistenceService;
import org.icroco.picture.ui.persistence.TagRepository;
import org.icroco.picture.ui.persistence.ThumbnailRepository;
import org.icroco.picture.ui.task.AbstractTask;
import org.icroco.picture.ui.task.TaskService;
import org.icroco.picture.ui.util.thumbnail.IThumbnailGenerator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.threeten.extra.AmountFormats;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
@Slf4j
public class MediaLoader {
    private final ThumbnailMapper     thumbnailMapper;
    private final TagRepository       tagRepository;
    private final ThumbnailRepository thumbnailRepository;
    public static Image               LOADING              = loadImage(MediaLoader.class.getResource("/images/loading-icon-png-9.jpg"));
    private final IThumbnailGenerator thumbnailGenerator;
    @Qualifier(ImageInConfiguration.THUMBNAILS)
    private final Cache               thumbnailCache;
    private final TaskService         taskService;
    private final PersistenceService  persistenceService;
    private final Map<Integer, Lock>  catalogLock          = new ConcurrentHashMap<>();
    private final Set<Integer>        catalogToReGenerated = new CopyOnWriteArraySet<>();

    private final ReentrantLock lock = new ReentrantLock();


    //    @Cacheable(cacheNames = ImageInConfiguration.THUMBNAILS, key = "#id", unless = "#result == null")
    private Thumbnail loadThumbnail(long id, Path p) {
        try {
            return thumbnailGenerator.generate(p, IThumbnailGenerator.DEFAULT_THUMB_SIZE);
        }
        catch (Exception e) {
            log.error("invalid mediaFile: '{}'", p, e);
        }
        // TODO: Add better error management if cannot read the image
        return null;
    }

    public Optional<Thumbnail> getCachedValue(MediaFile mf) {
        return Optional.ofNullable(thumbnailCache.get(mf.id(), Thumbnail.class))
                       .or(() -> persistenceService.findByPathOrId(mf)
                                                   .map(thumbnail -> {
                                                       thumbnailCache.put(mf.getId(), thumbnail);
                                                       return thumbnail;
                                                   }));
    }

    private Thumbnail extractThumbnail(MediaFile mf) {
        try {
            var thumbnail = Optional.ofNullable(thumbnailGenerator.extractThumbnail(mf.getFullPath()))
                                    .orElseGet(() -> thumbnailGenerator.generate(mf.getFullPath(), IThumbnailGenerator.DEFAULT_THUMB_SIZE));
            thumbnail.setId(mf.getId());
            return thumbnail;
        }
        catch (Exception e) {
            log.error("invalid mediaFile: '{}'", mf, e);
        }
        // TODO: Add better error management if cannot read the image
//        return Thumbnail.builder()
//                .id(mf)
//                .build();
        return null;
    }

    public void loadThumbnailFromFx(final MediaFile file) {
//        log.info("Load without cache mediaFile: '{}'", mediaFile.getFullPath());
        CompletableFuture.supplyAsync(() -> loadThumbnail(file))
                         .thenAccept(thumbnailRes -> Platform.runLater(thumbnailRes::updateThumbnail));
    }


    /**
     * This method must not be called from javafx thread
     */
    private ThumbnailRes loadThumbnail(final MediaFile file) {
        if (Platform.isFxApplicationThread()) { // TODO: Maybe remove this check later for performance issue.
            throw new RuntimeException("Invalid method call (called into JavaFx Thread, must not)");
        }
        var thumbnail = thumbnailCache.get(file.id(), Thumbnail.class);
        if (thumbnail == null) {
            thumbnail = persistenceService.findByPathOrId(file)
                                          .orElseGet(() -> {
                                              log.debug("No db cached for: '{}'", file.fullPath());
                                              var caption = loadThumbnail(file.getId(), file.getFullPath());

                                              return persistenceService.saveOrUpdate(Thumbnail.builder().id(file.id())
                                                                                                        .fullPath(file.fullPath())
                                                                                                        .image(caption.getImage())
                                                                                                        .origin(EThumbnailType.GENERATED)
                                                                                                        .build());
                                          });
            thumbnailCache.put(file.id(), thumbnail);
        }
        return new ThumbnailRes(file, thumbnail, false);
    }


    private static Image loadImage(URL url) {
        try {
            return new Image(url.toURI().toString(), 64, 64, true, true);
        }
        catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Cacheable(cacheNames = ImageInConfiguration.FULL_SIZE, unless = "#result == null")
    private Image loadImage(Path p) {
        try {
            return new Image(p.toUri().toString(), 1024D, 0, true, true);
        }
        catch (Exception e) {
            log.error("invalid mediaFile: {}", p, e);
        }
        return null;
    }

    @EventListener(ExtractThumbnailEvent.class)
    public void extractThumbnails(ExtractThumbnailEvent event) {
        var       catalog    = event.getCatalog();
        var       mediaFiles = List.copyOf(catalog.medias());
        final var start      = System.currentTimeMillis();

        log.info("Extract thumbnail, nbEntries: {}", mediaFiles.size());
        if (catalog.medias().isEmpty()) {
            log.warn("Trying to generate thumbnail for empty list of files: {}", catalog);
            return;
        }

        var futures = StreamEx.ofSubLists(mediaFiles, Math.max(1, mediaFiles.size() / Constant.NB_CORE))
                              .map(p -> taskService.supply(thumbnailBatchExtraction(catalog, p)))
                              .toArray(new CompletableFuture[0]);

        CompletableFuture.allOf(futures)
                         .thenAccept(unused -> log.info("Scanning catalog for thumbnail extraction: '{}' took: {} ",
                                                        catalog.path(),
                                                        AmountFormats.wordBased(Duration.ofMillis(System.currentTimeMillis() - start), Locale.getDefault())))
                         .thenAccept(u -> catalogToReGenerated.add(catalog.id()))
                         .thenAccept(u -> taskService.notifyLater(new CatalogEvent(catalog, CatalogEvent.EventType.READY, this)))
        ;
    }

    @EventListener(WarmThumbnailCacheEvent.class)
    public void warmThumbnailCache(final WarmThumbnailCacheEvent event) {
//        var lock    = catalogLock.computeIfAbsent(catalog.id(), integer -> new ReentrantLock());
        var catalog    = event.getCatalog();
        var mediaFiles = List.copyOf(catalog.medias());
        var futures = StreamEx.ofSubLists(mediaFiles, Math.max(1, mediaFiles.size() / Constant.NB_CORE))
                              .map(files -> taskService.supply(warmCache(catalog, files)))
                              .toArray(new CompletableFuture[0]);

        CompletableFuture.allOf(futures)
                         .thenAcceptAsync(unused -> taskService.notifyLater(new CatalogEvent(catalog, CatalogEvent.EventType.SELECTED, this)))
                         .thenAcceptAsync(u -> lockThenRunOrSkip(catalog, () -> mayRegenerateThubmnail(catalog)))
        ;
    }

    private <R> R lockThenRunOrSkip(Catalog catalog, Supplier<R> callable) {
        var lock = catalogLock.computeIfAbsent(catalog.id(), integer -> new ReentrantLock());

        try {
            if (lock.tryLock(100, TimeUnit.MILLISECONDS)) {
                return callable.get();
            }
        }
        catch (InterruptedException e) {
            log.error("Cannot acquire lock for catalog: '{}'", catalog.path(), e);
        }
        finally {
            lock.unlock();
        }
        return null;
    }

    Void mayRegenerateThubmnail(Catalog catalog) {
        if (catalogToReGenerated.contains(catalog.id())) {
            taskService.notifyLater(new GenerateThumbnailEvent(catalog, this));
            catalogToReGenerated.remove(catalog.id());
        }
        return null;
    }


    Task<List<ThumbnailRes>> warmCache(Catalog catalog, final List<MediaFile> files) {
        return new AbstractTask<>() {
            @Override
            protected List<ThumbnailRes> call() throws Exception {
                log.info("Warm Cache for: '{}', size: {}", catalog.path(), files.size());
                updateTitle("Warm thumbnails cache");
                updateProgress(0, files.size());
                return EntryStream.of(new ArrayList<>(files))
                                  .map(entry -> {
                                      var thumbnail = thumbnailCache.get(entry.getValue().id(), Thumbnail.class);
                                      if (thumbnail == null) {
                                          thumbnail = persistenceService.findByPathOrId(entry.getValue()).orElse(null);
                                          if (thumbnail != null) {
                                              thumbnailCache.put(entry.getValue().id(), thumbnail);
                                              updateMessage("Thumbnail loaded: " + entry.getValue().fullPath().getFileName());
                                          }
                                          updateProgress(entry.getKey(), files.size());
                                      }
                                      return new ThumbnailRes(entry.getValue(), thumbnail, false);
                                  })
                                  .filter(tr -> tr.thumbnail() != null)
                                  .toList();
            }

            @Override
            protected void succeeded() {
                getValue().forEach(thumbnailRes -> thumbnailRes.mediaFile.getThumbnailType().set(thumbnailRes.thumbnail.getOrigin()));
            }
        };
    }

    record ThumbnailRes(MediaFile mediaFile, Thumbnail thumbnail, boolean requirePersistance) {
        void updateThumbnail() {
            if (!Platform.isFxApplicationThread()) {
                throw new RuntimeException("This call must be done from JavaFx Thread (and not from: " + Thread.currentThread().getName() + ")");
            }
//            mediaFile.getThumbnail().set(thumbnail);
        }
    }

    Task<List<ThumbnailRes>> thumbnailBatchExtraction(Catalog catalog, final List<MediaFile> files) {
        return new AbstractTask<>() {
            //            StopWatch w = new StopWatch("Load Task");
            @Override
            protected List<ThumbnailRes> call() throws Exception {
                var size = files.size();
                log.debug("Generate a batch of '{}' thumbnails for: {}", size, catalog.path());
                updateTitle("Thumbnail fast extraction for '" + size + " images");
                updateProgress(0, size);
                return StreamEx.ofSubLists(EntryStream.of(files).toList(), 20)
                               .map(ts -> ts.stream()
                                            .map(mf -> {
                                                // First Memory Cache
                                                var file = mf.getValue();
                                                updateProgress(mf.getKey(), size);
                                                var thumbnail = thumbnailCache.get(file.id(), Thumbnail.class);
                                                if (thumbnail == null) {
                                                    // Second Database Cache
                                                    thumbnail = persistenceService.findByPathOrId(file).orElse(null);
                                                    if (thumbnail == null) {
                                                        thumbnail = extractThumbnail(file);
                                                        return new ThumbnailRes(mf.getValue(), thumbnail, true);
                                                    } else {
                                                        return new ThumbnailRes(mf.getValue(), thumbnail, false);
                                                    }
                                                } else {
                                                    return new ThumbnailRes(mf.getValue(), thumbnail, false);
                                                }
                                            })
                                            .toList())
                               .flatMap(trs -> {
                                            var toBePersisted = trs.stream()
                                                                   .filter(tr -> tr.thumbnail != null)
                                                                   .filter(tr -> tr.thumbnail.getImage() != null)
                                                                   .filter(tr -> tr.requirePersistance)
                                                                   .map(ThumbnailRes::thumbnail)
                                                                   .toList();
                                            persistenceService.saveAll(toBePersisted);
                                   return trs.stream();
                                        }
                               )
                               .toList();
            }

            @Override
            protected void succeeded() {
                getValue().forEach(thumbnailRes -> thumbnailRes.mediaFile.getThumbnailType().set(thumbnailRes.thumbnail.getOrigin()));
            }
        };
    }

    record ThumbnailUpdate(MediaFile mf, Thumbnail thumbnail, Image update) {}

    @EventListener(GenerateThumbnailEvent.class)
    public void generateThumbnails(GenerateThumbnailEvent event) {
        var catalog    = event.getCatalog();
        var mediaFiles = List.copyOf(catalog.medias());

        log.info("Generate high quality thumbnail, nbEntries: {}", mediaFiles.size());
        if (catalog.medias().isEmpty()) {
            return;
        }

        StreamEx.ofSubLists(mediaFiles, 50)
                .map(p -> taskService.supply(thumbnailBatchGeneration(catalog, p)))
                .toArray(new CompletableFuture[0]);
    }

    Task<List<ThumbnailUpdate>> thumbnailBatchGeneration(Catalog catalog, final List<MediaFile> files) {
        return new AbstractTask<>() {
            @Override
            protected List<ThumbnailUpdate> call() throws Exception {
                var size = files.size();
                log.debug("Generate a batch of '{}' thumbnails for: {}", size, catalog.path());
                updateTitle("Thumbnail high quality generation for '" + size + " images");
                updateProgress(0, size);
                return StreamEx.ofSubLists(EntryStream.of(files).toList(), 20)
                               .map(ts -> ts.stream()
                                            .map(mf -> {
                                                var file = mf.getValue();
                                                updateProgress(mf.getKey(), size);
                                                return Optional.ofNullable(thumbnailCache.get(file.id(), Thumbnail.class))
                                                               .or(() -> persistenceService.findByPathOrId(file))
                                                               .filter(t -> t.getOrigin() != EThumbnailType.GENERATED)
                                                               .map(t -> new ThumbnailUpdate(file, t, thumbnailGenerator.generate(t)))
                                                               .orElse(null);
                                            })
                                            .filter(Objects::nonNull)
                                            .filter(tu -> tu.thumbnail() != null)
                                            .peek(tu -> {
                                                tu.thumbnail.setOrigin(EThumbnailType.GENERATED);
                                                tu.thumbnail.setImage(tu.update);
                                            })
                                            .toList())
                               .flatMap(thumbails -> {
                                            log.info("Update '{}' thumbnails", thumbails.size());
                                            persistenceService.saveAll(thumbails.stream()
                                                                                .map(ThumbnailUpdate::thumbnail)
                                                                                .toList());
                                            return thumbails.stream();
                                        }
                               )
                               .toList();
            }

            @Override
            protected void succeeded() {
                getValue().forEach(tu -> {
                    thumbnailCache.put(tu.thumbnail.getId(), tu.thumbnail);
                    tu.thumbnail().setImage(tu.update);
                    tu.mf.getThumbnailType().set(tu.thumbnail.getOrigin());
                });
            }
        };
    }
}
