package org.icroco.picture.views.util;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.config.SpoConfiguration;
import org.icroco.picture.event.CollectionEvent;
import org.icroco.picture.event.FilesChangesDetectedEvent;
import org.icroco.picture.event.FilesChangesDetectedEvent.PathItem;
import org.icroco.picture.model.EFileType;
import org.icroco.picture.persistence.CollectionRepository;
import org.icroco.picture.util.FileUtil;
import org.icroco.picture.util.LangUtils;
import org.icroco.picture.views.task.TaskService;
import org.jooq.lambda.Unchecked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.*;

@Slf4j
@Component
// TODO: clean watched dir when a collection is removed.
public class DirectoryWatcher {
    private final WatchService        watcher;
    private final boolean             recursive;
    private final TaskService         taskService;
    private final Set<FileChange>     changes      = ConcurrentHashMap.newKeySet();
    private final Map<Path, WatchKey> keys         = new ConcurrentHashMap<>();
    private       Thread              drainerVThread;
    private final Predicate<Path>     excludeFiles = p -> p.getFileName().equals(Path.of(".DS_Store"));
    private final AtomicBoolean suspend = new AtomicBoolean(false);

    @Autowired
    public DirectoryWatcher(TaskService taskService,
                            CollectionRepository repository,
                            @Qualifier(SpoConfiguration.DIRECTORY_WATCHER) ExecutorService executorService) throws IOException {
        this(taskService, null, true);
        executorService.submit(this::processEvents);
        drainerVThread = Thread.ofVirtual()
                               .name("File-Watcher-drainer")
                               .start(this::drainFiles);
    }

    @PreDestroy
    private void destroy() {
        drainerVThread.interrupt();
    }

