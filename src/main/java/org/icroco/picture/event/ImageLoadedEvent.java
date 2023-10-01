package org.icroco.picture.event;

import javafx.scene.image.Image;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.icroco.picture.model.MediaFile;

@Getter
@ToString
@SuperBuilder
public class ImageLoadedEvent extends IiEvent {
    private final MediaFile mediaFile;
    private final Image     image;
}
