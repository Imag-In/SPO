package org.icroco.picture.ui.event;

import lombok.Getter;
import org.icroco.picture.ui.model.MediaCollection;
import org.springframework.context.ApplicationEvent;

@Getter
public class GenerateThumbnailEvent extends ApplicationEvent {
    private final MediaCollection mediaCollection;

    public GenerateThumbnailEvent(MediaCollection mediaCollection, Object source) {
        super(source);
        this.mediaCollection = mediaCollection;
    }
}
