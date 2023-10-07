package org.icroco.picture.views.organize;

import lombok.Builder;
import org.springframework.lang.Nullable;

import java.nio.file.Path;
import java.util.Objects;

@Builder
public record PathSelection(int mediaCollectionId, Path subPath, double seed) {
    public PathSelection(int mediaCollectionId, Path subPath) {
        this(mediaCollectionId, subPath, Math.random());
    }

    public boolean equalsNoSeed(@Nullable PathSelection pathSelection) {
        return pathSelection != null
               && Objects.equals(mediaCollectionId, pathSelection.mediaCollectionId)
               && Objects.equals(subPath, pathSelection.subPath);
    }
}
