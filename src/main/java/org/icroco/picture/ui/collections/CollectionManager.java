package org.icroco.picture.ui.collections;

import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.ui.event.CollectionsLoadedEvent;
import org.icroco.picture.ui.event.FilesChangesDetectedEvent;
import org.icroco.picture.ui.model.MediaCollection;
import org.icroco.picture.ui.model.MediaFile;
import org.icroco.picture.ui.persistence.PersistenceService;
import org.icroco.picture.ui.task.AbstractTask;
import org.icroco.picture.ui.task.TaskService;
import org.icroco.picture.ui.util.Constant;
import org.icroco.picture.ui.util.DirectoryWatcher;
import org.icroco.picture.ui.util.metadata.IMetadataExtractor;
import org.icroco.picture.ui.util.metadata.MetadataHeader;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class CollectionManager {
    private final IMetadataExtractor metadataExtractor;
    private final TaskService        taskService;
    private final DirectoryWatcher   directoryWatcher;
    private final PersistenceService persistenceService;

    @EventListener(CollectionsLoadedEvent.class)
    public void collectionLoaded(CollectionsLoadedEvent event) {
        taskService.supply(() -> {
            event.getMediaCollections()
                 .stream()
                 .map(MediaCollection::path)
                 .forEach(directoryWatcher::registerAll);

            log.info("End of adding directory watcher");
        });


        taskService.supply(analyseCollections(List.copyOf(event.getMediaCollections())))
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
        var difference  = org.icroco.picture.ui.util.Collections.difference(recordedFiles, currenFiles);

        difference.leftMissing().forEach(path -> log.info("Collections: '{}', file added: {}", mediaCollection.path(), path));
        difference.rightMissing().forEach(path -> log.info("Collections: '{}', file deleted: {}", mediaCollection.path(), path));
        //TODO: Add files updated.

        if (difference.isNotEmpty()) {
            updateCollection(mediaCollection.id(), difference.leftMissing(), difference.rightMissing(), Collections.emptyList());
        }
    }

    public void updateCollection(int collectionId, Collection<Path> toBeAdded, Collection<Path> toBeDeleted, Collection<Path> hasBeenModified) {
        var now = LocalDate.now();
        persistenceService.updateCollection(collectionId,
                                            toBeAdded.stream().map(p -> create(now, p)).toList(),
                                            toBeDeleted.stream().map(p -> MediaFile.builder().fullPath(p).build()).toList());
        // TODO: Process hasBeenModified.
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

    private MediaFile create(LocalDate now, Path p) {
        final var h = metadataExtractor.header(p);

        return MediaFile.builder()
                .fullPath(p)
                .fileName(p.getFileName().toString())
                .thumbnailUpdateProperty(new SimpleObjectProperty<>(LocalDateTime.MIN))
                .hashDate(now)
                .originalDate(h.map(MetadataHeader::orginalDate).orElse(LocalDateTime.now()))
                .build();
    }


    @EventListener(FilesChangesDetectedEvent.class)
    public void filesChangeDetected(FilesChangesDetectedEvent event) {

        groupByCollection(event.getCreated()).forEach((mediaCollection, files) -> updateCollection(mediaCollection.id(),
                                                                                                   files,
                                                                                                   Collections.emptyList(),
                                                                                                   Collections.emptyList()));
        groupByCollection(event.getDeleted()).forEach((mediaCollection, files) -> updateCollection(mediaCollection.id(),
                                                                                                   Collections.emptyList(),
                                                                                                   files,
                                                                                                   Collections.emptyList()));
        groupByCollection(event.getModified()).forEach((mediaCollection, files) -> updateCollection(mediaCollection.id(),
                                                                                                    Collections.emptyList(),
                                                                                                    Collections.emptyList(),
                                                                                                    files));
    }

    private Map<MediaCollection, List<Path>> groupByCollection(Collection<Path> files) {
        record PathAndCollection(Path path, MediaCollection mediaCollection) {}

        return files.stream()
                    .map(path -> new PathAndCollection(path, persistenceService.findMediaCollectionForFile(path).orElse(null)))
                    .filter(it -> it.mediaCollection != null)
                    .collect(Collectors.groupingBy(pathAndCollection -> pathAndCollection.mediaCollection,
                                                   Collectors.mapping(PathAndCollection::path, Collectors.toList())));
    }


}
