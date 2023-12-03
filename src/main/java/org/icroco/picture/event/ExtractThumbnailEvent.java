package org.icroco.picture.event;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class ExtractThumbnailEvent extends IiEvent {
    private final int mcId;
    private final boolean update;


    @Override
    public String toString() {
        return "ExtractThumbnailEvent, Id: '%d'".formatted(mcId);
    }
}
