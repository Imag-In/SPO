package org.icroco.picture.ui.event;

import lombok.Builder;
import lombok.Getter;
import org.icroco.picture.ui.model.MediaFile;
import org.springframework.context.ApplicationEvent;

@Getter
public class CarouselEvent extends ApplicationEvent {

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
