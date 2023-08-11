package org.icroco.picture.ui.util;

import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.icroco.picture.ui.config.ImageInConfiguration;
import org.icroco.picture.ui.event.*;
import org.icroco.picture.ui.model.EThumbnailType;
import org.icroco.picture.ui.model.MediaCollection;
import org.icroco.picture.ui.model.MediaFile;
import org.icroco.picture.ui.model.Thumbnail;
import org.icroco.picture.ui.model.mapper.ThumbnailMapper;
import org.icroco.picture.ui.persistence.PersistenceService;
import org.icroco.picture.ui.persistence.TagRepository;
import org.icroco.picture.ui.persistence.ThumbnailRepository;
import org.icroco.picture.ui.task.AbstractTask;
import org.icroco.picture.ui.task.TaskService;
import org.icroco.picture.ui.util.image.ImageLoader;
import org.icroco.picture.ui.util.thumbnail.IThumbnailGenerator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.threeten.extra.AmountFormats;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
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
    public static double              PRIMARY_SCREEN_WIDTH = Screen.getPrimary().getVisualBounds().getWidth();
    private final IThumbnailGenerator thumbnailGenerator;
    private final ImageLoader         imageLoader;
    @Qualifier(ImageInConfiguration.THUMBNAILS)
    private final Cache               thCache;
    private final TaskService         taskService;
    private final PersistenceService  persistenceService;
    private final Map<Integer, Lock>  catalogLock          = new ConcurrentHashMap<>();
    private final Set<Integer>        catalogToReGenerate  = new CopyOnWriteArraySet<>();
