package org.icroco.picture.event;

import lombok.Getter;
import lombok.ToString;
import org.icroco.picture.model.MediaFile;

import java.util.Collection;

@Getter
@ToString(exclude = { "newItems", "deletedItems" })
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
