package org.icroco.picture.event;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.icroco.picture.model.MediaFile;

@Getter
@ToString
@SuperBuilder
public class CarouselEvent extends IiEvent {

    public enum EventType {
        SHOW,
        HIDE,
    }

    private final EventType eventType;
    private final MediaFile mediaFile;
}
