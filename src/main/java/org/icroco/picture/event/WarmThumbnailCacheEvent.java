package org.icroco.picture.event;

import lombok.Getter;
import lombok.ToString;
import org.icroco.picture.model.MediaCollection;

@Getter
@ToString(exclude = "mediaCollection")
public class WarmThumbnailCacheEvent extends IiEvent {
    private final MediaCollection mediaCollection;

    public WarmThumbnailCacheEvent(MediaCollection mediaCollection, Object source) {
        super(source);
        this.mediaCollection = mediaCollection;
    }
}
