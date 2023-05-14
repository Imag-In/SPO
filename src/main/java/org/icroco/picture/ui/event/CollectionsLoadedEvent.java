package org.icroco.picture.ui.event;

import lombok.Getter;
import org.icroco.picture.ui.model.MediaCollection;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class CollectionsLoadedEvent extends ApplicationEvent {
    private final List<MediaCollection> mediaCollections;

    public CollectionsLoadedEvent(List<MediaCollection> mediaCollections, Object source) {
        super(source);
        this.mediaCollections = mediaCollections;
    }
}
