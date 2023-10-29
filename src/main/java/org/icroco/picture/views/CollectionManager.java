package org.icroco.picture.views;

import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.EntryStream;
import org.icroco.picture.event.CollectionEvent;
import org.icroco.picture.event.CollectionsLoadedEvent;
import org.icroco.picture.event.ExtractThumbnailEvent;
import org.icroco.picture.event.FilesChangesDetectedEvent;
import org.icroco.picture.hash.IHashGenerator;
import org.icroco.picture.metadata.IMetadataExtractor;
import org.icroco.picture.model.MediaCollection;
import org.icroco.picture.model.MediaCollectionEntry;
import org.icroco.picture.model.MediaFile;
import org.icroco.picture.persistence.PersistenceService;
import org.icroco.picture.util.FileUtil;
import org.icroco.picture.views.task.AbstractTask;
import org.icroco.picture.views.task.TaskService;
import org.icroco.picture.views.util.Constant;
import org.icroco.picture.views.util.DirectoryWatcher;
import org.springframework.context.event.EventListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
public class CollectionManager {
    private final IMetadataExtractor metadataExtractor;
    private final TaskService        taskService;
    private final DirectoryWatcher   directoryWatcher;
    private final PersistenceService persistenceService;
    private final IHashGenerator     hashGenerator;

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
                   .thenRun(() -> log.info("TODO: Add a timer to rescan folders on regular basics"));
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
        var difference  = org.icroco.picture.views.util.Collections.difference(recordedFiles, currenFiles);

        difference.leftMissing().forEach(path -> log.info("Collections: '{}', file added: {}", mediaCollection.path(), path));
        difference.rightMissing().forEach(path -> log.info("Collections: '{}', file deleted: {}", mediaCollection.path(), path));
        //TODO: Add files updated.

