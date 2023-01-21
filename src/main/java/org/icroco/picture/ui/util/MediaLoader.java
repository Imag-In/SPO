package org.icroco.picture.ui.util;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.icroco.picture.ui.config.ImageInConfiguration;
import org.icroco.picture.ui.event.CatalogEvent;
import org.icroco.picture.ui.event.GenerateThumbnailEvent;
import org.icroco.picture.ui.model.Catalog;
import org.icroco.picture.ui.model.EThumbnailStatus;
import org.icroco.picture.ui.model.MediaFile;
import org.icroco.picture.ui.model.Thumbnail;
import org.icroco.picture.ui.persistence.PersistenceService;
import org.icroco.picture.ui.persistence.ThumbnailRepository;
import org.icroco.picture.ui.task.AbstractTask;
import org.icroco.picture.ui.task.TaskService;
import org.icroco.picture.ui.util.thumbnail.IThumbnailGenerator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

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
    private Image loadThumbnail(long id, Path p) {
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

    public void loadThumbnailFromFx(final MediaFile file) {
        CompletableFuture.supplyAsync(() -> loadThumbnail(file))
                         .thenAccept(thumbnailRes -> Platform.runLater(thumbnailRes::updateThumbnail));
    }

    /**
     * This method must not be called from javafx thread
     */
    public ThumbnailRes loadThumbnail(final MediaFile file) {
        if (Platform.isFxApplicationThread()) { // TODO: Maybe remove this check later for performance issue.
            throw new RuntimeException("Invalid method call (called into JavaFx Thread snd must not)");
        }
        var thumbnail = thumbnailCache.get(file.id(), Thumbnail.class);
        if (thumbnail == null) {
            thumbnail = persistenceService.findByPathOrId(file)
                                          .orElseGet(() -> {
                                              log.debug("No db cached for: '{}'", file.fullPath());
                                              var image = loadThumbnail(file.getId(), file.getFullPath());
                                              var now   = LocalDate.now();

                                              return persistenceService.saveOrUpdate(Thumbnail.builder().id(file.id())
                                                                                                        .fullPath(file.fullPath())
                                                                                                        .thumbnail(image)
                                                                                                        .hashDate(now)
                                                                                                        .lastAccess(now)
                                                                                                        .origin(EThumbnailStatus.GENERATED)
                                                                                                        .build());
                                          });
            thumbnailCache.put(file.id(), thumbnail);
        }
        return new ThumbnailRes(file, thumbnail);
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
        var futures = mediaFiles.chunk(Math.max(1, mediaFiles.size() / Constant.NB_CORE))
                                .collect(p -> taskService.supply(thumbnailBatch(event.getCatalog(), p)))
                                .toArray(new CompletableFuture[0]);

        CompletableFuture.allOf(futures)
                         .thenAccept(u -> taskService.notifyLater(new CatalogEvent(event.getCatalog(), CatalogEvent.EventType.SELECTED, this)));
    }

    record ThumbnailRes(MediaFile file, Thumbnail thumbnail) {
        void updateThumbnail() {
            if (!Platform.isFxApplicationThread()) {
                throw new RuntimeException("This call must be done from JavaFx Thread (and not from: " + Thread.currentThread().getName() + ")");
            }
            file.getThumbnail().set(thumbnail);
        }
    }

    Task<List<ThumbnailRes>> thumbnailBatch(Catalog catalog, final RichIterable<MediaFile> files) {
        return new AbstractTask<>() {
            //            StopWatch w = new StopWatch("Load Task");
            @Override
            protected List<ThumbnailRes> call() throws Exception {
                var size = files.size();
//                w.start("Build Thumbnail");
                log.debug("Generate a batch of '{}' thumbnails for: {}", size, catalog.path());
                updateMessage("Thumbnail generation for '" + size + " images");
                updateProgress(0, size);
                final LocalDate now = LocalDate.now();

                int i   = 0;
                var res = new ArrayList<ThumbnailRes>(size);
                for (MediaFile mf : files) {
                    i++;
                    res.add(loadThumbnail(mf));
                    updateProgress(i, size);
                }

                return res;
            }

            @Override
            protected void succeeded() {
                getValue().forEach(ThumbnailRes::updateThumbnail);
            }
        };
    }
}
