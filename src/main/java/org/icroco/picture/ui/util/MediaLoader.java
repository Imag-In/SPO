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
import org.icroco.picture.ui.event.GenerateThumbnailEvent;
import org.icroco.picture.ui.event.WarmThumbnailCacheEvent;
import org.icroco.picture.ui.model.Catalog;
import org.icroco.picture.ui.model.EThumbnailStatus;
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

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private final Dimension DEFAULT_THUMB_SIZE = new Dimension(600, 600);

    //    @Cacheable(cacheNames = ImageInConfiguration.THUMBNAILS, key = "#id", unless = "#result == null")
    private Image loadThumbnail(long id, Path p) {
        try {
//            log.info("Cache miss: {}", p);
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
    private ThumbnailRes loadThumbnail(final MediaFile file) {
        if (Platform.isFxApplicationThread()) { // TODO: Maybe remove this check later for performance issue.
            throw new RuntimeException("Invalid method call (called into JavaFx Thread, must not)");
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

    @EventListener(GenerateThumbnailEvent.class)
    public void generateThumbnails(GenerateThumbnailEvent event) {
//        ImmutableList<MediaFile> mediaFiles = Lists.immutable.ofAll(event.getCatalog().medias());
        var mediaFiles = List.copyOf(event.getCatalog().medias());
        log.info("Entries: {}", mediaFiles.size());
        if (event.getCatalog().medias().isEmpty()) {
            log.warn("Trying to generate thumbnail for empty list of files: {}", event.getCatalog());
            return;
        }
//        var futures = mediaFiles.chunk(Math.max(1, mediaFiles.size() / Constant.NB_CORE))
//                                .collect(p -> taskService.supply(thumbnailBatch(event.getCatalog(), p)))
//                                .toArray(new CompletableFuture[0]);

        var futures = StreamEx.ofSubLists(mediaFiles, Math.max(1, mediaFiles.size() / Constant.NB_CORE))
                              .map(p -> taskService.supply(thumbnailBatch(event.getCatalog(), p)))
                              .toArray(new CompletableFuture[0]);

        CompletableFuture.allOf(futures)
                         .thenAccept(u -> taskService.notifyLater(new CatalogEvent(event.getCatalog(), CatalogEvent.EventType.READY, this)));
    }

    @EventListener(WarmThumbnailCacheEvent.class)
    public void warmThumbnailCache(final WarmThumbnailCacheEvent event) {
        taskService.supply(new AbstractTask<Void>() {
            @Override
            protected Void call() throws Exception {
                log.info("Warm Cache for: '{}'", event.getCatalog().path());
                updateTitle("Warm thumbnails cache");
                Set<MediaFile> medias = event.getCatalog().medias();
                updateProgress(0, medias.size());
                EntryStream.of(new ArrayList<>(medias))
                           .forEach(entry -> {
                               var thumbnail = thumbnailCache.get(entry.getValue().id(), Thumbnail.class);
                               if (thumbnail == null) {
                                   thumbnail = persistenceService.findByPathOrId(entry.getValue()).orElse(null);
                                   if (thumbnail != null) {
                                       thumbnailCache.put(entry.getValue().id(), thumbnail);
                                       updateMessage("Thumbnail loaded: " + entry.getValue().fullPath().getFileName());
                                   }
                                   updateProgress(entry.getKey(), medias.size());
                               }
                           });
                return null;
            }
        });
    }

    record ThumbnailRes(MediaFile file, Thumbnail thumbnail, boolean requirePersistance) {
        void updateThumbnail() {
            if (!Platform.isFxApplicationThread()) {
                throw new RuntimeException("This call must be done from JavaFx Thread (and not from: " + Thread.currentThread().getName() + ")");
            }
            file.getThumbnail().set(thumbnail);
        }
    }

    Task<List<MediaFile>> thumbnailBatch(Catalog catalog, final List<MediaFile> files) {
        return new AbstractTask<>() {
            //            StopWatch w = new StopWatch("Load Task");
            @Override
            protected List<MediaFile> call() throws Exception {
                var size = files.size();
//                w.start("Build Thumbnail");
                log.debug("Generate a batch of '{}' thumbnails for: {}", size, catalog.path());
                updateTitle("Thumbnail generation for '" + size + " images");
                updateProgress(0, size);
                final LocalDate now = LocalDate.now();

                return StreamEx.ofSubLists(EntryStream.of(files).toList(), 20)
                               .map(ts -> ts.stream()
                                            .map(mf -> {
                                                // First Memory Cache
                                                var file      = mf.getValue();
                                                var thumbnail = thumbnailCache.get(file.id(), Thumbnail.class);
                                                updateProgress(mf.getKey(), size);
                                                if (thumbnail == null) {
                                                    // Second Database Cache
                                                    thumbnail = persistenceService.findByPathOrId(file).orElse(null);
                                                    if (thumbnail == null) {
                                                        var image = loadThumbnail(file.getId(), file.getFullPath());
                                                        thumbnail = Thumbnail.builder().id(file.id())
                                                                                       .fullPath(file.fullPath())
                                                                                       .thumbnail(image)
                                                                                       .hashDate(now)
                                                                                       .lastAccess(now)
                                                                                       .origin(EThumbnailStatus.GENERATED)
                                                                                       .build();
                                                        return new ThumbnailRes(mf.getValue(), thumbnail, true);
                                                    } else {
                                                        return new ThumbnailRes(mf.getValue(), thumbnail, false);
                                                    }
                                                } else {
                                                    return new ThumbnailRes(mf.getValue(), thumbnail, false);
                                                }
                                            })
                                            .toList())
                               .flatMap(thumbails -> {
                                            persistenceService.saveAll(thumbails.stream()
                                                                                .filter(tr -> tr.requirePersistance)
                                                                                .map(ThumbnailRes::thumbnail)
                                                                                .toList());
                                            return thumbails.stream().map(tagRepository -> tagRepository.file);
                                        }
                               )
                               .toList();
//                for (MediaFile mf : files) {
//                    i++;
//                    var thumbnail = thumbnailCache.get(mf.id(), Thumbnail.class);
//                    if (thumbnail == null) {
//                        res.add(loadThumbnail(mf));
//                    } else {
//                        res.add(new ThumbnailRes(mf, thumbnail));
//                    }
//
//                }
//
//                return res;
            }

            @Override
            protected void succeeded() {
//                getValue().forEach(ThumbnailRes::updateThumbnail);
            }
        };
    }
}
