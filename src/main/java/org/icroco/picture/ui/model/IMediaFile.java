package org.icroco.picture.ui.model;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Set;

public interface IMediaFile {
    long id();

    Path fullPath();

    String fileName();

    LocalDateTime originalDate();

    Set<Tag> tags();
}
