package org.icroco.picture.views.util;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.icroco.picture.config.ImagInConfiguration;
import org.icroco.picture.event.*;
import org.icroco.picture.model.EThumbnailType;
import org.icroco.picture.model.MediaCollection;
import org.icroco.picture.model.MediaFile;
import org.icroco.picture.model.Thumbnail;
import org.icroco.picture.persistence.PersistenceService;
import org.icroco.picture.thumbnail.IThumbnailGenerator;
import org.icroco.picture.views.task.AbstractTask;
import org.icroco.picture.views.task.TaskService;
import org.icroco.picture.views.util.image.ImageLoader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.threeten.extra.AmountFormats;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

@Component
@Slf4j
public class MediaLoader {
    public static double PRIMARY_SCREEN_WIDTH = Screen.getPrimary().getVisualBounds().getWidth();

    private final IThumbnailGenerator thumbnailGenerator;
    private final ImageLoader         imageLoader;
    final         Cache               thCache;
    final         Cache               imagesCache;
    private final TaskService         taskService;
    private final PersistenceService  persistenceService;
    private final Map<Integer, Lock>  catalogLock         = new ConcurrentHashMap<>();
    private final Set<Integer>        catalogToReGenerate = new CopyOnWriteArraySet<>();
//    private final LRUCache<MediaFile, Thumbnail> lruCache = new LRUCache<>(1000);

    final Function<MediaFile, Thumbnail> cacheOrLoad;

    public MediaLoader(IThumbnailGenerator thumbnailGenerator,
                       ImageLoader imageLoader,
                       @Qualifier(ImagInConfiguration.CACHE_THUMBNAILS) Cache thCache,
                       @Qualifier(ImagInConfiguration.CACHE_IMAGE_FULL_SIZE) Cache imagesCache,
                       TaskService taskService,
                       PersistenceService persistenceService) {
        this.thumbnailGenerator = thumbnailGenerator;
        this.imageLoader = imageLoader;
        this.thCache = thCache;
        this.imagesCache = imagesCache;
        this.taskService = taskService;
        this.persistenceService = persistenceService;
        cacheOrLoad = mf -> getCachedValue(mf).orElseGet(() -> persistenceService.findByPathOrId(mf)
                                                                                 .map(t -> {
                                                                                     thCache.put(mf, t);
                                                                                     return t;
                                                                                 })
                                                                                 .orElseThrow());
    }

    public Optional<Thumbnail> getCachedValue(MediaFile mf) {
        return ofNullable(thCache.get(mf, Thumbnail.class));
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
        taskService.supply(cacheOrLoad, mf)
                   .thenAcceptAsync(t -> Platform.runLater(() -> mf.setLoadedInCache(true)))
                   .exceptionally(throwable -> {
                       Platform.runLater(() -> mf.setLoadedInCache(false));
                       return null;
                   });
//        Task<Optional<Thumbnail>> t = new AbstractTask<>() {
//            @Override
//            protected Optional<Thumbnail> call() {
//                return getCachedValue(mf).or(() -> {
////                                             log.debug("thumbnailCache missed for: {}:'{}', size: '{}'",
////                                                       mf.getId(),
////                                                       mf.getFullPath().getFileName(),
////                                                       ((com.github.benmanes.caffeine.cache.Cache<?, ?>) thCache.getNativeCache()).estimatedSize()
////                                             );
//                    return persistenceService.findByPathOrId(mf)
//                                             .map(t -> {
//                                                 thCache.put(mf, t);
//                                                 return t;
//                                             });
//                });
//            }
//        };
//        t.setOnSucceeded(unused -> t.getValue().ifPresentOrElse(u -> mf.setLoadedInCahce(true), () -> mf.setLoadedInCahce(false)));
//        t.setOnFailed(unused -> t.getValue().ifPresent(u -> mf.setLoadedInCahce(false)));
//        t.setOnCancelled(unused -> t.getValue().ifPresent(u -> mf.setLoadedInCahce(false)));
//        taskService.supply(t, true);
    }


