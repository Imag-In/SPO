package org.icroco.picture.event;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.icroco.picture.model.MediaFile;
import org.springframework.lang.NonNull;

import java.util.Objects;
import java.util.Optional;

@Getter
@SuperBuilder
public class ShowDiffEvent extends IiEvent {
    private final MediaFile left;
    private final MediaFile right;

    @Override
    @NonNull
    public String toString() {
        return STR."ShowDiffEvent{left=\{Optional.ofNullable(left)
                                                 .map(MediaFile::getFullPath)
                                                 .map(Objects::toString)
                                                 .orElse("empty")}, right=\{Optional.ofNullable(right)
                                                                                    .map(MediaFile::getFullPath)
                                                                                    .map(Objects::toString)
                                                                                    .orElse("empty")}\{'}'}";
    }
}
