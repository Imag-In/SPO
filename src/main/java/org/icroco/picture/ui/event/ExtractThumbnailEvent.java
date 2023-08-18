package org.icroco.picture.ui.event;

import lombok.Getter;
import org.icroco.picture.ui.model.MediaCollection;

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
