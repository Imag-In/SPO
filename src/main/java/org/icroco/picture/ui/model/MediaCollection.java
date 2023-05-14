package org.icroco.picture.ui.model;

import lombok.Builder;

import java.nio.file.Path;
import java.util.Set;

@Builder
public record MediaCollection(int id, Path path, Set<MediaFile> medias, Set<MediaCollectionEntry> subPaths) {
    public MediaCollection {
        path = path.normalize();
    }
}
