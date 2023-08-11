package org.icroco.picture.ui.util;

import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.ui.config.ImageInConfiguration;
import org.icroco.picture.ui.event.FilesChangesDetectedEvent;
import org.icroco.picture.ui.persistence.CollectionRepository;
import org.icroco.picture.ui.task.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.*;

@Slf4j
@Component
public class DirectoryWatcher {
    private final WatchService        watcher;
    private final Map<WatchKey, Path> keys;
    private final boolean             recursive;
    private final TaskService         taskService;

    @Autowired
    public DirectoryWatcher(TaskService taskService,
                            CollectionRepository repository,
                            @Qualifier(ImageInConfiguration.DIRECTORY_WATCHER) ExecutorService executorService) throws IOException {
        this(taskService, null, true);
//        repository.findAll()
//                  .forEach(c -> registerAll(c.getPath()));
        executorService.submit(this::processEvents);
    }

    /**
     * Creates a WatchService and registers the given directory
     */
    DirectoryWatcher(TaskService taskService, @Nullable Path dir, boolean recursive) throws IOException {
        this.taskService = taskService;
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new ConcurrentHashMap<>();
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
    private void register(Path dir) throws IOException {
        WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        keys.put(key, dir);
        log.debug("Watch: '{}'", dir);
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
        }
        catch (IOException e) {
            log.error("Cannot walk through '{}'", start, e);
        }
    }

    enum FileChangeType {CREATED, DELETED, MODIFIED}

    record FileChange(Path path, FileChangeType type) {}

    /**
     * Process all events for keys queued to the watcher
     */
    void processEvents() {
        Set<FileChange> changes   = new HashSet<>();
        long            lastDrain = System.currentTimeMillis();

        try {
            for (; ; ) {
                if (!changes.isEmpty() && System.currentTimeMillis() > lastDrain + 5_000) {
                    log.info("Drain files changes detected, nb changes: '{}': ten first: {}", changes.size(), changes.stream().limit(10).toList());
                    FilesChangesDetectedEvent event = new FilesChangesDetectedEvent(changes.stream()
                                                                                           .filter(fc -> fc.type == FileChangeType.CREATED)
                                                                                           .filter(fc -> Files.isDirectory(fc.path) ||
                                                                                                         Constant.isSupportedExtension(fc.path))
                                                                                           .map(FileChange::path)
                                                                                           .toList(),
                                                                                    changes.stream()
                                                                                           .filter(fc -> fc.type == FileChangeType.DELETED)
                                                                                           .filter(fc -> Files.isDirectory(fc.path) ||
                                                                                                         Constant.isSupportedExtension(fc.path))
                                                                                           .map(FileChange::path)
                                                                                           .toList(),
                                                                                    changes.stream()
                                                                                           .filter(fc -> fc.type == FileChangeType.MODIFIED)
                                                                                           .filter(fc -> Files.isDirectory(fc.path) ||
                                                                                                         Constant.isSupportedExtension(fc.path))
                                                                                           .map(FileChange::path)
                                                                                           .toList(),
                                                                                    this);
                    if (event.isNotEmpty()) {
                        log.info("Drain files changes detected, valid changes are: '{}' creation, '{}' deletion, '{}' updates",
                                 event.getCreated().size(),
                                 event.getDeleted().size(),
                                 event.getModified().size());
                        taskService.sendEvent(event);
                    }
                    changes.clear();
                    lastDrain = System.currentTimeMillis();
                }
                // wait for key to be signalled
                WatchKey key;
                try {
                    key = watcher.poll(5, TimeUnit.SECONDS);
                }
                catch (InterruptedException x) {
                    return;
                }

                if (key != null) {
                    Path dir = keys.get(key);
                    if (dir == null) {
                        log.error("WatchKey not recognized!!");
                        continue;
                    }

                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();

                        // TBD - provide example of how OVERFLOW event is handled
                        if (kind == OVERFLOW) {
                            continue;
                        }

                        // Context for directory entry event is the file name of entry
                        WatchEvent<Path> ev    = cast(event);
                        Path             name  = ev.context();
                        Path             child = dir.resolve(name);

                        // print out event
                        log.debug("{}: {}", event.kind().name(), child);

                        // if directory is created, and watching recursively, then
                        // register it and its subdirectories
                        if (recursive && Files.isDirectory(child, NOFOLLOW_LINKS)) {
                            if (kind == ENTRY_CREATE) {
                                log.info("Watch dir: {}", child);
                                registerAll(child);
                            } else if (kind == ENTRY_DELETE) {
                                log.info("Un-watch dir: {}", child);
                                keys.remove(key);
                            } else if (kind == ENTRY_MODIFY) {
                                log.info("Modify dir: {}", child);
                            }
                        } else {
                            if (kind == ENTRY_CREATE) {
                                changes.add(new FileChange(child, FileChangeType.CREATED));
                            } else if (kind == ENTRY_DELETE) {
                                changes.add(new FileChange(child, FileChangeType.DELETED));
                                keys.remove(key); // because Files.isDirectory(child) return false on a deleted dir.
                            } else if (kind == ENTRY_MODIFY) {
                                changes.add(new FileChange(child, FileChangeType.MODIFIED));
                            }
                        }
                    }

                    // reset key and remove from set if directory no longer accessible
                    boolean valid = key.reset();
                    if (!valid) {
                        keys.remove(key);
//                // all directories are inaccessible
//                if (keys.isEmpty()) {
//                    break;
//                }
                    }
                }
            }
        }
        catch (Exception e) {
            log.warn("Fail to scan dir", e);
        }
    }
}