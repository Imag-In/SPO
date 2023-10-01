package org.icroco.picture.event;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.icroco.picture.model.MediaCollection;

import java.util.Collection;

@Getter
@ToString
@SuperBuilder
public class CollectionsLoadedEvent extends IiEvent {
    private final Collection<MediaCollection> mediaCollections;
}
