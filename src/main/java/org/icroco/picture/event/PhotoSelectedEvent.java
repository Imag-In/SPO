package org.icroco.picture.event;

import lombok.Getter;
import org.icroco.picture.model.MediaFile;

import java.nio.file.Path;
import java.util.Optional;

@Getter
public class PhotoSelectedEvent extends IiEvent {
    private final MediaFile      mf;
    private final ESelectionType type;

    public enum ESelectionType {
        SELECTED,
        UNSELECTED
    }

    public PhotoSelectedEvent(ESelectionType type, MediaFile file, Object source) {
        super(source);
        this.mf = file;
        this.type = type;
    }

    @Override
    public String toString() {
        var f = Optional.ofNullable(mf);
        return "PhotoSelectedEvent, Type: '%s', Id: '%s', file: '%s' ".formatted(type,
                                                                                 f.map(MediaFile::getId).orElse(-1L),
                                                                                 f.map(MediaFile::getFullPath).orElse(Path.of("")));
    }
}
