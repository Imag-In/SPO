package org.icroco.picture.views.util;

import io.jbock.util.Either;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.icroco.picture.config.SpoConfiguration;
import org.icroco.picture.event.*;
import org.icroco.picture.hash.IHashGenerator;
import org.icroco.picture.model.EThumbnailType;
import org.icroco.picture.model.MediaCollection;
import org.icroco.picture.model.MediaFile;
import org.icroco.picture.model.Thumbnail;
import org.icroco.picture.persistence.PersistenceService;
import org.icroco.picture.persistence.mapper.ThumbnailMapper;
import org.icroco.picture.thumbnail.IThumbnailGenerator;
import org.icroco.picture.thumbnail.ThumbnailService;
import org.icroco.picture.util.EitherUtils;
import org.icroco.picture.util.LangUtils;
import org.icroco.picture.views.task.*;
import org.icroco.picture.views.util.image.ImageLoader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Collections.emptySet;
import static org.icroco.picture.views.task.IFxCallable.wrap;

@Component
@Slf4j
public class MediaLoader {
    private final ThumbnailMapper thumbnailMapper;
    public static double          PRIMARY_SCREEN_WIDTH = Screen.getPrimary().getVisualBounds().getWidth();

    private final ThumbnailService thumbnailService;
    private final ImageLoader      imageLoader;
    //    final         Map<MediaFile, Thumbnail> thCache;
    final         Cache            imagesCache;
    private final TaskService      taskService;
    private final IHashGenerator   hashGenerator;

    private final PersistenceService persistenceService;
    private final Map<Integer, Lock> catalogLock         = new ConcurrentHashMap<>();
    private final Set<Integer>       catalogToReGenerate = new CopyOnWriteArraySet<>();
//    private final LRUCache<MediaFile, Thumbnail> lruCache = new LRUCache<>(1000);

    final Function<MediaFile, Either<Exception, Thumbnail>> cacheOrLoad;

    public MediaLoader(ThumbnailService thumbnailService,
                       ImageLoader imageLoader,
//                       @Qualifier(SpoConfiguration.CACHE_THUMBNAILS) Map<MediaFile, Thumbnail> thCache,
                       @Qualifier(SpoConfiguration.CACHE_IMAGE_FULL_SIZE) Cache imagesCache,
                       TaskService taskService,
                       IHashGenerator hashGenerator,
                       PersistenceService persistenceService,
                       ThumbnailMapper thumbnailMapper) {
        this.thumbnailService = thumbnailService;
        this.imageLoader = imageLoader;
//        this.thCache = thCache;
        this.imagesCache = imagesCache;
        this.taskService = taskService;
        this.hashGenerator = hashGenerator;
        this.persistenceService = persistenceService;
        this.thumbnailMapper = thumbnailMapper;
        cacheOrLoad = mf -> EitherUtils.of(thumbnailService.get(mf)
                , () -> new IllegalArgumentException(STR."Cannot find thumbnail for: '\{mf.getFullPath()}', id: '\{mf.getId()}'"));
    }


//    public Optional<Thumbnail> getCachedValue(MediaFile mf) {
//        return ofNullable(thCache.get(mf));
//    }

    public void loadAndCachedValue(final MediaFile mf) {
        taskService.supply(cacheOrLoad, mf)
                   .thenAcceptAsync(either -> either.ifLeftOrElse(e -> {
                                                                      Platform.runLater(() -> mf.setLoadedInCacheProperty(false));
                                                                      log.debug(e.getMessage());
                                                                  },
                                                                  t -> Platform.runLater(() -> mf.setLoadedInCacheProperty(true))));
    }

    public void loadAndCachedValues(final Collection<MediaFile> files) {
        final var localFiles = new ArrayList<>(files);
        Thread.ofVirtual().start(() -> {
            localFiles.stream()
                      .limit(200)
                      .forEach(cacheOrLoad::apply);

            Platform.runLater(() -> localFiles.forEach(mf -> mf.setLoadedInCacheProperty(true)));
        });
    }

//    @NonNull
//    private Thumbnail extractThumbnail(MediaFile mf) {
//        var thumbnail = thumbnailService.extract(mf);
//
//        mf.setThumbnailType(thumbnail.getOrigin());
//        thCache.remove(mf);
//
//        return thumbnail;
//    }

