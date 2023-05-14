package org.icroco.picture.ui.collections;

import javafx.concurrent.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.ui.config.ImageInConfiguration;
import org.icroco.picture.ui.event.CollectionsLoadedEvent;
import org.icroco.picture.ui.model.MediaCollection;
import org.icroco.picture.ui.model.MediaFile;
import org.icroco.picture.ui.persistence.PersistenceService;
import org.icroco.picture.ui.task.AbstractTask;
import org.icroco.picture.ui.task.TaskService;
import org.icroco.picture.ui.util.Constant;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.cache.Cache;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class CollectionManager {
    @Qualifier(ImageInConfiguration.CATALOG)
    private final Cache              cache;
    private final PersistenceService service;
    private final TaskService        taskService;

    @EventListener(ApplicationStartedEvent.class)
    public void loadAllCatalog() {
        List<MediaCollection> mediaCollections = service.findAllCatalog();
        for (MediaCollection c : mediaCollections) {
            cache.put(c.id(), c);
        }
        taskService.fxNotifyLater(new CollectionsLoadedEvent(mediaCollections, this));
    }

    private void applicationShowingUp(CollectionsLoadedEvent event) {
        taskService.supply(analyseCollections(event.getMediaCollections()))
                   .thenRun(() -> {
                       log.info("TODO: Add a timer to rescan folders on regular basics");
                   });
    }

    Task<Void> analyseCollections(final List<MediaCollection> mediaCollections) {
        return new AbstractTask<>() {
            @Override
            protected Void call() throws Exception {
                log.info("Synching '{}' mediaCollections", mediaCollections.size());
                updateTitle("Synching '" + mediaCollections.size() + "' mediaCollections");
                updateProgress(0, mediaCollections.size());

                mediaCollections.forEach(CollectionManager.this::computeDiff);

                return null;
            }
        };
    }


    private void computeDiff(MediaCollection mediaCollection) {
        var recordedFiles = mediaCollection.medias()
                                           .stream()
                                           .map(MediaFile::getFullPath)
                                           .collect(Collectors.toSet());
        var currenFiles = Set.copyOf(getAllFiles(mediaCollection.path()));

        var difference = org.icroco.picture.ui.util.Collections.difference(recordedFiles, currenFiles);

        difference.leftMissing().forEach(path -> log.info("Collections: '{}', file added: {}", mediaCollection.path(), path));
        difference.rightMissing().forEach(path -> log.info("Collections: '{}', file deleted: {}", mediaCollection.path(), path));
    }

    List<Path> getAllFiles(Path rootPath) {
        try (var images = Files.walk(rootPath)) {
            return images.filter(p -> !Files.isDirectory(p))   // not a directory
                         .map(Path::normalize)
                         .filter(Constant::isSupportedExtension)
                         .toList();
        }
        catch (IOException e) {
            log.error("Cannot walk through directory: '{}'", rootPath);
            return Collections.emptyList();
        }
    }

}
