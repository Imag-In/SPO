package org.icroco.picture.ui.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.nio.file.Path;
import java.time.Clock;

@Getter
public class CollectionSubPathSelectedEvent extends ApplicationEvent {
    private final Path root;
    private final Path entry;

    public CollectionSubPathSelectedEvent(Path root, Path entry, Object source) {
        super(source);
        this.root = root;
        this.entry = entry;
    }

    public CollectionSubPathSelectedEvent(Path root, Path entry, Object source, Clock clock) {
        super(source, clock);
        this.root = root;
        this.entry = entry;
    }
}
