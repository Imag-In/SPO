package org.icroco.picture.ui.event;

import lombok.Getter;
import lombok.ToString;
import org.icroco.picture.ui.model.MediaCollection;

@Getter
@ToString
public class ExtractThumbnailEvent extends IiEvent {
    private final MediaCollection mediaCollection;

    public ExtractThumbnailEvent(MediaCollection mediaCollection, Object source) {
        super(source);
        this.mediaCollection = mediaCollection;
    }
}