    @Cacheable(cacheNames = SpoConfiguration.CACHE_IMAGE_FULL_SIZE, unless = "#result == null")
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

    @Deprecated
    public void getOrLoadImage(final MediaFile mediaFile) {
        if (mediaFile == null) {
            return;
        }
        record FutureImage(Image image, CompletableFuture<?> future) {
        }
        getCachedImage(mediaFile)
                .ifPresentOrElse(image -> taskService.sendEvent(ImageLoadedEvent.builder()
                                                                                .mediaFile(mediaFile)
                                                                                .image(image)
                                                                                .fromCache(true)
                                                                                .source(MediaLoader.this)
                                                                                .build()),
                                 () -> taskService.supply(new AbstractTask<FutureImage>() {
                                     @Override
                                     protected FutureImage call() {
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
                                 }, false));
    }


    public Image getImage(MediaFile mediaFile) {
        return getCachedImage(mediaFile).orElseGet(() -> {
            var image = new Image(mediaFile.getFullPath().toUri().toString(),
                                  0,
                                  0,
                                  true,
                                  false,
                                  false);
            imagesCache.put(mediaFile, image);
            return image;
        });
    }

    public void getOrLoadImage2(final MediaFile mediaFile, @NonNull BiConsumer<MediaFile, Image> fxImageConsumer) {
        if (mediaFile == null) {
            return;
        }

        taskService.supply(ModernTask.<Image>builder()
                                     .execute(_ -> getImage(mediaFile))
                                     .onSuccess((_, image) -> fxImageConsumer.accept(mediaFile, image))
                                     .build(),
                           false);
//        record FutureImage(Image image, CompletableFuture<?> future) {
//        }
//        getCachedImage(mediaFile)
//                .ifPresentOrElse(image -> FxPlatformExecutor.fxRun(() -> fxImageConsumer.accept(mediaFile, image)),
//                                 () -> taskService.supply(new AbstractTask<FutureImage>() {
//                                     @Override
//                                     protected FutureImage call() {
//                                         var image = new Image(mediaFile.getFullPath().toUri().toString(),
//                                                               0,
//                                                               0,
//                                                               true,
//                                                               false,
//                                                               false);
////                                             var image = imageLoader.loadImage(mediaFile);
//                                         var future = taskService.sendEvent(ImageLoadingdEvent.builder()
//                                                                                              .mediaFile(mediaFile)
//                                                                                              .progress(image.progressProperty())
//                                                                                              .source(MediaLoader.this)
//                                                                                              .build());
//                                         imagesCache.put(mediaFile, image);
////                                             Thread.sleep(20);
//                                         return new FutureImage(image, future);
//                                     }
//
//                                     @Override
//                                     protected void succeeded() {
//                                         var futureImage = getValue();
//                                         // To make sure ImageLoadedEvent come after ImageLoadingdEvent.
//                                         futureImage.future
//                                                 .thenRun(() -> fxImageConsumer.accept(mediaFile, futureImage.image));
//                                     }
//                                 }, false));
    }

    @EventListener(WarmThumbnailCacheEvent.class)
    public void warmThumbnailCache(final WarmThumbnailCacheEvent event) {
        var mc = event.getMediaCollection();
        log.info("warmThumbnailCache for '{}', size: {}", event.getMediaCollection().path(), event.getMediaCollection().medias().size());
//        var mediaFiles = List.copyOf(mc.medias());
        taskService.sendEvent(CollectionEvent.builder()
                                             .mcId(mc.id())
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
            protected List<ThumbnailRes> call() {
                log.debug("Warm Cache for: '{}', size: {}", mediaCollection.path(), files.size());
                updateTitle("Loading thumbnails ...");
                updateProgress(0, files.size());
                return EntryStream.of(new ArrayList<>(files))
                                  .map(entry -> {
                                      var thumbnail = thumbnailService.get(entry.getValue());
                                      thumbnail.ifPresent(t -> updateMessage(STR."Thumbnail loaded: \{entry.getValue()
                                                                                                           .fullPath()
                                                                                                           .getFileName()}"));
//                                      if (thumbnail == null) {
//                                          thumbnail = persistenceService.findByPathOrId(entry.getValue()).orElse(null);
//                                          if (thumbnail != null) {
//                                              thCache.put(entry.getValue(), thumbnail);
//                                              updateMessage(STR."Thumbnail loaded: \{entry.getValue().fullPath().getFileName()}");
//                                          }
                                      updateProgress(entry.getKey(), files.size());
//                                      }
                                      return new ThumbnailRes(entry.getValue(), thumbnail.orElse(null));
                                  })
                                  .filter(tr -> tr.thumbnail() != null)
                                  .toList();
            }

            @Override
            protected void succeeded() {
                getValue().forEach(thumbnailRes -> thumbnailRes.mf.getLastUpdatedProperty().set(thumbnailRes.thumbnail.getLastUpdate()));
            }
        };
    }

    private <R> R lockThenRunOrSkip(MediaCollection mediaCollection, Supplier<R> callable) {
        var lock = catalogLock.computeIfAbsent(mediaCollection.id(), _ -> new ReentrantLock());

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
                                                        .mcId(mediaCollection.id())
                                                        .source(this).build());
            catalogToReGenerate.remove(mediaCollection.id());
        }
        return null;
    }

//    public void removeFromCache(MediaFile oldValue) {
//        if (oldValue != null) {
//            thCache.remove(oldValue);
//        }
//    }

