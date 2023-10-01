package org.icroco.picture.event;

import javafx.beans.property.ReadOnlyDoubleProperty;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.icroco.picture.model.MediaFile;

@Getter
@ToString
@SuperBuilder
public class ImageLoadingdEvent extends IiEvent {
    private final MediaFile              mediaFile;
    private final ReadOnlyDoubleProperty progress;
}
