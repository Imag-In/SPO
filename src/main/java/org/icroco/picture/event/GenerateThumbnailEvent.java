package org.icroco.picture.event;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.icroco.picture.model.MediaCollection;

@Getter
@ToString(exclude = "mediaCollection")
@SuperBuilder
public class GenerateThumbnailEvent extends IiEvent {
    private final MediaCollection mediaCollection;
}
