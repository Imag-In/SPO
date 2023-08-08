package org.icroco.picture.ui.event;

import lombok.Getter;
import lombok.ToString;
import org.icroco.picture.ui.model.MediaCollection;

import java.util.Collection;

@Getter
@ToString(exclude = "mediaCollections")
public class CollectionsLoadedEvent extends IiEvent {
    private final Collection<MediaCollection> mediaCollections;

    public CollectionsLoadedEvent(Collection<MediaCollection> mediaCollections, Object source) {
        super(source);
        this.mediaCollections = mediaCollections;
    }
}
