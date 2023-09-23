package org.icroco.picture.event;

import lombok.Getter;
import lombok.ToString;
import org.icroco.picture.model.MediaCollection;

import java.util.Collection;

@Getter
@ToString
public class CollectionsLoadedEvent extends IiEvent {
    private final Collection<MediaCollection> mediaCollections;

    public CollectionsLoadedEvent(Collection<MediaCollection> mediaCollections, Object source) {
        super(source);
        this.mediaCollections = mediaCollections;
    }
}
