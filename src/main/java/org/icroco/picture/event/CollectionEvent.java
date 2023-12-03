package org.icroco.picture.event;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.nio.file.Path;
import java.util.Collection;

@Getter
@SuperBuilder
@ToString
public class CollectionEvent extends IiEvent {

    public enum EventType {
        READY,
        SELECTED,
        CREATED,
        DELETED
    }

    private final int              mcId;
    private final EventType        type;
    private final Collection<Path> subDirs;
}
