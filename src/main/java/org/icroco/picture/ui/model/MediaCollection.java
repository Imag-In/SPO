package org.icroco.picture.ui.model;

import lombok.Builder;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

@Builder
public record MediaCollection(int id, Path path, Set<MediaFile> medias, Set<MediaCollectionEntry> subPaths) {
    public MediaCollection {
        path = path.normalize();
    }

    public void replaceMedias(Collection<MediaFile> mf) {
        medias.removeAll(mf);
        medias.addAll(mf);
    }

    public void replaceSubPaths(Collection<MediaCollectionEntry> sb) {
        subPaths.removeAll(sb);
        subPaths.addAll(sb);
    }

    @Override
    public String toString() {
        return "MediaCollection{" +
               "id:" + id +
               ", path:" + path +
               '}';
    }
}
