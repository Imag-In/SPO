package org.icroco.picture.ui.event;

import lombok.Getter;
import org.icroco.picture.ui.model.MediaCollection;

@Getter
public class WarmThumbnailCacheEvent extends IiEvent {
    private final MediaCollection mediaCollection;

    public WarmThumbnailCacheEvent(MediaCollection mediaCollection, Object source) {
        super(source);
        this.mediaCollection = mediaCollection;
    }
}