    record ThumbnailRes(MediaFile mf, Thumbnail thumbnail) {
    }

    @EventListener(ExtractThumbnailEvent.class)
    public void extractThumbnails(ExtractThumbnailEvent event) {
        MediaCollection mediaCollection = persistenceService.getMediaCollection(event.getMcId());
        extractThumbnails(mediaCollection, List.copyOf(mediaCollection.medias()), true, event.isUpdate());
    }

    private void extractThumbnails(MediaCollection mediaCollection, List<MediaFile> mediaFiles, boolean sendReadyEvent, boolean isUpdate) {
        Thread.ofVirtual().name("extract-thumbails").start(() -> {
            final var start = System.currentTimeMillis();

            log.info("Extract thumbnail, collection: '{}', nbEntries: {}", mediaCollection.path(), mediaFiles.size());
            if (mediaCollection.medias().isEmpty()) {
                log.warn("Trying to generate thumbnail for empty list of files: {}", mediaCollection);
                return;
            }

            try (var scope = new FxRunAllScope<MediaFile>(taskService, STR."Extract thumbnails for '\{mediaFiles.size()}' files.")) {
                var tasks = mediaFiles.stream()
                                      .map(mf -> wrap(STR."Processing: \{mf.fileName()}",
                                                      () -> extractAndSaveThumbails(isUpdate, mf).mf))
                                      .map(scope::fork)
                                      .toList();

                // Wait subtasks to finish
                scope.join();

                // Post compute.
                var files = tasks.stream()
                                 .filter(task -> task.state() == StructuredTaskScope.Subtask.State.SUCCESS)
                                 .map(StructuredTaskScope.Subtask::get)
                                 .map(TaskResult::result)
                                 .toList();
                if (isUpdate) {
                    persistenceService.updateCollection(mediaCollection.id(), emptySet(), emptySet(), Set.copyOf(files));
                } else {
                    persistenceService.updateCollection(mediaCollection.id(), Set.copyOf(files), emptySet(), emptySet());
                }
                var filesExtracted = files.stream()
                                          .filter(mf -> thumbnailService.get(mf).filter(t -> t.getOrigin() == EThumbnailType.EXTRACTED).isPresent())
                                          .toList();
                Platform.runLater(() -> {
                    var now = LocalDateTime.now();
                    filesExtracted.forEach(mediaFile -> mediaFile.setLastUpdated(now));
                });
                log.info("Thumbnail extraction finished for '{}', '{}', files, it took: '{}'",
                         mediaCollection.path(),
                         mediaFiles.size(),
                         LangUtils.wordBased(Duration.ofMillis(System.currentTimeMillis() - start)));
                catalogToReGenerate.add(mediaCollection.id());
                if (sendReadyEvent) {
                    taskService.sendEvent(CollectionEvent.builder()
                                                         .mcId(mediaCollection.id())
                                                         .type(CollectionEvent.EventType.READY)
                                                         .source(this)
                                                         .build());
                }
                taskService.sendEvent(NotificationEvent.builder()
                                                       .type(NotificationEvent.NotificationType.INFO)
                                                       .message("'%s' thumbnails extracted!"
                                                                        .formatted(filesExtracted.size()))
                                                       .source(this)
                                                       .build());
                taskService.sendEvent(GenerateThumbnailEvent.builder().mcId(mediaCollection.id()).source(this).build());
            } catch (InterruptedException e) {
                log.error("Unexpected error", e);
            }
        });

//        final var batches = Collections.splitByCoreWithIdx(mediaFiles);
//        var futures = batches.values()
//                             .map(e -> taskService.supply(thumbnailBatchExtraction(mediaCollection, batches.splitCount(), e, isUpdate)))
//                             .toArray(new CompletableFuture[0]);q
//
//        CompletableFuture.allOf(futures)
//                         // Only log statement
//                         .thenAccept(_ -> log.info("Thumbnail extraction finished for '{}', '{}', files, it took: '{}'",
//                                                   mediaCollection.path(),
//                                                   mediaFiles.size(),
//                                                   LangUtils.wordBased(Duration.ofMillis(System.currentTimeMillis() - start))
//                         ))
//                         .thenAccept(_ -> catalogToReGenerate.add(mediaCollection.mcId()))
//                         .thenAccept(_ -> taskService.sendEvent(GalleryRefreshEvent.builder()
//                                                                                   .mediaCollectionId(mediaCollection.mcId())
//                                                                                   .source(this)
//                                                                                   .build()))
//                         .thenAccept(_ -> {
//                             if (sendReadyEvent) {
//                                 taskService.sendEvent(CollectionEvent.builder()
//                                                                      .mcId(mediaCollection.mcId())
//                                                                      .type(CollectionEvent.EventType.READY)
//                                                                      .source(this)
//                                                                      .build());
//                             }
//                         })
//                         .thenAccept(_ -> taskService.sendEvent(GenerateThumbnailEvent.builder()
//                                                                                      .mcId(mediaCollection.mcId())
//                                                                                      .source(this)
//                                                                                      .build()))
//        ;
    }

