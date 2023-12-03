package org.icroco.picture.event;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.icroco.picture.model.MediaFile;

import java.util.Collection;

@Getter
@ToString(exclude = { "newItems", "deletedItems" })
@SuperBuilder
public class CollectionUpdatedEvent extends IiEvent {
    private final int mcId;
    private final Collection<MediaFile> newItems;
    private final Collection<MediaFile> deletedItems;
    private final Collection<MediaFile> modifiedItems;

    public boolean isEmpty() {
        return newItems.isEmpty() && deletedItems.isEmpty() && modifiedItems.isEmpty();
    }
}
