package org.icroco.picture.event;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class CollectionEvent extends IiEvent {

    public enum EventType {
        READY,
        SELECTED,
        CREATED,
        DELETED
    }

    private final int       mcId;
    private final EventType type;
}
