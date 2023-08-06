package org.icroco.picture.ui.event;

import lombok.Getter;
import org.icroco.picture.ui.model.MediaCollection;

@Getter
public class GenerateThumbnailEvent extends IiEvent {
    private final MediaCollection mediaCollection;

    public GenerateThumbnailEvent(MediaCollection mediaCollection, Object source) {
        super(source);
        this.mediaCollection = mediaCollection;
    }
}
