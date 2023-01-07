package org.icroco.picture.ui.model;

import lombok.Builder;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Set;

@Builder
public record MediaFile(long id, Path fullPath, String fileName, LocalDate originalDate, Set<Tag> tags) implements IMediaFile {
}
