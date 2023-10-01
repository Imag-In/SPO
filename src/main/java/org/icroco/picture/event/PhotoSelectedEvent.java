package org.icroco.picture.event;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.icroco.picture.model.MediaFile;

import java.nio.file.Path;
import java.util.Optional;

@Getter
@SuperBuilder
public class PhotoSelectedEvent extends IiEvent {
    private final MediaFile      mf;
    private final ESelectionType type;

    public enum ESelectionType {
        SELECTED,
        UNSELECTED
    }

    @Override
    public String toString() {
        var f = Optional.ofNullable(mf);
        return "PhotoSelectedEvent, Type: '%s', Id: '%s', file: '%s' ".formatted(type,
                                                                                 f.map(MediaFile::getId).orElse(-1L),
                                                                                 f.map(MediaFile::getFullPath).orElse(Path.of("")));
    }
}
