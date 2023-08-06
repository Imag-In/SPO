package org.icroco.picture.ui.event;

import lombok.Getter;
import org.icroco.picture.ui.model.MediaFile;

import java.util.Collection;

@Getter
public class CollectionUpdatedEvent extends IiEvent {
    private final int                   mediaCollectionId;
    private final Collection<MediaFile> newItems;
    private final Collection<MediaFile> deletedItems;

    public CollectionUpdatedEvent(int mediaCollectionId, Collection<MediaFile> newItems, Collection<MediaFile> deletedItems, Object source) {
        super(source);
        this.mediaCollectionId = mediaCollectionId;
        this.newItems = newItems;
        this.deletedItems = deletedItems;
    }
}
