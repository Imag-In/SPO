package org.icroco.picture.event;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@ToString
@SuperBuilder
public class ShowViewEvent extends IiEvent {

    public enum EventType {
        SHOW,
        HIDE,
    }

    private final EventType eventType;
    private final String viewId;
}