//    private final LRUCache<MediaFile, Thumbnail> lruCache = new LRUCache<>(1000);

    public Optional<Thumbnail> getCachedValue(MediaFile mf) {
        return Optional.ofNullable(thCache.get(mf, Thumbnail.class));
//                       .or(() -> {
//                           log.debug("thumbnailCache missed for: {}:'{}', size: '{}'",
//                                     mf.getId(),
//                                     mf.getFullPath().getFileName(),
//                                     ((com.github.benmanes.caffeine.cache.Cache<?, ?>) thumbnailCache.getNativeCache()).estimatedSize()
//                           );
//                           return persistenceService.findByPathOrId(mf)
//                                                    .map(t -> {
//                                                        thumbnailCache.put(mf, t);
//                                                        return t;
//                                                    });
//                       });
    }

    public void loadAndCachedValue(final MediaFile mf) {
        Task<Optional<Thumbnail>> t = new AbstractTask<>() {
            @Override
            protected Optional<Thumbnail> call() {
                return getCachedValue(mf).or(() -> {
//                                             log.debug("thumbnailCache missed for: {}:'{}', size: '{}'",
//                                                       mf.getId(),
//                                                       mf.getFullPath().getFileName(),
//                                                       ((com.github.benmanes.caffeine.cache.Cache<?, ?>) thCache.getNativeCache()).estimatedSize()
//                                             );
                    return persistenceService.findByPathOrId(mf)
                                             .map(t -> {
                                                 thCache.put(mf, t);
                                                 return t;
                                             });
                });
            }
        };
        t.setOnSucceeded(unused -> t.getValue().ifPresentOrElse(u -> mf.setLoadedInCahce(true), () -> mf.setLoadedInCahce(false)));
        t.setOnFailed(unused -> t.getValue().ifPresent(u -> mf.setLoadedInCahce(false)));
        t.setOnCancelled(unused -> t.getValue().ifPresent(u -> mf.setLoadedInCahce(false)));
        taskService.supply(t, true);
    }


    private Thumbnail extractThumbnail(MediaFile mf) {
        try {
            var thumbnail = Optional.ofNullable(thumbnailGenerator.extractThumbnail(mf.getFullPath()))
                                    .orElseGet(() -> Thumbnail.builder()
                                            .mfId(mf.getId())
                                            .fullPath(mf.getFullPath())
                                            .image(LOADING)
                                            .origin(EThumbnailType.ABSENT)
                                            .lastUpdate(LocalDateTime.now())
                                            .build());
//                                    .orElseGet(() -> thumbnailGenerator.generate(mf.getFullPath(), IThumbnailGenerator.DEFAULT_THUMB_SIZE));
            thumbnail.setMfId(mf.getId());
            return thumbnail;
        }
        catch (Exception e) {
            log.error("invalid mf: '{}'", mf, e);
        }
        // TODO: Add better error management if cannot read the image
        return null;
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
    public Image loadImage(MediaFile mediaFile) {
        if (mediaFile == null) {
            return null;
        }

        try {
            return imageLoader.loadImage(mediaFile);
        }
        catch (Exception e) {
            log.error("invalid mf: {}", mediaFile.getFullPath(), e);
        }
        return null;
    }

    @EventListener(WarmThumbnailCacheEvent.class)
    public void warmThumbnailCache(final WarmThumbnailCacheEvent event) {
        var catalog = event.getMediaCollection();
        log.info("warmThumbnailCache for '{}', size: {}", event.getMediaCollection().path(), event.getMediaCollection().medias().size());
//        var mediaFiles = List.copyOf(catalog.medias());
        taskService.sendFxEvent(new CollectionEvent(catalog, CollectionEvent.EventType.SELECTED, this));

//        var mediaFiles = List.copyOf(catalog.medias().stream().sorted(Comparator.comparing(MediaFile::getOriginalDate)).limit(500).toList());
//
//        var futures = StreamEx.ofSubLists(mediaFiles, Constant.split(mediaFiles.size()))
//                              .map(files -> taskService.supply(warmCache(catalog, files)))
//                              .toArray(new CompletableFuture[0]);
//
//        CompletableFuture.allOf(futures)
//                         .thenAcceptAsync(unused -> taskService.senfEventIntoFx(new CollectionEvent(catalog, CollectionEvent.EventType.SELECTED, this)))
//                         .thenAcceptAsync(u -> lockThenRunOrSkip(catalog, () -> mayRegenerateThubmnail(catalog)));
////        taskService.fxNotifyLater(new CollectionEvent(catalog, CollectionEvent.EventType.SELECTED, this));
    }

    Task<List<ThumbnailRes>> warmCache(MediaCollection mediaCollection, final List<MediaFile> files) {
        return new AbstractTask<>() {
            @Override
            protected List<ThumbnailRes> call() throws Exception {
                log.debug("Warm Cache for: '{}', size: {}", mediaCollection.path(), files.size());
                updateTitle("Loading thumbnails ...");
                updateProgress(0, files.size());
                return EntryStream.of(new ArrayList<>(files))
                                  .map(entry -> {
                                      var thumbnail = thCache.get(entry.getValue(), Thumbnail.class);
                                      if (thumbnail == null) {
                                          thumbnail = persistenceService.findByPathOrId(entry.getValue()).orElse(null);
                                          if (thumbnail != null) {
                                              thCache.put(entry.getValue(), thumbnail);
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
                getValue().forEach(thumbnailRes -> {
                    thumbnailRes.mf.getThumbnailUpdateProperty().set(thumbnailRes.thumbnail.getLastUpdate());
                });
            }
        };
    }

    private <R> R lockThenRunOrSkip(MediaCollection mediaCollection, Supplier<R> callable) {
        var lock = catalogLock.computeIfAbsent(mediaCollection.id(), integer -> new ReentrantLock());

        try {
            if (lock.tryLock(1, TimeUnit.SECONDS)) {
                return callable.get();
            }
        }
        catch (InterruptedException e) {
            log.error("Cannot acquire lock for mediaCollection: '{}'", mediaCollection.path(), e);
        }
        finally {
            lock.unlock();
        }
        return null;
    }

    Void mayRegenerateThubmnail(MediaCollection mediaCollection) {
        if (catalogToReGenerate.contains(mediaCollection.id())) {
            taskService.sendFxEvent(new GenerateThumbnailEvent(mediaCollection, this));
            catalogToReGenerate.remove(mediaCollection.id());
        }
        return null;
    }

    public void removeFromCache(MediaFile oldValue) {
        if (oldValue != null) {
            thCache.evict(oldValue);
        }
    }

    record ThumbnailRes(MediaFile mf, Thumbnail thumbnail, boolean requirePersistance) {
    }

    @EventListener(ExtractThumbnailEvent.class)
    public void extractThumbnails(ExtractThumbnailEvent event) {
        extractThumbnails(event.getMediaCollection(), List.copyOf(event.getMediaCollection().medias()), true);
    }

    private void extractThumbnails(MediaCollection mediaCollection, List<MediaFile> mediaFiles, boolean sendReadyEvent) {
        final var start = System.currentTimeMillis();

        log.info("Extract thumbnail, nbEntries: {}", mediaFiles.size());
        if (mediaCollection.medias().isEmpty()) {
            log.warn("Trying to generate thumbnail for empty list of files: {}", mediaCollection);
            return;
        }

        final var batches = Collections.splitByCoreWithIdx(mediaFiles);
        var futures = batches.values()
                             .map(e -> taskService.supply(thumbnailBatchExtraction(mediaCollection, batches.splitCount(), e)))
                             .toArray(new CompletableFuture[0]);

        CompletableFuture.allOf(futures)
                         .thenAcceptAsync(unused -> log.info("Thumbnail extraction finished for '{}', '{}', files, it took: '{}'",
                                                             mediaCollection.path(),
                                                             mediaFiles.size(),
                                                             AmountFormats.wordBased(Duration.ofMillis(System.currentTimeMillis() - start), Locale.getDefault())
                         ))
                         .thenAcceptAsync(u -> catalogToReGenerate.add(mediaCollection.id()))
                         .thenAcceptAsync(u -> taskService.sendFxEvent(new GalleryRefreshEvent(mediaCollection.id(), this)))
                         .thenAcceptAsync(u -> {
//                             if (sendReadyEvent) {
                                 taskService.sendFxEvent(new CollectionEvent(persistenceService.getMediaCollection(mediaCollection.id()),
                                                                             CollectionEvent.EventType.READY,
                                                                             this));
//                             }
                         })
                         .thenAcceptAsync(u -> taskService.sendEvent(new GenerateThumbnailEvent(mediaCollection, this)))
        ;
    }

    Task<List<MediaFile>> thumbnailBatchExtraction(MediaCollection mediaCollection,
                                                   final int nbBatches,
                                                   Map.Entry<Integer, List<MediaFile>> e) {
        return new AbstractTask<>() {
            //            StopWatch w = new StopWatch("Load Task");
            @Override
            protected List<MediaFile> call() {
                var files = e.getValue();
                var size  = files.size();
                log.debug("thumbnailBatchExtraction a batch of '{}' thumbnails for: {}", size, mediaCollection.path());
                updateTitle("Thumbnail fast extraction for '%s' image, batch: %d/%d".formatted(size, e.getKey(), nbBatches));
                updateProgress(0, size);
                return StreamEx.ofSubLists(EntryStream.of(files).toList(), 20)
                               .map(ts -> ts.stream()
                                            .map(mf -> {
                                                // First Memory Cache
                                                var file = mf.getValue();
                                                updateProgress(mf.getKey(), size);
                                                updateMessage("Extract thumbnail from: " + file.getFullPath().getFileName());
                                                log.debug("Extract thumbnail from: '{}'", file.getFullPath().getFileName());
                                                var thumbnail = thCache.get(file, Thumbnail.class);
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
                                            persistenceService.saveAll(toBePersisted, EThumbnailType.EXTRACTED);
                                   return trs.stream().map(tr -> tr.mf);
                                        }
                               )
                               .toList();
            }
        };
    }

    record ThumbnailUpdate(MediaFile mf, Thumbnail thumbnail, Image update) {}

    @EventListener(GenerateThumbnailEvent.class)
    public void generateThumbnails(GenerateThumbnailEvent event) {
        var catalog = event.getMediaCollection();
        generateThumbnails(catalog.medias(), catalog);
    }

    private void generateThumbnails(Collection<MediaFile> mediaFiles, @Nullable MediaCollection mediaCollection) {
        final var start = System.currentTimeMillis();
        final var mfFiltered = List.copyOf(mediaFiles.stream().filter(mf -> Optional.ofNullable(thCache.get(mf, Thumbnail.class))
                                                                                    .map(Thumbnail::getOrigin)
                                                                                    .orElse(EThumbnailType.ABSENT)
                                                                            != EThumbnailType.GENERATED).toList());

        log.info("Generate high quality thumbnail, nbEntries: {}", mfFiltered.size());
        if (mfFiltered.isEmpty()) {
            return;
        }

        final var batches = Collections.splitByCoreWithIdx(mfFiltered);
        var futures = batches.values()
                             .map(e -> taskService.supply(thumbnailBatchGeneration(Optional.of(mediaCollection), e, batches.splitCount())))
                             .toArray(new CompletableFuture[0]);

        CompletableFuture.allOf(futures)
                         .thenAcceptAsync(u -> taskService.sendFxEvent(new GalleryRefreshEvent(mediaCollection.id(), this)))
                         .thenAcceptAsync(u -> log.info("Thumbnail generation finished for '{}', '{}', files, it took: '{}'",
                                                        mediaCollection.path(),
                                                        mediaFiles.size(),
                                                        AmountFormats.wordBased(Duration.ofMillis(System.currentTimeMillis() - start), Locale.getDefault())
                         ));
    }

    Task<List<MediaFile>> thumbnailBatchGeneration(Optional<MediaCollection> mediaCollection,
                                                   final Map.Entry<Integer, List<MediaFile>> files,
                                                   int nbBatches) {
        return new AbstractTask<>() {
            @Override
            protected List<MediaFile> call() throws Exception {
                var size = files.getValue().size();
                log.debug("thumbnailBatchGeneration a batch of '{}' thumbnails for: {}",
                          size,
                          mediaCollection.map(MediaCollection::path).map(Path::toString).orElse("Updates Auto-Detection"));
                updateTitle("Thumbnail high quality generation for '%s' images, batch %d/%d".formatted(size, files.getKey(), nbBatches));
                updateProgress(0, size);
                return StreamEx.ofSubLists(EntryStream.of(files.getValue()).toList(), 20)
                               .map(ts -> ts.stream()
                                            .map(mf -> {
                                                var file = mf.getValue();
                                                updateProgress(mf.getKey(), size);
                                                return Optional.ofNullable(thCache.get(file, Thumbnail.class))
                                                               .or(() -> persistenceService.findByPathOrId(file))
                                                               .filter(t -> t.getOrigin() != EThumbnailType.GENERATED)
                                                               .map(t -> new ThumbnailUpdate(file, t, thumbnailGenerator.generate(t)))
                                                               .orElse(null);
                                            })
                                            .filter(Objects::nonNull)
                                            .filter(tu -> tu.thumbnail() != null)
                                            .peek(tu -> tu.thumbnail.setImage(tu.update))
                                            .toList())
                               .flatMap(tu -> {
                                            log.debug("Update '{}' high res thumbnails", tu.size());
                                            persistenceService.saveAll(tu.stream()
                                                                         .map(ThumbnailUpdate::thumbnail)
                                                                         .toList(), EThumbnailType.GENERATED);
                                            return tu.stream().map(tr -> tr.mf);
                                        }
                               )
                               .toList();
            }
//
//            @Override
//            protected void succeeded() {
//                getValue().forEach(mf -> {
//                    var now = LocalDateTime.now();
//                    mf.getThumbnailUpdateProperty().set(now);
//                });
//            }
        };
    }

    @EventListener(CollectionUpdatedEvent.class)
    public void updateImages(CollectionUpdatedEvent event) {
        log.info("Recieved update on collection: '{}', newItems: '{}', deletedItems: '{}'",
                 event.getMediaCollectionId(),
                 event.getNewItems().size(),
                 event.getDeletedItems().size());

        if (!event.getNewItems().isEmpty()) {
            extractThumbnails(persistenceService.getMediaCollection(event.getMediaCollectionId()), List.copyOf(event.getNewItems()), false);
        }
    }

}
