package org.icroco.picture.event;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class GenerateThumbnailEvent extends IiEvent {
    private final int mcId;
}
