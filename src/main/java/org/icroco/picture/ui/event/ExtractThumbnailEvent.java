package org.icroco.picture.ui.event;

import lombok.Getter;
import org.icroco.picture.ui.model.Catalog;
import org.springframework.context.ApplicationEvent;

@Getter
public class ExtractThumbnailEvent extends ApplicationEvent {
    private final Catalog catalog;

    public ExtractThumbnailEvent(Catalog catalog, Object source) {
        super(source);
        this.catalog = catalog;
    }
}
