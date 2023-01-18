package org.icroco.picture.ui.util;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.icroco.picture.ui.config.ImageInConfiguration;
import org.icroco.picture.ui.event.GenerateThumbnailEvent;
import org.icroco.picture.ui.model.Catalog;
import org.icroco.picture.ui.model.EThumbnailStatus;
import org.icroco.picture.ui.model.MediaFile;
import org.icroco.picture.ui.model.Thumbnail;
import org.icroco.picture.ui.persistence.PersistenceService;
import org.icroco.picture.ui.persistence.ThumbnailRepository;
import org.icroco.picture.ui.task.TaskService;
import org.icroco.picture.ui.util.thumbnail.IThumbnailGenerator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class MediaLoader {
    private final ThumbnailRepository thumbnailRepository;
    public static Image               LOADING = loadImage(MediaLoader.class.getResource("/images/loading-icon-png-9.jpg"));
    private final IThumbnailGenerator thumbnailGenerator;
    @Qualifier(ImageInConfiguration.THUMBNAILS)
    private final Cache               thumbnailCache;
    private final TaskService         taskService;

    private final PersistenceService                  persistenceService;
    private final Map<Long, CompletableFuture<Image>> taskCache = new ConcurrentHashMap<>();

    private final Dimension DEFAULT_THUMB_SIZE = new Dimension(600, 600);

    //    @Cacheable(cacheNames = ImageInConfiguration.THUMBNAILS, key = "#id", unless = "#result == null")
    private synchronized Image loadThumbnail(long id, Path p) {
        try {
//            log.info("Cache miss: {}", p);
//            return new Image(p.toUri().toString(), 300D, 0, true, true);
            return thumbnailGenerator.generate(p, DEFAULT_THUMB_SIZE);
        }
        catch (Exception e) {
            log.error("invalid file: {}", p, e);
        }
        // TODO: Add better error management if cannot read the image
        return null;
    }

    public void loadThumbnail(final MediaFile file) {
        final var thumbnail = thumbnailCache.get(file.id(), Thumbnail.class);
//
        if (thumbnail == null) {
            persistenceService.findById(file.id())
                              .ifPresentOrElse(t -> {
                                                   thumbnailCache.put(file.id(), t);
                                                   Platform.runLater(() -> file.getThumbnail().set(thumbnail));
                                               },
                                               () ->
                                                       taskCache.computeIfAbsent(file.id(), (k) -> {
                                                           var f = CompletableFuture.supplyAsync(() -> loadThumbnail(k, file.fullPath()));
                                                           f.handle((i, throwable) -> {
                                                               taskCache.remove(k);
                                                               if (throwable == null) {
                                                                   try {
                                                                       final Thumbnail t = persistenceService.saveOrUpdate(Thumbnail.builder().id(file.id())
                                                                                                                                              .thumbnail(i)
                                                                                                                                              .hashDate(
                                                                                                                                                      LocalDate.now())
                                                                                                                                              .lastAccess(
                                                                                                                                                      LocalDate.now())
                                                                                                                                              .origin(EThumbnailStatus.GENERATED)
                                                                                                                                              .build());
                                                                       thumbnailCache.put(k, t);
                                                                       Platform.runLater(() -> file.getThumbnail().set(t));
                                                                       return t;
                                                                   }
                                                                   catch (Exception ex) {
                                                                       log.error("Cannot save thumbnail for: '{}'", k, ex);
                                                                   }
                                                               }
                                                               log.info("Error while loading thumbnail: {}", k, throwable);
                                                               return null;
                                                           });
                                                           return f;
                                                       }));
        } else {
            Platform.runLater(() -> file.getThumbnail().set(thumbnail));
        }
//        return LOADING;
//        var image = tumbnailCache.get(file.id(), Image.class);
//
//        if (image == null) {
//            CompletableFuture.supplyAsync(() -> loadThumbnail(file.id(), file.fullPath()))
//                             .thenAccept(i -> Platform.runLater(() -> consumer.accept(i)));
//            return loading;
//        }
//        return image;

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

    @CachePut(key = "p")
    public Image updateCache(Path p, Image image) {
        return image;

    }

    @EventListener(GenerateThumbnailEvent.class)
    public void generateThumbnails(GenerateThumbnailEvent event) {
        ImmutableList<MediaFile> mediaFiles = Lists.immutable.ofAll(event.getCatalog().medias());
        log.info("Entries: {}", mediaFiles.size());
        if (event.getCatalog().medias().isEmpty()) {
            log.warn("Trying to generate thumbnail for empty list of files: {}", event.getCatalog());
            return;
        }
        mediaFiles
//                .chunk(Math.max(1, mediaFiles.size() / Constant.NB_CORE))
.chunk(10)
.forEach(p -> taskService.supply(thumbnailBatch(event.getCatalog(), p.toList())));
    }

    Task<List<MediaFile>> thumbnailBatch(Catalog catalog, final List<MediaFile> files) {
        return new Task<>() {
//            StopWatch w = new StopWatch("Load Task");

//            record ThumbnailRes(MediaFile file, )

            @Override
            protected List<MediaFile> call() throws Exception {
                var size          = files.size();
                var imagesResults = new ArrayList<>(files);
//                w.start("Build Thumbnail");
                log.info("Generate {} Thumbnails for: {}", size, catalog.path());
                updateMessage("Thumbnail generation for '" + size + " images");
                updateProgress(0, size);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                for (int i = 0, filesSize = files.size(); i < filesSize; i++) {
                    MediaFile mediaFile = files.get(i);
                    loadThumbnail(mediaFile);
//                    mediaFile.getThumbnail().set(loadThumbnail(mediaFile));
                    updateProgress(i, size);
                }


//                for (int i = 0; i < size; i++) {
//                    final var mf = files.get(i);
//                    imagesResults.add(mf);
//                    mf.thumbnail().setLoaded(true);
//                    mf.thumbnail().setImage(mediaLoader.loadThumbnail(mf.fullPath()));
//
////                    imagesResults.add(new MediaFile(mf.id(),
////                                                    mf.fullPath(),
////                                                    mf.fileName(),
////                                                    mf.originalDate(),
////                                                    mf.tags(),
////                                                    new Thumbnail(mediaLoader.loadThumbnail(mf.fullPath()), true)));
//                    updateProgress(i, size);
//                }
//                w.stop();
//                w.start("Save Task into DB");
//                persistenceService.saveMediaFiles(imagesResults);
//                w.stop();
//                log.info("PERF: {}", w.prettyPrint());

                return imagesResults;
            }

//            @Override
//            protected void succeeded() {
//                images.addAll(getValue()
//                                      .stream()
//                                      .filter(Objects::nonNull)
//                                      .toList());
//            }

            @Override
            protected void failed() {
                try {
                    throw getException();
                }
                catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

}
