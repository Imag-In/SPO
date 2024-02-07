package org.icroco.picture.views;

import javafx.application.Platform;
import javafx.concurrent.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.EntryStream;
import org.icroco.picture.event.*;
import org.icroco.picture.event.FilesChangesDetectedEvent.PathItem;
import org.icroco.picture.hash.IHashGenerator;
import org.icroco.picture.metadata.IMetadataExtractor;
import org.icroco.picture.model.HashDuplicate;
import org.icroco.picture.model.MediaCollection;
import org.icroco.picture.model.MediaCollectionEntry;
import org.icroco.picture.model.MediaFile;
import org.icroco.picture.persistence.CollectionRepository;
import org.icroco.picture.persistence.PersistenceService;
import org.icroco.picture.util.Constant;
import org.icroco.picture.util.FileUtil;
import org.icroco.picture.util.LangUtils;
import org.icroco.picture.util.UserAbortedException;
import org.icroco.picture.views.task.AbstractTask;
import org.icroco.picture.views.task.ModernTask;
import org.icroco.picture.views.task.TaskService;
import org.icroco.picture.views.util.DirectoryWatcher;
import org.springframework.context.event.EventListener;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.icroco.picture.util.LangUtils.getAllFiles;

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
            log.debug("End of adding directory watcher");
        });

        taskService.supply(analyseCollections(List.copyOf(event.getMediaCollections())));
    }

    Task<Void> analyseCollections(final List<MediaCollection> mediaCollections) {
        persistenceService.cleanOrphans();
        return new AbstractTask<>() {
            @Override
            protected Void call() {
                log.info("Synching '{}' mediaCollections", mediaCollections.size());
                updateTitle(STR."Synching '\{mediaCollections.size()}' mediaCollections");
                updateProgress(0, mediaCollections.size());
                mediaCollections.forEach(CollectionManager.this::computeDiff);
                return null;
            }
        };
    }

    /**
     * Check if some root path of collection are still present (for instance if external HD or network mount point).
     */
    @Scheduled(fixedDelay = 60_000)
    void checkRootPath() {
        var statuses = persistenceService.findAllIdAndPath()
                                         .stream()
                                         .collect(Collectors.toMap(CollectionRepository.IdAndPath::getId,
                                                                   idAndPath -> Files.exists(idAndPath.getPath())));

        taskService.sendEvent(CollectionsStatusEvent.builder()
                                                    .statuses(statuses)
                                                    .source(this)
                                                    .build());
    }


    private void computeDiff(MediaCollection mediaCollection) {
        if (Files.exists(mediaCollection.path())) {
            mediaCollection.setConnected(true);
            var recordedFiles = mediaCollection.medias()
                                               .stream()
                                               .map(MediaFile::getFullPath)
                                               .map(path -> new PathItem(path, Files.isDirectory(path)))
                                               .collect(Collectors.toSet());
            var currenFiles = Set.copyOf(getAllFiles(mediaCollection.path()).stream()
                                                                            .map(path -> new PathItem(path, Files.isDirectory(path)))
                                                                            .toList());
            var difference = org.icroco.picture.views.util.Collections.difference(recordedFiles, currenFiles);

            difference.leftMissing().forEach(path -> log.info("Collections: '{}', file added: {}", mediaCollection.path(), path));
            difference.rightMissing().forEach(path -> log.info("Collections: '{}', file deleted: {}", mediaCollection.path(), path));
            //TODO: Add files updated.

            if (difference.isNotEmpty()) { // TODO: implements update.
                updateCollection(mediaCollection.id(), difference.leftMissing(), difference.rightMissing(), Collections.emptyList());
            }
            // TODO: Check empty sub dir and clean database / ui.
        } else {
            log.warn("Media Collection dir is not found: '{}'", mediaCollection.path());
            mediaCollection.setConnected(false);
        }
    }

    private void updateCollection(int collectionId,
                                  Collection<PathItem> toBeAdded,
                                  Collection<PathItem> toBeDeleted,
                                  Collection<PathItem> hasBeenModified) {
        var now = LocalDate.now();
        hasBeenModified = new ArrayList<>(hasBeenModified);
        hasBeenModified.removeAll(toBeDeleted); // To make sure !
        var res = persistenceService.updateCollection(collectionId,
                                                      toBeAdded.stream()
                                                               .filter(PathItem::isRegularFile)
                                                               .map(PathItem::path)
                                                               .flatMap(p -> create(now, p, false).stream())
                                                               .collect(Collectors.toSet()),
                                                      toBeDeleted.stream()
                                                                 .filter(PathItem::isRegularFile)
                                                                 .map(PathItem::path)
                                                                 .map(p -> MediaFile.builder().fullPath(p).build())
                                                                 .collect(Collectors.toSet()),
                                                      reloadMetadate(persistenceService.getMediaCollection(collectionId),
                                                                     hasBeenModified.stream()
                                                                                    .filter(PathItem::isRegularFile)
                                                                                    .map(PathItem::path)
                                                                                    .toList(),
                                                                     now));

        var mceIds = persistenceService.removeSubDirFromCollection(collectionId, toBeDeleted.stream()
                                                                                            .filter(PathItem::isDirectory)
                                                                                            .map(PathItem::path)
                                                                                            .collect(Collectors.toSet()));
        taskService.sendEvent(CollectionUpdatedEvent.builder()
                                                    .mcId(collectionId)
                                                    .newItems(res.added())
                                                    .deletedItems(res.deleted())
                                                    .modifiedItems(res.updated())
                                                    .subPathDeletedIds(mceIds)
                                                    .source(this)
                                                    .build());
        taskService.sendEvent(CustomExtractThumbnailEvent.builder()
                                                         .mcId(collectionId)
                                                         .newItems(res.added())
                                                         .modifiedItems(res.updated())
                                                         .source(this)
                                                         .build());

        // TODO: Send event to clean collection subdir nodes.
    }

    private Set<MediaFile> reloadMetadate(MediaCollection mediaCollection, Collection<Path> hasBeenModified, LocalDate now) {
        var mfUpdated = mediaCollection.medias()
                                       .stream()
                                       .filter(mf -> hasBeenModified.contains(mf.fullPath()))
                                       .collect(Collectors.toSet());

        // We skip if changes are only about file date changes.
        return mfUpdated.stream()
                        .peek(mf -> log.atDebug()
                                       .log(() -> "Hash: '%s', newHash: '%s'".formatted(mf.getHash(),
                                                                                        hashGenerator.compute(mf.fullPath()).orElse(""))))
//                        .filter(mf -> !Objects.equals(mf.getHash(), hashGenerator.compute(mf.fullPath()).orElse(null)))
                        .map(mf -> create(now, mf.fullPath(), true)
                                // TODO: do not create a new MedialFile pointer, copy new one into current.
                                .filter(newMf -> MediaFile.UPDATED_COMP.compare(newMf, mf) != 0)
                                .map(newMf -> {
                                    mf.initFrom(newMf);
                                    return mf;
                                })
                                .orElse(null))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
    }

    public Optional<MediaFile> create(LocalDate now, Path p, boolean generateHash) {
        var builder = MediaFile.builder()
                               .fullPath(p)
                               .fileName(p.getFileName().toString());

        if (generateHash) {
            builder.hash(hashGenerator.compute(p).orElse(""))
                   .hashDate(now);
        }


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
        Thread.ofVirtual().name("FileUpdatedInNg").start(() -> { // operation can be long, we do not block the event bus.
            groupByCollection(event.getCreated())
                    .forEach((mediaCollection, files) -> updateCollection(mediaCollection.id(),
                                                                          files,
                                                                          Collections.emptyList(),
                                                                          Collections.emptyList()));
            groupByCollection(event.getDeleted())
                    .forEach((mediaCollection, files) -> updateCollection(mediaCollection.id(),
                                                                          Collections.emptyList(),
                                                                          files,
                                                                          Collections.emptyList()));
            groupByCollection(event.getModified())
                    .forEach((mediaCollection, files) -> updateCollection(mediaCollection.id(),
                                                                          Collections.emptyList(),
                                                                          Collections.emptyList(),
                                                                          files));
        });
    }

    @EventListener
    public void mediaFileUpdate(UpdateMedialFileEvent event) {
//        persistenceService.saveMediaFile(event.getMediaFile().getCollectionId(), event.getMediaFile());
        persistenceService.saveMediaFile(event.getMediaFile());
        Platform.runLater(() -> event.getMediaFile().setLastUpdated(LocalDateTime.now()));
    }

    private Map<MediaCollection, List<PathItem>> groupByCollection(Collection<PathItem> files) {
        record PathAndCollection(PathItem path, MediaCollection mediaCollection) {
        }

        return files.stream()
                    .map(pi -> new PathAndCollection(pi, persistenceService.findMediaCollectionForFile(pi.path()).orElse(null)))
                    .filter(it -> it.mediaCollection != null)
                    .collect(Collectors.groupingBy(pathAndCollection -> pathAndCollection.mediaCollection,
                                                   Collectors.mapping(PathAndCollection::path, Collectors.toList())));
    }

    public Task<MediaCollection> newCollection(@NonNull Path selectedDirectory, Function<Long, Boolean> askConfirmation) {
        var task = scanDirectory(selectedDirectory, askConfirmation);
        taskService.supply(task);
        return task;
    }

    private Task<MediaCollection> scanDirectory(Path rootPath, Function<Long, Boolean> askConfirmation) {
        return ModernTask.<MediaCollection>builder()
                         .execute(myself -> {
                             myself.updateTitle(STR."Scanning directory: \{rootPath.getFileName()}");
                             myself.updateMessage("%s: scanning".formatted(rootPath));
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
                             final var size           = filteredImages.size();
                             myself.updateProgress(0, size);
                             var mc = MediaCollection.builder()
                                                     .path(rootPath)
                                                     .subPaths(children)
                                                     .medias(EntryStream.of(List.copyOf(filteredImages))
                                                                        .peek(i -> myself.updateProgress(i.getKey(), size))
                                                                        .flatMap(i -> create(now, i.getValue(), false).stream())
                                                                        .collect(Collectors.toSet()))
                                                     .build();

                             return taskService.runAndWait(() -> askConfirmation.apply((long) size), false)
                                               .filter(b -> b)
                                               .map(_ -> {
                                                   var mcSaved = persistenceService.saveCollection(mc);
                                                   taskService.sendEvent(ExtractThumbnailEvent.builder()
                                                                                              .mcId(mcSaved.id())
                                                                                              .update(false)
                                                                                              .source(this)
                                                                                              .build());
                                                   directoryWatcher.registerAll(mcSaved.path());
                                                   return mcSaved;
                                               })
                                               .orElseThrow(() -> new UserAbortedException(STR."User aborted action, collection too large: \{size}"));
                         })
                         .onSuccess((myself, mediaCollection) -> {
                             log.info("Collections entries: {}, time: {}",
                                      mediaCollection.medias().size(),
                                      LangUtils.wordBased(myself.getDuration()));
                             mediaCollection.setConnected(Files.exists(rootPath));
                         })
                         .onFailed(throwable -> {
                             log.error("While scanning dir: '{}'", rootPath, throwable);
                         })
                         .build();
    }

    public Task<Set<Path>> scanDirectory(Path rootPath, boolean resursiveScan) {
        return new AbstractTask<>() {
            @Override
            protected Set<Path> call() {
                updateTitle(STR."Scanning directory: \{rootPath.getFileName()}");
                updateMessage("%s: scanning".formatted(rootPath));
                updateProgress(-1, -1);
                return scanDir(rootPath, resursiveScan);
            }
        };
    }

    public Set<Path> scanDir(Path rootPath, boolean resursiveScan) {
        try (var images = Files.walk(rootPath, resursiveScan ? Integer.MAX_VALUE : 1)) {
            return images.filter(p -> !Files.isDirectory(p))
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
        deleteCollection(entry, () -> {
        });
    }

    public void deleteCollection(final MediaCollection mc, Runnable onFinisHandler) {
        taskService.supply(ModernTask.<Void>builder()
                                     .execute(myself -> {
                                         myself.updateProgress(-1, -1);
                                         myself.updateTitle("Delete collection: '%s', '%s' files".formatted(mc.id(),
                                                                                                            mc.medias().size()));
                                         taskService.sendEvent(CollectionEvent.builder()
                                                                              .mcId(mc.id())
                                                                              .type(CollectionEvent.EventType.DELETED)
                                                                              .subDirs(mc.subPaths()
                                                                                         .stream()
                                                                                         .map(MediaCollectionEntry::name)
                                                                                         .toList())
                                                                              .source(this)
                                                                              .build());
                                         myself.updateMessage("Deleting database items ...");
                                         persistenceService.deleteMediaCollection(mc.id());
                                         return null;
                                     })
                                     .onFinished(onFinisHandler)
                                     .build());
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
            protected List<MediaFile> call() {
                var size = mediaFiles.size();
                updateTitle("Hashing %s files. %d/%d ".formatted(size, batchId, nbBatches));
                updateProgress(0, size);
//                updateMessage("%s: scanning".formatted(rootPath));
                for (int i = 0; i < size; i++) {
                    var mf = mediaFiles.get(i);
                    updateProgress(i, size);
                    updateMessage(STR."Hashing: \{mf.getFullPath().getFileName()}");
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

    public List<HashDuplicate> findDuplicateByHash() {
        return persistenceService.findDuplicateByHash();
    }
}
