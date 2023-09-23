package org.icroco.picture.event;

import lombok.Getter;
import lombok.ToString;
import org.icroco.picture.model.MediaCollection;

@Getter
@ToString(exclude = "mediaCollection")
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
