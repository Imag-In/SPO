package org.icroco.picture.ui.model;

import lombok.Builder;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Set;

@Builder
public record MediaFile(long id, Path fullPath, String fileName, LocalDate originalDate, Set<Tag> tags, ThumbnailImage thumbnail) implements IMediaFile {
    public MediaFile(long id, Path fullPath, String fileName, LocalDate originalDate, Set<Tag> tags, ThumbnailImage thumbnail) {
        this.id = id;
        this.fullPath = fullPath;
        this.fileName = fileName;
        this.originalDate = originalDate;
        this.tags = tags;
        this.thumbnail = thumbnail == null ? new ThumbnailImage(null, false) : thumbnail;
    }
}
