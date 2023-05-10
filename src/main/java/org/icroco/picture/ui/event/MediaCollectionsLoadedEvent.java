package org.icroco.picture.ui.event;

import lombok.Getter;
import org.icroco.picture.ui.model.Catalog;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class MediaCollectionsLoadedEvent extends ApplicationEvent {
    private final List<Catalog> catalogs;

    public MediaCollectionsLoadedEvent(List<Catalog> catalogs, Object source) {
        super(source);
        this.catalogs = catalogs;
    }
}
