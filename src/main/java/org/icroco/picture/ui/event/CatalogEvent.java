package org.icroco.picture.ui.event;

import lombok.Getter;
import org.icroco.picture.ui.model.Catalog;
import org.springframework.context.ApplicationEvent;

@Getter
public class CatalogEvent extends ApplicationEvent {

    public enum EventType {
        READY,
        SELECTED,
        CREATED,
        DELETED,
        UPDATED
    }

    private final Catalog catalog;
    private final EventType type;

    public CatalogEvent(Catalog files, EventType type, Object source) {
        super(source);
        this.catalog = files;
        this.type = type;
    }
}