    @NonNull
    private Thumbnail extractThumbnail(MediaFile mf) {
        Thumbnail thumbnail;
        try {
            thumbnail = Optional.ofNullable(thumbnailGenerator.extractThumbnail(mf.getFullPath()))
                                .orElseGet(() -> Thumbnail.builder().fullPath(mf.getFullPath())
                                                          .origin(EThumbnailType.ABSENT)
                                                          .build());
            thumbnail.setMfId(mf.getId());
            mf.setThumbnailType(thumbnail.getOrigin());
        } catch (Exception e) {
            log.error("invalid mf: '{}'", mf, e);
            thumbnail = Thumbnail.builder().fullPath(mf.getFullPath())
                                 .origin(EThumbnailType.ABSENT)
                                 .mfId(mf.getId())
                                 .build();
        }
        // TODO: Add better error management if cannot read the image
        thumbnail.setLastUpdate(LocalDateTime.now());
        return thumbnail;
    }

    @Cacheable(cacheNames = ImagInConfiguration.CACHE_IMAGE_FULL_SIZE, unless = "#result == null")
    public Image loadImage(MediaFile mediaFile) {
        if (mediaFile == null) {
            return null;
        }

        try {
            return imageLoader.loadImage(mediaFile);
        } catch (Exception e) {
            log.error("invalid mf: {}", mediaFile.getFullPath(), e);
        }
        return null;
    }

    public Optional<Image> getCachedImage(MediaFile mediaFile) {
        if (mediaFile == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(imagesCache.get(mediaFile, Image.class));
    }

    public void warmCache(final MediaFile mediaFile) {
        taskService.supply(() -> {
            getCachedImage(mediaFile)
                    .orElseGet(() -> {
                        var image = new Image(mediaFile.getFullPath().toUri().toString(),
                                              MediaLoader.PRIMARY_SCREEN_WIDTH,
                                              0,
                                              true,
                                              false,
                                              false);
                        imagesCache.put(mediaFile, image);
                        return image;
                    });
        });
    }

    public void getOrLoadImage(final MediaFile mediaFile) {
        record FutureImage(Image image, CompletableFuture<?> future) {
        }
        getCachedImage(mediaFile)
                .ifPresentOrElse(image -> taskService.sendEvent(ImageLoadedEvent.builder()
                                                                                .mediaFile(mediaFile)
                                                                                .image(image)
                                                                                .fromCache(true)
                                                                                .source(MediaLoader.this)
                                                                                .build()),
                                 () -> {
                                     taskService.supply(new AbstractTask<FutureImage>() {
                                         @Override
                                         protected FutureImage call() throws Exception {
                                             var image = new Image(mediaFile.getFullPath().toUri().toString(),
                                                                   0,
                                                                   0,
                                                                   true,
                                                                   false,
                                                                   false);
//                                             var image = imageLoader.loadImage(mediaFile);
                                             var future = taskService.sendEvent(ImageLoadingdEvent.builder()
                                                                                                  .mediaFile(mediaFile)
                                                                                                  .progress(image.progressProperty())
                                                                                                  .source(MediaLoader.this)
                                                                                                  .build());
                                             imagesCache.put(mediaFile, image);
//                                             Thread.sleep(20);
                                             return new FutureImage(image, future);
                                         }

                                         @Override
                                         protected void succeeded() {
                                             var futureImage = getValue();
                                             // To make sure ImageLoadedEvent come after ImageLoadingdEvent.
                                             futureImage.future
                                                     .thenRun(() -> taskService.sendEvent(ImageLoadedEvent.builder()
                                                                                                          .mediaFile(mediaFile)
                                                                                                          .image(futureImage.image)
                                                                                                          .fromCache(false)
                                                                                                          .source(MediaLoader.this)
                                                                                                          .build()));
                                         }
                                     }, false);
                                 });
    }

    @EventListener(WarmThumbnailCacheEvent.class)
    public void warmThumbnailCache(final WarmThumbnailCacheEvent event) {
        var mc = event.getMediaCollection();
        log.info("warmThumbnailCache for '{}', size: {}", event.getMediaCollection().path(), event.getMediaCollection().medias().size());
//        var mediaFiles = List.copyOf(mc.medias());
        taskService.sendEvent(CollectionEvent.builder()
                                             .mediaCollection(mc)
                                             .type(CollectionEvent.EventType.SELECTED)
                                             .source(this)
                                             .build());

//        var mediaFiles = List.copyOf(mc.medias().stream().sorted(Comparator.comparing(MediaFile::getOriginalDate)).limit(500).toList());
//
//        var futures = StreamEx.ofSubLists(mediaFiles, Constant.split(mediaFiles.size()))
//                              .map(files -> taskService.supply(warmCache(mc, files)))
//                              .toArray(new CompletableFuture[0]);
//
//        CompletableFuture.allOf(futures)
//                         .thenAcceptAsync(unused -> taskService.senfEventIntoFx(new CollectionEvent(mc, CollectionEvent.EventType.SELECTED, this)))
//                         .thenAcceptAsync(u -> lockThenRunOrSkip(mc, () -> mayRegenerateThubmnail(mc)));
////        taskService.fxNotifyLater(new CollectionEvent(mc, CollectionEvent.EventType.SELECTED, this));
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
                                      return new ThumbnailRes(entry.getValue(), thumbnail);
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
        } catch (InterruptedException e) {
            log.error("Cannot acquire lock for mediaCollection: '{}'", mediaCollection.path(), e);
        } finally {
            lock.unlock();
        }
        return null;
    }

