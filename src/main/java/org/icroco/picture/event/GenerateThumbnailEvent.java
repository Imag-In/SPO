package org.icroco.picture.event;

import lombok.Getter;
import lombok.ToString;
import org.icroco.picture.model.MediaCollection;

@Getter
@ToString(exclude = "mediaCollection")
public class GenerateThumbnailEvent extends IiEvent {
    private final MediaCollection mediaCollection;

    public GenerateThumbnailEvent(MediaCollection mediaCollection, Object source) {
        super(source);
        this.mediaCollection = mediaCollection;
    }
}
