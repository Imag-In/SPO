package org.icroco.picture.event;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.icroco.picture.model.MediaFile;

@Getter
@ToString
@SuperBuilder
public class UpdateMedialFileEvent extends IiEvent {
    private final MediaFile mediaFile;
}
