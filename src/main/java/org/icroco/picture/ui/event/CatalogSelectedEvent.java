package org.icroco.picture.ui.event;

import lombok.Getter;
import org.icroco.picture.ui.model.Catalog;
import org.springframework.context.ApplicationEvent;

import java.time.Clock;

@Getter
public class CatalogSelectedEvent extends ApplicationEvent {
    private final Catalog catalog;
    public CatalogSelectedEvent(Catalog files, Object source) {
        super(source);
        this.catalog = files;
    }

    public CatalogSelectedEvent(Catalog files, Object source, Clock clock) {
        super(source, clock);
        this.catalog = files;
    }
}