    /**
     * Creates a WatchService and registers the given directory
     */
    DirectoryWatcher(TaskService taskService, @Nullable Path dir, boolean recursive) throws IOException {
        this.taskService = taskService;
        this.watcher = FileSystems.getDefault().newWatchService();
        this.recursive = recursive;

        if (dir != null) {
            if (recursive) {
                log.debug("Scanning '{}'", dir);
                registerAll(dir);
            } else {
                register(dir);
            }
        }
    }

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    /**
     * Register the given directory with the WatchService
     */
    private void register(Path dir) {
        // TODO: Filter out based on a list of name.
        if (!excludeFiles.test(dir.getFileName())) {
            keys.computeIfAbsent(dir, Unchecked.function(d -> d.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)));
            log.debug("Watch: '{}'", dir);
        }
    }

    /**
     * Register the given directory, and all its subdirectories, with the
     * WatchService.
     */
    public void registerAll(final Path start) {
        // register directory and subdirectories
        try {
            Files.walkFileTree(start, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                        throws IOException {
                    register(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (NoSuchFileException e) {
            log.warn("Directory doest not exist. Make sure to mount external or remote drive '{}'", start);
        } catch (IOException e) {
            log.error("Cannot walk through '{}'", start, e);
        }
    }

    enum FileChangeType {
        CREATED,
        DELETED,
        MODIFIED
    }

    record FileChange(Path path, FileChangeType type, boolean isDirectory) {
        PathItem toPathItem() {
            return new PathItem(path, isDirectory);
        }
    }

    /**
     * Process all events for keys queued to the watcher
     */
    void processEvents() {
        try {
            //noinspection InfiniteLoopStatement
            for (; ; ) {
                // wait for key to be signalled
                WatchKey key;
                try {
                    key = watcher.take();
                } catch (InterruptedException x) {
                    log.warn("Thread interrupted. Directory watcher is disabled ");
                    continue;
                }

                if (key != null) {
                    log.debug("Watch: {}", key.watchable());
                    Path path = (Path) key.watchable();
                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        // TBD - provide example of how OVERFLOW event is handled
                        if (kind == OVERFLOW) {
                            continue;
                        }

                        // Context for directory entry event is the file name of entry
                        WatchEvent<Path> ev    = cast(event);
                        Path             name  = ev.context();
                        Path             child = path.resolve(name);


                        // print out event
                        log.debug("{}: {}", event.kind().name(), child);

                        // if directory is created, and watching recursively, then
                        // register it and its subdirectories
                        if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                            if (kind == ENTRY_CREATE && recursive) {
                                log.info("Watch new dir: {}", child);
                                registerAll(child);
                                changes.add(new FileChange(child, FileChangeType.CREATED, true));
//                            } else if (kind == ENTRY_DELETE) {
//                                log.info("Un-watch dir: {}", child);
//                                changes.add(new FileChange(child, FileChangeType.CREATED, true));
                            } else if (kind == ENTRY_MODIFY) {
                                log.debug("Dir modified: {}", child);
                            }
                        } else {
                            if (kind == ENTRY_CREATE) {
                                changes.add(new FileChange(child, FileChangeType.CREATED, false));
                            } else if (kind == ENTRY_DELETE) {
                                changes.add(new FileChange(child, FileChangeType.DELETED, keys.containsKey(child)));
                                keys.computeIfPresent(child, (_, _) -> null);
                            } else if (kind == ENTRY_MODIFY) {
                                changes.add(new FileChange(child, FileChangeType.MODIFIED, false));
                            }
                        }
                    }

                    // reset key and remove from set if directory no longer accessible
                    boolean valid = key.reset();
                    if (!valid) {
                        log.warn("Key reset failed: {}", ((Path) key.watchable()).toAbsolutePath());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to scan dir", e);
        }
        log.error("Thread stopped");
    }

    private void drainFiles() {
        var ofSeconds = Duration.ofSeconds(10);
        try {
            //noinspection InfiniteLoopStatement
            for (; ; ) {
                Thread.sleep(ofSeconds);
                if (suspend.get()) {
                    continue;
                }
                if (!changes.isEmpty()) {
                    log.info("Drain files changes detected: nb changes: '{}': ten first: {}",
                             changes.size(),
                             changes.stream().limit(10).toList());
//                    var dirEvent = changes.stream().filter(fc -> Files.isDirectory(fc.path)).toList();
//                    dirEvent.forEach(changes::remove);

                    // Add files when dire is created (sometime when you copy a dir, there is missing event for existing files)
                    changes.stream().filter(fc -> fc.type == FileChangeType.CREATED)
                           .flatMap(fc -> FileUtil.getAllFiles(fc.path).stream())
                           .map(p -> new FileChange(p, FileChangeType.CREATED, false))
                           .forEach(changes::add);

                    var event = FilesChangesDetectedEvent.builder()
                                                         .created(changes.stream()
                                                                         .filter(fc -> fc.type == FileChangeType.CREATED)
                                                                         .filter(fc -> Files.isDirectory(fc.path) ||
                                                                                       EFileType.isSupportedExtension(fc.path))
                                                                         .map(FileChange::toPathItem)
                                                                         .toList())
                                                         .modified(changes.stream()
                                                                          .filter(fc -> fc.type == FileChangeType.MODIFIED)
                                                                          .filter(fc -> EFileType.isSupportedExtension(fc.path))
                                                                          .map(FileChange::toPathItem)
                                                                          .toList())
                                                         .deleted(changes.stream()
                                                                         .filter(fc -> fc.type == FileChangeType.DELETED)
                                                                         .filter(fc -> fc.isDirectory ||
                                                                                       EFileType.isSupportedExtension(fc.path))
                                                                         .map(FileChange::toPathItem)
                                                                         .toList())
                                                         .source(this)
                                                         .build();
//                    // Add files when dir is deleted, we have to notifty UI.
//                    dirEvent.stream().
//                            filter(fc -> fc.type == FileChangeType.DELETED)
//                            .forEach(fc -> event.getDeleted().add(fc.path));

                    if (event.isNotEmpty()) {
                        log.info("Drain files changes detected: valid changes are: '{}' creation, '{}' deletion, '{}' updates",
                                 event.getCreated().size(),
                                 event.getDeleted().size(),
                                 event.getModified().size());
                        taskService.sendEvent(event);
                    } else {
                        log.info("Drain files changes detected: no valid changes");
                    }
                    changes.clear();
                }
            }
        } catch (InterruptedException e) {
            log.warn("Thread interruped");
        }
    }

    public void setSuspend(boolean value) {
        suspend.set(value);
        log.info("Directory Watcher suspended: '{}'", suspend.get());
//        if (!value) {
//            drainerVThread.notify();
//        }
    }

    @EventListener
    void collectionDeleted(CollectionEvent event) {
        if (event.getType() == CollectionEvent.EventType.DELETED) {
            LangUtils.safeStream(event.getSubDirs()).forEach(path -> {
                var watchKey = keys.remove(path);
                if (watchKey != null) {
                    watchKey.cancel();
                }
            });
        }
    }
}