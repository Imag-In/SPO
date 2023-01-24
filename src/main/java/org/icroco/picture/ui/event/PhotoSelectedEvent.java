package org.icroco.picture.ui.event;

import lombok.Getter;
import org.icroco.picture.ui.model.MediaFile;
import org.springframework.context.ApplicationEvent;

@Getter
public class PhotoSelectedEvent extends ApplicationEvent {
    private final MediaFile file;

    public PhotoSelectedEvent(MediaFile file, Object source) {
        super(source);
        this.file = file;
    }
}
