package org.icroco.picture.ui.model;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.image.Image;
import lombok.Data;

@Data
public class MfCached {
    private Image thumbnail;
    private SimpleBooleanProperty loading = new SimpleBooleanProperty(true);

    public void setThumbnail(Image thumbnail) {
        this.thumbnail = thumbnail;
        loading.set(false);
    }
}