    private ThumbnailRes extractAndSaveThumbails(boolean isUpdate, MediaFile mf) {
        Thumbnail thumbnail;
        if (isUpdate) { // We force extraction.
            thumbnail = thumbnailService.extract(mf);
            mf.setHash(hashGenerator.compute(mf.fullPath()).orElse("???"));
            mf.setHashDate(LocalDate.now());
        } else {
            thumbnail = thumbnailService.get(mf, false).orElse(null);
        }
        if (thumbnail == null) { // if isUpdate == false
            // Second Database Cache
            thumbnail = thumbnailService.get(mf, false).orElseGet(() -> {
                var t = thumbnailService.extract(mf);
                mf.setHash(hashGenerator.compute(mf.fullPath()).orElse("????"));
                mf.setHashDate(LocalDate.now());
                return t;
            });
        }
        thumbnail = persistenceService.save(mf, thumbnail);
        return new ThumbnailRes(mf, thumbnail);
    }

    Task<List<MediaFile>> thumbnailBatchExtraction(MediaCollection mediaCollection,
                                                   final int nbBatches,
                                                   Map.Entry<Integer, List<MediaFile>> e,
                                                   boolean isUpdate) {
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
                                                            updateMessage(STR."Extract thumbnail from: \{mf.getFullPath().getFileName()}");
                                                            log.debug("Extract thumbnail from: '{}'", mf.getFullPath().getFileName());
                                                            return extractAndSaveThumbails(isUpdate, mf);
                                                        })
                                                        .toList())
                                           .flatMap(thumbnailRes -> thumbnailRes.stream().map(ThumbnailRes::mf))
                                           .toList();
                if (isUpdate) {
                    persistenceService.updateCollection(mediaCollection.id(), emptySet(), emptySet(), Set.copyOf(updatedFiles));
                } else {
                    persistenceService.updateCollection(mediaCollection.id(), Set.copyOf(updatedFiles), emptySet(), emptySet());
                }
