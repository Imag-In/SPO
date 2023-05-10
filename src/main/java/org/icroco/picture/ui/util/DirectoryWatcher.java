package org.icroco.picture.ui.util;

import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.ui.config.ImageInConfiguration;
import org.icroco.picture.ui.persistence.CollectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
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

    @Autowired
    public DirectoryWatcher(CollectionRepository repository,
                            @Qualifier(ImageInConfiguration.DIRECTORY_WATCHER) ExecutorService executorService) throws IOException {
        this(null, true);
        repository.findAll()
                  .forEach(c -> registerAll(c.getPath()));
        executorService.submit(this::processEvents);
    }

    /**
     * Creates a WatchService and registers the given directory
     */
    DirectoryWatcher(@Nullable Path dir, boolean recursive) throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new ConcurrentHashMap<>();
        this.recursive = recursive;

        if (dir != null) {
            if (recursive) {
                log.info("Scanning '{}'", dir);
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
        log.info("Watch: '{}'", dir);
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


    /**
     * Process all events for keys queued to the watcher
     */
    void processEvents() {
        for (; ; ) {
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
                            registerAll(child);
                        } else if (kind == ENTRY_DELETE) {
                            keys.remove(key);
                        }
                    } else {
                        if (kind == ENTRY_CREATE) {
                            log.info("File created: {}", child);
                        } else if (kind == ENTRY_DELETE) {
                            log.info("File deleted: {}", child);
                        } else if (kind == ENTRY_MODIFY) {
                            log.info("File modified: {}", child);
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
}