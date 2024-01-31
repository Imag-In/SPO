package org.icroco.picture.views.util;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.config.SpoConfiguration;
import org.icroco.picture.event.CollectionEvent;
import org.icroco.picture.event.FilesChangesDetectedEvent;
import org.icroco.picture.persistence.CollectionRepository;
import org.icroco.picture.util.Constant;
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


    @Autowired
    public DirectoryWatcher(TaskService taskService,
                            CollectionRepository repository,
                            @Qualifier(SpoConfiguration.DIRECTORY_WATCHER) ExecutorService executorService) throws IOException {
        this(taskService, null, true);
        executorService.submit(this::processEvents);
        drainerVThread = Thread.ofVirtual().name("File-Watcher-drainer")
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
     * Register the given directory, and all its sub-directories, with the
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

    record FileChange(Path path, FileChangeType type) {
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
                            } else if (kind == ENTRY_DELETE) {
                                log.info("Un-watch dir: {}", child);
                            } else if (kind == ENTRY_MODIFY) {
                                log.debug("Dir modified: {}", child);
                            }
                        } else {
                            if (kind == ENTRY_CREATE) {
                                changes.add(new FileChange(child, FileChangeType.CREATED));
                            } else if (kind == ENTRY_DELETE) {
                                changes.add(new FileChange(child, FileChangeType.DELETED));
                            } else if (kind == ENTRY_MODIFY) {
                                changes.add(new FileChange(child, FileChangeType.MODIFIED));
                            }
                        }
                    }

                    // reset key and remove from set if directory no longer accessible
                    boolean valid = key.reset();
                    if (!valid) {
                        log.info("Key reset failed: {}", key.watchable());
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
                if (!changes.isEmpty()) {
                    log.info("Drain files changes detected: nb changes: '{}': ten first: {}",
                             changes.size(),
                             changes.stream().limit(10).toList());
                    var event = FilesChangesDetectedEvent.builder()
                                                         .created(changes.stream()
                                                                         .filter(fc -> fc.type == FileChangeType.CREATED)
                                                                         .filter(fc -> Files.isDirectory(fc.path) ||
                                                                                       Constant.isSupportedExtension(fc.path))
                                                                         .map(FileChange::path)
                                                                         .toList())
                                                         .modified(changes.stream()
                                                                          .filter(fc -> fc.type == FileChangeType.MODIFIED)
                                                                          .filter(fc -> Files.isDirectory(fc.path) ||
                                                                                        Constant.isSupportedExtension(fc.path))
                                                                          .map(FileChange::path)
                                                                          .toList())
                                                         .deleted(
                                                                 changes.stream()
                                                                        .filter(fc -> fc.type == FileChangeType.DELETED)
                                                                        .filter(fc -> Files.isDirectory(fc.path) ||
                                                                                      Constant.isSupportedExtension(fc.path))
                                                                        .map(FileChange::path)
                                                                        .toList())
                                                         .source(this)
                                                         .build();

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