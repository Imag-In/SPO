package org.icroco.picture.event;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.icroco.picture.model.MediaFile;

@Getter
@ToString
public class CarouselEvent extends IiEvent {

    public enum EventType {
        SHOW,
        HIDE,
    }

    private final EventType eventType;
    private final MediaFile mediaFile;

    @Builder
    public CarouselEvent(EventType eventType, MediaFile mediaFile, Object source) {
        super(source);
        this.eventType = eventType;
        this.mediaFile = mediaFile;
    }
}
