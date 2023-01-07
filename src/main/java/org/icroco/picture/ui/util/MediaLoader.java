package org.icroco.picture.ui.util;

import javafx.concurrent.Task;
import javafx.scene.image.Image;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.collections.api.factory.Lists;
import org.icroco.picture.ui.config.PictureConfiguration;
import org.icroco.picture.ui.event.GenerateThumbnailEvent;
import org.icroco.picture.ui.event.TaskEvent;
import org.icroco.picture.ui.model.MediaFile;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class MediaLoader {
    private static Image                       loading = loadImage(MediaLoader.class.getResource("/images/Loading_2.gif"));
    @Qualifier(Constant.APPLICATION_EVENT_MULTICASTER)
    private final  ApplicationEventMulticaster eventBus;

    //    @Cacheable(cacheNames = PictureConfiguration.THUMBNAILS, unless = "#result == null")
    private synchronized Image loadThumbnail(Path p) {
        try {
            log.info("Cache miss: {}", p);
            return new Image(p.toUri().toString(), 400D, 0, true, false);
        }
        catch (Exception e) {
            log.error("invalid file: {}", p, e);
        }
        return null;
    }

    @Cacheable(cacheNames = PictureConfiguration.THUMBNAILS, key = "#file.id", unless = "#result == null")
    public Image loadThumbnail(MediaFile file) {
        return loadThumbnail(file.fullPath());
    }

    private static Image loadImage(URL url) {
        try {
            return new Image(url.toURI().toString());
        }
        catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Cacheable(cacheNames = PictureConfiguration.FULL_SIZE, unless = "#result == null")
    public Image loadImage(Path p) {
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
        Lists.immutable
                .ofAll(event.getCatalog().medias())
                .chunk(event.getCatalog().medias().size() / Constant.NB_CORE)
//                .chunk(50)
                .forEach(p -> eventBus.multicastEvent(new TaskEvent(thumbnailBatch(p.toList()), this)));
    }

    Task<List<MediaFile>> thumbnailBatch(final List<MediaFile> files) {
        return new Task<>() {
//            StopWatch w = new StopWatch("Load Task");

            @Override
            protected List<MediaFile> call() throws Exception {
                var size          = files.size();
                var imagesResults = new ArrayList<>(files);
//                w.start("Build Thumbnail");
                updateMessage("Thumbnail generation for '" + size + " images");
                updateProgress(0, size);

                for (int i = 0, filesSize = files.size(); i < filesSize; i++) {
                    MediaFile mediaFile = files.get(i);
                    loadThumbnail(mediaFile);
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
