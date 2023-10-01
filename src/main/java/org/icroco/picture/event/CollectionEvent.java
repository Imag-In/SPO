package org.icroco.picture.event;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.icroco.picture.model.MediaCollection;

@Getter
@ToString(exclude = "mediaCollection")
@SuperBuilder
public class CollectionEvent extends IiEvent {

    public enum EventType {
        READY,
        SELECTED,
        CREATED,
        DELETED
    }

    private final MediaCollection mediaCollection;
    private final EventType       type;
}