        if (difference.isNotEmpty()) {
            updateCollection(mediaCollection.id(), difference.leftMissing(), difference.rightMissing(), Collections.emptyList());
        }
    }

    public void updateCollection(int collectionId,
                                 Collection<Path> toBeAdded,
                                 Collection<Path> toBeDeleted,
                                 Collection<Path> hasBeenModified) {
        var now = LocalDate.now();
        persistenceService.updateCollection(collectionId,
                                            toBeAdded.stream().flatMap(p -> create(now, p).stream()).toList(),
                                            toBeDeleted.stream().map(p -> MediaFile.builder().fullPath(p).build()).toList(),
                                            true);
        // TODO: Process hasBeenModified.
    }

    List<Path> getAllFiles(Path rootPath) {
        try (var images = Files.walk(rootPath)) {
            return images.filter(p -> !Files.isDirectory(p))   // not a directory
                         .map(Path::normalize)
                         .filter(Constant::isSupportedExtension)
                         .toList();
        } catch (IOException e) {
            log.error("Cannot walk through directory: '{}'", rootPath);
            return Collections.emptyList();
        }
    }

    public Optional<MediaFile> create(LocalDate now, Path p) {
        var builder = MediaFile.builder()
                               .fullPath(p)
                               .fileName(p.getFileName().toString())
                               .thumbnailUpdateProperty(new SimpleObjectProperty<>(LocalDateTime.MIN))
//                               .hash(hashGenerator.compute(p).orElse("")) // TODO: Hash later.
//                               .hashDate(now); // TODO: Hash later.
                ;

        return metadataExtractor.header(p)
                                .map(header -> builder.dimension(header.size())
                                                      .orientation(header.orientation())
                                                      .geoLocation(header.geoLocation())
                                                      .camera(header.camera())
                                                      .originalDate(header.orginalDate())
                                                      .keywords(header.keywords())
                                                      .build()
                                )
                                .or(() -> {
                                    log.error("Files not created because we cannot read header: '{}'", p);
                                    return Optional.empty();
                                });
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
        record PathAndCollection(Path path, MediaCollection mediaCollection) {
        }

        return files.stream()
                    .map(path -> new PathAndCollection(path, persistenceService.findMediaCollectionForFile(path).orElse(null)))
                    .filter(it -> it.mediaCollection != null)
                    .collect(Collectors.groupingBy(pathAndCollection -> pathAndCollection.mediaCollection,
                                                   Collectors.mapping(PathAndCollection::path, Collectors.toList())));
    }

    public Task<MediaCollection> newCollection(@NonNull Path selectedDirectory) {
        var task = scanDirectory(selectedDirectory);
        taskService.supply(task);
        return task;
    }

    private Task<MediaCollection> scanDirectory(Path rootPath) {
        return new AbstractTask<>() {
            @Override
            protected MediaCollection call() throws Exception {
                updateTitle("Scanning directory: " + rootPath.getFileName());
                updateMessage("%s: scanning".formatted(rootPath));
                Set<MediaCollectionEntry> children;
                var                       now = LocalDate.now();
                try (var s = Files.walk(rootPath)) {
                    children = s.filter(Files::isDirectory)
                                .map(Path::normalize)
                                .filter(p -> !p.equals(rootPath))
                                .filter(FileUtil::isLastDir)
                                .map(p -> MediaCollectionEntry.builder().name(p).build())
                                .collect(Collectors.toSet());
                }
                final var filteredImages = scanDir(rootPath, true);
                    final var size = filteredImages.size();
                    updateProgress(0, size);
                var mc = MediaCollection.builder()
                                        .path(rootPath)
                                            .subPaths(children)
                                        .medias(EntryStream.of(List.copyOf(filteredImages))
                                                               .peek(i -> updateProgress(i.getKey(), size))
                                                           .flatMap(i -> create(now, i.getValue()).stream())
                                                               .collect(Collectors.toSet()))
                                            .build();
                    mc = persistenceService.saveCollection(mc);
                    taskService.sendEvent(ExtractThumbnailEvent.builder()
                                                               .mediaCollection(mc)
                                                               .source(this)
                                                               .build());
                    directoryWatcher.registerAll(mc.path());
                    return mc;
            }

            @Override
            protected void succeeded() {
                var catalog = getValue();
                log.info("Collections entries: {}, time: {}", catalog.medias().size(), System.currentTimeMillis() - start);
            }

            @Override
            protected void failed() {
                log.error("While scanning dir: '{}'", rootPath, getException());
                super.failed();
            }
        };
    }

    public Task<Set<Path>> scanDirectory(Path rootPath, boolean resursiveScan) {
        return new AbstractTask<>() {
            @Override
            protected Set<Path> call() throws Exception {
                updateTitle("Scanning directory: " + rootPath.getFileName());
                updateMessage("%s: scanning".formatted(rootPath));
                Set<MediaCollectionEntry> children;
                updateProgress(-1, -1);
                return scanDir(rootPath, resursiveScan);
            }
        };
    }

    private Set<Path> scanDir(Path rootPath, boolean resursiveScan) {
        try (var images = Files.walk(rootPath, resursiveScan ? Integer.MAX_VALUE : 1)) {
            return images.filter(p -> !Files.isDirectory(p))   // not a directory
                         .filter(Constant::isSupportedExtension)
                         .filter(metadataExtractor::isFileTypeSupported)
                         .map(Path::normalize)
                         .collect(Collectors.toSet());
        } catch (IOException e) {
            log.error("Unexpected error", e);
            return Collections.emptySet();
        }
    }

    public void deleteCollection(final MediaCollection entry) {
        taskService.supply(() -> {
            persistenceService.deleteMediaCollection(entry.id());
            taskService.sendEvent(CollectionEvent.builder()
                                                 .mediaCollection(entry)
                                                 .type(CollectionEvent.EventType.DELETED)
                                                 .source(this)
                                                 .build());
        });
    }

    public Optional<Path> isSameOrSubCollection(Path rootPath) {
        return persistenceService.findAllMediaCollection()
                                 .stream()
                                 .filter(mc -> rootPath.startsWith(mc.path()))
                                 .findFirst()
                                 .stream()
                                 .flatMap(mc -> Stream.concat(Stream.of(rootPath),
                                                              mc.subPaths().stream().map(e -> mc.path().resolve(e.name()))))
                                 .filter(p -> p.equals(rootPath))
                                 .findFirst();
    }

    // TODO: move outside of this class, like collectionmanager.
    private Task<List<MediaFile>> hashFiles(final List<MediaFile> mediaFiles, final int batchId, final int nbBatches) {
        return new AbstractTask<>() {
            @Override
            protected List<MediaFile> call() throws Exception {
                var size = mediaFiles.size();
                updateTitle("Hashing %s files. %d/%d ".formatted(size, batchId, nbBatches));
                updateProgress(0, size);
//                updateMessage("%s: scanning".formatted(rootPath));
                for (int i = 0; i < size; i++) {
                    var mf = mediaFiles.get(i);
                    updateProgress(i, size);
                    updateMessage("Hashing: " + mf.getFullPath().getFileName());
                    mf.setHash(hashGenerator.compute(mf.getFullPath()).orElse(null));
                }
                return mediaFiles;
            }

            @Override
            protected void succeeded() {
                log.info("'{}' files hashing time: {}", mediaFiles.size(), System.currentTimeMillis() - start);
//            // We do not expand now, we're waiting thumbnails creation.
//            createTreeView(mediaCollection);
//            disablePathActions.set(false);
//            taskService.notifyLater(new ExtractThumbnailEvent(mediaCollection, this));
            }
        };
    }

}
