package org.icroco.picture.ui.model;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Set;

public interface IMediaFile {
    long id();

    Path fullPath();

    String fileName();

    LocalDate originalDate();

    Set<Tag> tags();
}
