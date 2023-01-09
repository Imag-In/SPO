package org.icroco.picture.ui.model;

import javafx.beans.Observable;
import javafx.scene.image.Image;
import javafx.util.Callback;
import lombok.Builder;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Set;

@Builder
public record MediaFile(long id, Path fullPath, String fileName, LocalDate originalDate, Set<Tag> tags, MfCached cachedInfo) implements IMediaFile {
    public MediaFile(long id, Path fullPath, String fileName, LocalDate originalDate, Set<Tag> tags) {
        this(id, fullPath, fileName, originalDate, tags, new MfCached());
    }

    public boolean isLoading() {
        return cachedInfo.getLoading().get();
    }

    public Image getThumbnail() {
        return cachedInfo.getThumbnail();
    }

    @Override
    public int hashCode() {
        return fullPath.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MediaFile mediaFile = (MediaFile) o;
        return id == mediaFile.id;
    }

    public static Callback<MediaFile, Observable[]> extractor() {
        return p -> new Observable[]{ p.cachedInfo().getLoading() };
    }
}
