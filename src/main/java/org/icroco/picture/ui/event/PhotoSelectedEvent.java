package org.icroco.picture.ui.event;

import lombok.Getter;
import org.icroco.picture.ui.model.MediaFile;

@Getter
public class PhotoSelectedEvent extends IiEvent {
    private final MediaFile file;

    public PhotoSelectedEvent(MediaFile file, Object source) {
        super(source);
        this.file = file;
    }
}
