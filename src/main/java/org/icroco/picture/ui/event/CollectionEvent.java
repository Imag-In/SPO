package org.icroco.picture.ui.event;

import lombok.Getter;
import org.icroco.picture.ui.model.MediaCollection;
import org.springframework.context.ApplicationEvent;

@Getter
public class CollectionEvent extends ApplicationEvent {

    public enum EventType {
        READY,
        SELECTED,
        CREATED,
        DELETED,
        UPDATED
    }

    private final MediaCollection mediaCollection;
    private final EventType       type;

    public CollectionEvent(MediaCollection files, EventType type, Object source) {
        super(source);
        this.mediaCollection = files;
        this.type = type;
    }
}
