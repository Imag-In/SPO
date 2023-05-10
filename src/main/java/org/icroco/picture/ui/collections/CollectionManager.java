package org.icroco.picture.ui.collections;

import javafx.concurrent.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.ui.event.MediaCollectionsLoadedEvent;
import org.icroco.picture.ui.model.Catalog;
import org.icroco.picture.ui.model.MediaFile;
import org.icroco.picture.ui.task.AbstractTask;
import org.icroco.picture.ui.task.TaskService;
import org.icroco.picture.ui.util.Constant;
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
    private final TaskService taskService;

    @EventListener(MediaCollectionsLoadedEvent.class)
    public void applicationShowingUp(MediaCollectionsLoadedEvent event) {
        taskService.supply(analyseCollections(event.getCatalogs()))
                   .thenRun(() -> {
                       log.info("TODO: Add a timer to rescan folders on regular basics");
                   });
    }

    Task<Void> analyseCollections(final List<Catalog> catalogs) {
        return new AbstractTask<>() {
            @Override
            protected Void call() throws Exception {
                log.info("Synching '{}' catalogs", catalogs.size());
                updateTitle("Synching '" + catalogs.size() + "' catalogs");
                updateProgress(0, catalogs.size());

                catalogs.forEach(CollectionManager.this::computeDiff);

                return null;
            }
        };
    }


    private void computeDiff(Catalog catalog) {
        var recordedFiles = catalog.medias()
                                   .stream()
                                   .map(MediaFile::getFullPath)
                                   .collect(Collectors.toSet());
        var currenFiles = Set.copyOf(getAllFiles(catalog.path()));

        var difference = org.icroco.picture.ui.util.Collections.difference(recordedFiles, currenFiles);

        difference.leftMissing().forEach(path -> log.info("Collections: '{}', file added: {}", catalog.path(), path));
        difference.rightMissing().forEach(path -> log.info("Collections: '{}', file deleted: {}", catalog.path(), path));
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
