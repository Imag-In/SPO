package org.icroco.picture.ui.event;

import javafx.scene.control.TitledPane;
import lombok.Getter;
import org.icroco.picture.ui.model.Catalog;
import org.springframework.context.ApplicationEvent;

@Getter
public class GenerateThumbnailEvent extends ApplicationEvent {
    private final Catalog catalog;

    public GenerateThumbnailEvent(Catalog catalog, Object source) {
        super(source);
        this.catalog = catalog;
    }
}
