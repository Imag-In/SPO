package org.icroco.picture.event;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.icroco.picture.model.MediaFile;

@Getter
@SuperBuilder
public class ForceGenerateThumbnailEvent extends IiEvent {
    private final MediaFile mediaFile;
}
