package org.icroco.picture.ui.model;

import javafx.scene.image.Image;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class ThumbnailImage {
//    private static Image NOT_LOADED_IMAGE = new Image();

    Image   image;
    boolean loaded;
}
