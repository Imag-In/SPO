package org.icroco.picture.event;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class GalleryRefreshEvent extends IiEvent {
    private final int mediaCollectionId;

    public GalleryRefreshEvent(int mediaCollectionId, Object source) {
        super(source);
        this.mediaCollectionId = mediaCollectionId;
    }
}
