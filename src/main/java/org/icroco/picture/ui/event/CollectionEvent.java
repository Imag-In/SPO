package org.icroco.picture.ui.event;

import lombok.Getter;
import org.icroco.picture.ui.model.MediaCollection;

@Getter
public class CollectionEvent extends IiEvent {

    public enum EventType {
        READY,
        SELECTED,
        CREATED,
        DELETED
    }

    private final MediaCollection mediaCollection;
    private final EventType       type;

    public CollectionEvent(MediaCollection mediaCollection, EventType type, Object source) {
        super(source);
        this.mediaCollection = mediaCollection;
        this.type = type;
    }
}
