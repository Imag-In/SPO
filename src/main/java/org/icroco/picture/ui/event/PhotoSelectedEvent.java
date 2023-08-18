package org.icroco.picture.ui.event;

import lombok.Getter;
import org.icroco.picture.ui.model.MediaFile;

@Getter
public class PhotoSelectedEvent extends IiEvent {
    private final MediaFile mf;

    public PhotoSelectedEvent(MediaFile file, Object source) {
        super(source);
        this.mf = file;
    }

    @Override
    public String toString() {
        return "PhotoSelectedEvent, Id: '%s', file: '%s' ".formatted(mf.getId(), mf.getFullPath());
    }
}