    Void mayRegenerateThubmnail(MediaCollection mediaCollection) {
        if (catalogToReGenerate.contains(mediaCollection.id())) {
            taskService.sendEvent(GenerateThumbnailEvent.builder()
                                                        .mediaCollection(mediaCollection)
                                                        .source(this).build());
            catalogToReGenerate.remove(mediaCollection.id());
        }
        return null;
    }

    public void removeFromCache(MediaFile oldValue) {
        if (oldValue != null) {
            thCache.evict(oldValue);
        }
    }

    record ThumbnailRes(MediaFile mf, Thumbnail thumbnail) {
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
                         .thenAccept(unused -> log.info("Thumbnail extraction finished for '{}', '{}', files, it took: '{}'",
                                                        mediaCollection.path(),
                                                        mediaFiles.size(),
                                                        AmountFormats.wordBased(Duration.ofMillis(System.currentTimeMillis() - start),
                                                                                Locale.getDefault())
                         ))
                         .thenAccept(u -> catalogToReGenerate.add(mediaCollection.id()))
                         .thenAccept(u -> taskService.sendEvent(GalleryRefreshEvent.builder()
                                                                                   .mediaCollectionId(mediaCollection.id())
                                                                                   .source(this)
                                                                                   .build()))
                         .thenAccept(u -> taskService.sendEvent(CollectionEvent.builder()
                                                                               .mediaCollection(persistenceService.getMediaCollection(
                                                                                       mediaCollection.id()))
                                                                               .type(CollectionEvent.EventType.READY)
                                                                               .source(this)
                                                                               .build()))
                         .thenAccept(u -> taskService.sendEvent(GenerateThumbnailEvent.builder()
                                                                                      .mediaCollection(mediaCollection)
                                                                                      .source(this)
                                                                                      .build()))
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
                var updatedFiles = StreamEx.ofSubLists(EntryStream.of(files).toList(), 20)
                                           .map(ts -> ts.stream()
                                                        .map(entry -> {
                                                            // First Memory Cache
                                                            var mf = entry.getValue();
                                                            updateProgress(entry.getKey(), size);
                                                            updateMessage("Extract thumbnail from: " + mf.getFullPath().getFileName());
                                                            log.debug("Extract thumbnail from: '{}'", mf.getFullPath().getFileName());
                                                            var thumbnail = thCache.get(mf, Thumbnail.class);
                                                            if (thumbnail == null) {
                                                                // Second Database Cache
                                                                thumbnail = persistenceService.findByPathOrId(mf).orElse(null);
                                                                if (thumbnail == null) {
                                                                    thumbnail = extractThumbnail(mf);
                                                                    return new ThumbnailRes(entry.getValue(), thumbnail);
                                                                } else {
                                                                    // TODO: set type
                                                                    return new ThumbnailRes(entry.getValue(), thumbnail);
                                                                }
                                                            } else {
                                                                // TODO: set type
                                                                return new ThumbnailRes(entry.getValue(), thumbnail);
                                                            }
                                                        })
                                                        .toList())
                                           .flatMap(trs -> {
                                                        var thumbToBePersisted = trs.stream()
                                                                                    .map(ThumbnailRes::thumbnail)
                                                                                    .toList();
                                                        persistenceService.saveAll(thumbToBePersisted);

                                                        return trs.stream()
                                                                  .peek(tu -> tu.mf.setThumbnailType(tu.thumbnail.getOrigin()))
                                                                  .map(ThumbnailRes::mf);
                                                    }
                                           )
                                           .toList();
                persistenceService.updateCollection(mediaCollection.id(), updatedFiles, emptyList(), false);
                mediaCollection.replaceMedias(updatedFiles);
                return updatedFiles;
            }
        };
    }

    record ThumbnailUpdate(MediaFile mf, Thumbnail thumbnail, Image update) {
    }

    @EventListener(GenerateThumbnailEvent.class)
    public void generateThumbnails(GenerateThumbnailEvent event) {
        var catalog = event.getMediaCollection();
        generateThumbnails(catalog, catalog.medias());
    }

    private void generateThumbnails(final MediaCollection mediaCollection, final Collection<MediaFile> mediaFiles) {
        final var start      = System.currentTimeMillis();
        final var mfFiltered = mediaFiles.stream().filter(mf -> mf.getThumbnailType() == EThumbnailType.ABSENT).toList();

        log.info("Generate high quality thumbnail, nbEntries: {}", mfFiltered.size());
        if (mfFiltered.isEmpty()) {
            return;
        }

        final var batches = Collections.splitByCoreWithIdx(mfFiltered);
        var futures = batches.values()
                             .map(e -> taskService.supply(thumbnailBatchGeneration(mediaCollection, e, batches.splitCount())))
                             .toArray(new CompletableFuture[0]);

        CompletableFuture.allOf(futures)
                         .thenAccept(u -> taskService.sendEvent(GalleryRefreshEvent.builder()
                                                                                   .mediaCollectionId(mediaCollection.id())
                                                                                   .source(this)
                                                                                   .build()))
                         .thenAccept(u -> log.info("Thumbnail generation finished for '{}', '{}', files, it took: '{}'",
                                                   mediaCollection.path(),
                                                   mediaFiles.size(),
                                                   AmountFormats.wordBased(Duration.ofMillis(System.currentTimeMillis() - start),
                                                                           Locale.getDefault())
                         ));
    }

    Task<List<MediaFile>> thumbnailBatchGeneration(final MediaCollection mediaCollection,
                                                   final Map.Entry<Integer, List<MediaFile>> files,
                                                   int nbBatches) {
        return new AbstractTask<>() {
            @Override
            protected List<MediaFile> call() throws Exception {
                var size = files.getValue().size();
                log.debug("thumbnailBatchGeneration a batch of '{}' thumbnails for: {}", size, mediaCollection.path());
                updateTitle("Thumbnail high quality generation for '%s' images, batch %d/%d".formatted(size, files.getKey(), nbBatches));
                updateProgress(0, size);
                var updatedFiles = StreamEx.ofSubLists(EntryStream.of(files.getValue()).toList(), 20)
                                           .map(ts -> ts.stream()
                                                        .map(entry -> {
                                                            var mf = entry.getValue();
                                                            updateProgress(entry.getKey(), size);
                                                            var tu = ofNullable(thCache.get(mf, Thumbnail.class))
                                                                    .or(() -> persistenceService.findByPathOrId(mf))
                                                                    .filter(t -> mf.getThumbnailType() != EThumbnailType.GENERATED)
                                                                    .map(t -> new ThumbnailUpdate(mf, t, thumbnailGenerator.generate(t)))
                                                                    .orElse(null);
                                                            return tu;
                                                        })
                                                        .filter(Objects::nonNull)
                                                        .filter(tu -> tu.update() != null)
                                                        .peek(tu -> tu.thumbnail.setOrigin(EThumbnailType.GENERATED))
                                                        .peek(tu -> tu.thumbnail.setImage(tu.update))
                                                        .toList())
                                           .flatMap(tus -> {
                                                        log.debug("Update '{}' high res thumbnails", tus.size());
                                                        persistenceService.saveAll(tus.stream()
                                                                                      .map(ThumbnailUpdate::thumbnail)
                                                                                      .toList());

                                                        return tus.stream()
                                                                  .peek(tu -> tu.mf.setThumbnailType(tu.thumbnail.getOrigin()))
                                                                  .peek(tr -> thCache.evict(tr.mf)) // to make sure grid reload latest version.
                                                                  .map(tr -> tr.mf);
                                                    }
                                           ).toList();
                persistenceService.updateCollection(mediaCollection.id(), updatedFiles, emptyList(), false);
                mediaCollection.replaceMedias(updatedFiles);

                return updatedFiles;
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
