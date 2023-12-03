package org.icroco.picture.event;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.icroco.picture.model.MediaFile;

import java.util.Collection;

@Getter
@SuperBuilder
public class CustomExtractThumbnailEvent extends IiEvent {
    @Min(value = 1)
    private final int                   mcId;
    private final Collection<MediaFile> newItems;
    private final Collection<MediaFile> modifiedItems;

    @Override
    public String toString() {
        return "CustomExtractThumbnailEvent, Id: '%d'".formatted(mcId);
    }
}
