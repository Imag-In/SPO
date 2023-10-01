package org.icroco.picture.event;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.icroco.picture.model.MediaCollection;

@Getter
@SuperBuilder
public class ExtractThumbnailEvent extends IiEvent {
    private final MediaCollection mediaCollection;

    @Override
    public String toString() {
        return "ExtractThumbnailEvent, Id: '%s', path: '%s', nbMediaFiles: '%s' ".formatted(mediaCollection.id(),
                                                                                            mediaCollection.path(),
                                                                                            mediaCollection.medias().size());
    }
}
