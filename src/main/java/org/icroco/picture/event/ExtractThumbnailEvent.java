package org.icroco.picture.event;

import lombok.Getter;
import org.icroco.picture.model.MediaCollection;

@Getter
public class ExtractThumbnailEvent extends IiEvent {
    private final MediaCollection mediaCollection;

    public ExtractThumbnailEvent(MediaCollection mediaCollection, Object source) {
        super(source);
        this.mediaCollection = mediaCollection;
    }

    @Override
    public String toString() {
        return "ExtractThumbnailEvent, Id: '%s', path: '%s', nbMediaFiles: '%s' ".formatted(mediaCollection.id(),
                                                                                            mediaCollection.path(),
                                                                                            mediaCollection.medias().size());
    }
}
