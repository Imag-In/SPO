package org.icroco.picture.ui.event;

import lombok.Getter;
import org.icroco.picture.ui.model.MediaCollection;
import org.springframework.context.ApplicationEvent;

@Getter
public class ExtractThumbnailEvent extends ApplicationEvent {
    private final MediaCollection mediaCollection;

    public ExtractThumbnailEvent(MediaCollection mediaCollection, Object source) {
        super(source);
        this.mediaCollection = mediaCollection;
    }
}
