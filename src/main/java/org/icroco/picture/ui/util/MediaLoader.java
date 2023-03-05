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
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class MediaLoader {
    private final TagRepository                       tagRepository;
    private final ThumbnailRepository                 thumbnailRepository;
    public static Image                               LOADING   = loadImage(MediaLoader.class.getResource("/images/loading-icon-png-9.jpg"));
    private final IThumbnailGenerator                 thumbnailGenerator;
    @Qualifier(ImageInConfiguration.THUMBNAILS)
    private final Cache                               thumbnailCache;
    private final TaskService                         taskService;
    private final PersistenceService                  persistenceService;
    private final Map<Long, CompletableFuture<Image>> taskCache = new ConcurrentHashMap<>();


    //    @Cacheable(cacheNames = ImageInConfiguration.THUMBNAILS, key = "#id", unless = "#result == null")
    private Thumbnail loadThumbnail(long id, Path p) {
        try {
            return thumbnailGenerator.generate(p, IThumbnailGenerator.DEFAULT_THUMB_SIZE);
        }
        catch (Exception e) {
            log.error("invalid file: '{}'", p, e);
        }
        // TODO: Add better error management if cannot read the image
        return null;
    }

    private Thumbnail extractThumbnail(MediaFile mf) {
        try {
            var thumbnail = Optional.ofNullable(thumbnailGenerator.extractThumbnail(mf.getFullPath()))
                                    .orElseGet(() -> thumbnailGenerator.generate(mf.getFullPath(), IThumbnailGenerator.DEFAULT_THUMB_SIZE));
            thumbnail.setId(mf.getId());
            return thumbnail;
        }
        catch (Exception e) {
            log.error("invalid file: '{}'", mf, e);
        }
        // TODO: Add better error management if cannot read the image
//        return Thumbnail.builder()
//                .id(mf)
//                .build();
        return null;
    }

    public void loadThumbnailFromFx(final MediaFile file) {
//        log.info("Load without cache file: '{}'", file.getFullPath());
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
                                              var now     = LocalDate.now();

                                              return persistenceService.saveOrUpdate(Thumbnail.builder().id(file.id())
                                                                                                        .fullPath(file.fullPath())
                                                                                                        .image(caption.getImage())
                                                                                                        .hashDate(now)
                                                                                                        .lastAccess(now)
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
            log.error("invalid file: {}", p, e);
        }
        return null;
    }

    @EventListener(ExtractThumbnailEvent.class)
    public void extractThumbnails(ExtractThumbnailEvent event) {
        var mediaFiles = List.copyOf(event.getCatalog().medias());
        log.info("Extract thumbnail, nbEntries: {}", mediaFiles.size());
        if (event.getCatalog().medias().isEmpty()) {
            log.warn("Trying to generate thumbnail for empty list of files: {}", event.getCatalog());
            return;
        }

        final var start = System.currentTimeMillis();
        var futures = StreamEx.ofSubLists(mediaFiles, Math.max(1, mediaFiles.size() / Constant.NB_CORE))
                              .map(p -> taskService.supply(thumbnailBatchExtraction(event.getCatalog(), p)))
                              .toArray(new CompletableFuture[0]);

        CompletableFuture.allOf(futures)
                         .thenAccept(unused -> log.info("Scanning catalog for thumbnail extraction: '{}' took: {} ",
                                                        event.getCatalog().path(),
                                                        AmountFormats.wordBased(Duration.ofMillis(System.currentTimeMillis() - start), Locale.getDefault())))
                         .thenAccept(u -> warmThumbnailCache(new WarmThumbnailCacheEvent(event.getCatalog(), this)))
                         .thenAccept(u -> taskService.notifyLater(new CatalogEvent(event.getCatalog(), CatalogEvent.EventType.READY, this)))
                         .thenAcceptAsync(unused -> taskService.notifyLater(new GenerateThumbnailEvent(event.getCatalog(), this)))
//                         .thenAcceptAsync(u -> taskService.notifyLater(new CatalogEvent(event.getCatalog(), CatalogEvent.EventType.READY, this)))
//        taskService                         .thenAcceptAsync(unused -> taskService.notifyLater(new GenerateThumbnailEvent(event.getCatalog(), this)))

        ;
    }

    @EventListener(WarmThumbnailCacheEvent.class)
    public void warmThumbnailCache(final WarmThumbnailCacheEvent event) {
        var mediaFiles = List.copyOf(event.getCatalog().medias());

        var futures = StreamEx.ofSubLists(mediaFiles, Math.max(1, mediaFiles.size() / Constant.NB_CORE))
                              .map(files -> taskService.supply(warmCache(event.getCatalog(), files)))
                              .toArray(new CompletableFuture[0]);

        CompletableFuture.allOf(futures)
                         .thenAccept(unused -> taskService.notifyLater(new CatalogEvent(event.getCatalog(), CatalogEvent.EventType.SELECTED, this)))
        ;
    }

    Task<Void> warmCache(Catalog catalog, final List<MediaFile> files) {
        return new AbstractTask<>() {
            @Override
            protected Void call() throws Exception {
                log.info("Warm Cache for: '{}', size: {}", catalog.path(), files.size());
                updateTitle("Warm thumbnails cache");
                updateProgress(0, files.size());
                EntryStream.of(new ArrayList<>(files))
                           .forEach(entry -> {
                               var thumbnail = thumbnailCache.get(entry.getValue().id(), Thumbnail.class);
                               if (thumbnail == null) {
                                   thumbnail = persistenceService.findByPathOrId(entry.getValue()).orElse(null);
                                   if (thumbnail != null) {
                                       thumbnailCache.put(entry.getValue().id(), thumbnail);
                                       updateMessage("Thumbnail loaded: " + entry.getValue().fullPath().getFileName());
                                   }
                                   updateProgress(entry.getKey(), files.size());
                               }
                           });
                return null;
            }
        };
    }

    record ThumbnailRes(MediaFile file, Thumbnail thumbnail, boolean requirePersistance) {
        void updateThumbnail() {
            if (!Platform.isFxApplicationThread()) {
                throw new RuntimeException("This call must be done from JavaFx Thread (and not from: " + Thread.currentThread().getName() + ")");
            }
            file.getThumbnail().set(thumbnail);
        }
    }

    Task<List<Thumbnail>> thumbnailBatchExtraction(Catalog catalog, final List<MediaFile> files) {
        return new AbstractTask<>() {
            //            StopWatch w = new StopWatch("Load Task");
            @Override
            protected List<Thumbnail> call() throws Exception {
                var size = files.size();
                log.debug("Generate a batch of '{}' thumbnails for: {}", size, catalog.path());
                updateTitle("Thumbnail fast extraction for '" + size + " images");
                updateProgress(0, size);
                final LocalDate now = LocalDate.now();
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
                                            return trs.stream().map(tagRepository -> tagRepository.thumbnail);
                                        }
                               )
                               .toList();
            }

            @Override
            protected void succeeded() {
                getValue().forEach(Thumbnail::initImageFromFx);
            }
        };
    }

    record ThumbnailUpdate(Thumbnail thumbnail, Image update) {}

    @EventListener(GenerateThumbnailEvent.class)
    public void generateThumbnails(GenerateThumbnailEvent event) {
        var mediaFiles = List.copyOf(event.getCatalog().medias());
        log.info("Generate high quality thumbnail, nbEntries: {}", mediaFiles.size());
        if (event.getCatalog().medias().isEmpty()) {
            return;
        }

        StreamEx.ofSubLists(mediaFiles, 50)
                .map(p -> taskService.supply(thumbnailBatchGeneration(event.getCatalog(), p)))
                .toArray(new CompletableFuture[0]);
    }

    Task<List<ThumbnailUpdate>> thumbnailBatchGeneration(Catalog catalog, final List<MediaFile> files) {
        return new AbstractTask<>() {
            //            StopWatch w = new StopWatch("Load Task");
            @Override
            protected List<ThumbnailUpdate> call() throws Exception {
                var size = files.size();
                log.debug("Generate a batch of '{}' thumbnails for: {}", size, catalog.path());
                updateTitle("Thumbnail high quality generation for '" + size + " images");
                updateProgress(0, size);
                final LocalDate now = LocalDate.now();
                return StreamEx.ofSubLists(EntryStream.of(files).toList(), 20)
                               .map(ts -> ts.stream()
                                            .map(mf -> {
                                                var file = mf.getValue();
                                                updateProgress(mf.getKey(), size);
                                                return Optional.ofNullable(thumbnailCache.get(file.id(), Thumbnail.class))
                                                               .or(() -> persistenceService.findByPathOrId(file))
                                                               .filter(t -> t.getOrigin() != EThumbnailType.GENERATED)
                                                               .map(t -> new ThumbnailUpdate(t, thumbnailGenerator.generate(t)))
                                                               .orElse(null);
                                            })
                                            .filter(Objects::nonNull)
                                            .filter(tr -> tr.thumbnail() != null)
                                            .toList())
                               .flatMap(thumbails -> {
                                            log.info("Update {} thumbnails", thumbails.size());
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
                });
//                log.info("End of thumbnails high quality generation");
            }
        };
    }
}
