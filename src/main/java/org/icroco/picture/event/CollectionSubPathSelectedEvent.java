package org.icroco.picture.event;

import lombok.Getter;
import lombok.ToString;

import java.nio.file.Path;
import java.time.Clock;

@Getter
@ToString
public class CollectionSubPathSelectedEvent extends IiEvent {
    private final int collectionId;
    private final Path entry;

    public CollectionSubPathSelectedEvent(int collectionId, Path entry, Object source) {
        super(source);
        this.collectionId = collectionId;
        this.entry = entry;
    }

    public CollectionSubPathSelectedEvent(int collectionId, Path entry, Object source, Clock clock) {
        super(source, clock);
        this.collectionId = collectionId;
        this.entry = entry;
    }
}