//                mediaCollection.replaceMedias(updatedFiles);
                return updatedFiles;
            }

            @Override
            protected void succeeded() {
                var now = LocalDateTime.now();
                getValue().forEach(mediaFile -> {
//                    mediaFile.setLoadedInCache(false);
                    mediaFile.setLastUpdated(now);
                });
            }
        };
    }

    public record ThumbnailUpdate(MediaFile mf, Thumbnail thumbnail, Image update) {
    }

    @EventListener(ForceGenerateThumbnailEvent.class)
    public void forceGenerateThumbnails(ForceGenerateThumbnailEvent event) {
        final var mf = event.getMediaFile();
        ModernTask<MediaFile> task = ModernTask.<MediaFile>builder()
                                               .execute(myself -> {
                                                   myself.updateProgress(-1, -1);
                                                   myself.updateTitle("Generate thumbnail");
                                                   myself.updateMessage(mf.fileName());
                                                   var shouldGenerate = thumbnailService.get(mf)
                                                                                        .map(Thumbnail::getDimension)
                                                                                        .map(d -> {
                                                                                            if (d.isLesserThan(IThumbnailGenerator.DEFAULT_THUMB_SIZE)) {
                                                                                                return true;
                                                                                            } else {
                                                                                                log.warn(
                                                                                                        "'{}': Embedded thumbail size is already wider: '{}' than wanted resize size: '{}'",
                                                                                                        mf.getFullPath(),
                                                                                                        d,
                                                                                                        IThumbnailGenerator.DEFAULT_THUMB_SIZE);
                                                                                                return false;
                                                                                            }
                                                                                        })
                                                                                        .orElse(true);
                                                   if (shouldGenerate) {
                                                       thumbnailService.generate(mf);
                                                       persistenceService.updateCollection(mf.getCollectionId(),
                                                                                           emptySet(),
                                                                                           emptySet(),
                                                                                           Set.of(mf));
                                                   }
                                                   return mf;
                                               })
                                               .onSuccess((_, mediaFile) -> {
                                                   mediaFile.setLastUpdated(LocalDateTime.now());
                                               })
                                               .build();
        taskService.supply(task);
    }

    @EventListener(GenerateThumbnailEvent.class)
    public void generateThumbnails(GenerateThumbnailEvent event) {
        var catalog = persistenceService.getMediaCollection(event.getMcId());
        generateThumbnails(catalog, catalog.medias(), false);
    }

    private void generateThumbnails(final MediaCollection mediaCollection,
                                    final Collection<MediaFile> mediaFiles,
                                    boolean forceGenerateAll) {
        Thread.ofVirtual().name("generate-thumbails")
              .start(() -> {
                  final var start = System.currentTimeMillis();
                  final var mfFiltered = forceGenerateAll
                                         ? mediaFiles
                                         : mediaFiles.stream()
                                                     .filter(mf -> thumbnailService.get(mf, false).filter(t -> t.getOrigin().isNotExtracted()).isPresent())
                                                     .toList();

                  log.info("Generate high quality thumbnail, nbEntries: {}", mfFiltered.size());
                  if (mfFiltered.isEmpty()) {
                      return;
                  }

//                  taskService.sendEvent(NotificationEvent.builder()
//                                                         .type(NotificationEvent.NotificationType.INFO)
//                                                         .message("'%s' thumbnails are missing, we'll generate them (HQ)..."
//                                                                          .formatted(mfFiltered.size()))
//                                                         .source(this)
//                                                         .build());

                  try (var scope = new FxRunAllScope<MediaFile>(taskService, STR."Generate thumbnails for '\{mfFiltered.size()}' files.")) {
                      var tasks = mfFiltered.stream()
                                            .map(mf -> wrap(STR."Generate thumbnail: \{mf.fileName()}",
                                                            () -> {
                                                                thumbnailService.get(mf)
                                                                                .filter(th -> th.getOrigin().isNotGenerated())
                                                                                .ifPresent(_ -> thumbnailService.generate(mf));
                                                                return mf;
                                                            }
                                            ))
                                            .map(scope::fork)
                                            .toList();

                      scope.join();

                      // Post compute.
                      var files = tasks.stream()
                                       .filter(t -> t.state() == StructuredTaskScope.Subtask.State.SUCCESS)
                                       .map(StructuredTaskScope.Subtask::get)
                                       .map(TaskResult::result)
                                       .toList();
                      persistenceService.updateCollection(mediaCollection.id(),
                                                          Set.copyOf(LangUtils.safeCollection(files)),
                                                          emptySet(),
                                                          emptySet());
                      mediaCollection.replaceMedias(files);

                      Platform.runLater(() -> {
                          var now = LocalDateTime.now();
                          files.forEach(mediaFile -> {
//                              mediaFile.setLoadedInCache(false);
                              mediaFile.setLastUpdated(now);
                          });
                      });
                      log.info("Thumbnail generation finished for '{}', '{}', files, it took: '{}'",
                               mediaCollection.path(),
                               mfFiltered.size(),
                               LangUtils.wordBased(Duration.ofMillis(System.currentTimeMillis() - start)));
                      taskService.sendEvent(NotificationEvent.builder()
                                                             .type(NotificationEvent.NotificationType.INFO)
                                                             .message("'%s' thumbnails generated!"
                                                                              .formatted(mfFiltered.size()))
                                                             .source(this)
                                                             .build());
                  } catch (InterruptedException e) {
                      log.error("Unexpected error", e);
                  }
              });
    }

    Task<List<MediaFile>> thumbnailBatchGeneration(final MediaCollection mediaCollection,
                                                   final Map.Entry<Integer, List<MediaFile>> files,
                                                   int nbBatches) {
        return new AbstractTask<>() {
            @Override
            protected List<MediaFile> call() {
                var size = files.getValue().size();
                log.debug("thumbnailBatchGeneration a batch of '{}' thumbnails for: {}", size, mediaCollection.path());
                updateTitle("Thumbnail high quality generation for '%s' images, batch %d/%d".formatted(size, files.getKey(), nbBatches));
                updateProgress(0, size);
                var updatedFiles = StreamEx.ofSubLists(EntryStream.of(files.getValue()).toList(), 20)
                                           .map(ts -> ts.stream()
                                                        .map(entry -> {
                                                            var mf = entry.getValue();
                                                            updateProgress(entry.getKey(), size);
                                                            updateMessage(STR."Generate thumbnail for: \{entry.getValue()
                                                                                                              .getFullPath()
                                                                                                              .getFileName()}");
                                                            return new ThumbnailRes(mf, thumbnailService.get(mf)
                                                                                                        .filter(th -> th.getOrigin()
                                                                                                                      != EThumbnailType.GENERATED)
                                                                                                        .map(_ -> thumbnailService.generate(mf))
                                                                                                        .orElse(null));
                                                        })
                                                        .filter(tr -> tr.thumbnail != null)
                                                        .toList())
                                           .flatMap(tus -> {
                                               log.debug("Update '{}' high res thumbnails", tus.size());
                                               return tus.stream()
                                                         .map(tr -> tr.mf);
                                           })
                                           .toList();
                persistenceService.updateCollection(mediaCollection.id(), Set.copyOf(updatedFiles), emptySet(), emptySet());
                mediaCollection.replaceMedias(updatedFiles);

                return updatedFiles;
            }

            @Override
            protected void succeeded() {
                var now = LocalDateTime.now();
                getValue().forEach(mediaFile -> {
//                    mediaFile.setLoadedInCache(false);
                    mediaFile.setLastUpdated(now);
                });
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

    @EventListener(CustomExtractThumbnailEvent.class)
    public void updateImages(CustomExtractThumbnailEvent event) {
        log.info("Recieved CustomExtractThumbnailEvent on collection: '{}', newItems: '{}', updateItems: {}",
                 event.getMcId(),
                 event.getNewItems().size(),
                 event.getModifiedItems());

        if (!event.getNewItems().isEmpty()) {
            extractThumbnails(persistenceService.getMediaCollection(event.getMcId()),
                              List.copyOf(event.getNewItems()),
                              true,
                              false);
        }
        if (!event.getModifiedItems().isEmpty()) {
            extractThumbnails(persistenceService.getMediaCollection(event.getMcId()),
                              List.copyOf(event.getModifiedItems()),
                              true,
                              true);
        }
        // Nothing to do for deleted files.
    }
}
