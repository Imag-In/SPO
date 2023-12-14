package org.icroco.picture.views.organize.gallery;

import lombok.AllArgsConstructor;
import org.icroco.picture.model.MediaFile;

import java.nio.file.Path;
import java.util.function.Predicate;

@AllArgsConstructor
public class PathPredicate implements Predicate<MediaFile> {
    private final Path prefix;

    @Override
    public boolean test(MediaFile mediaFile) {
        return mediaFile.fullPath().startsWith(prefix);
    }
}
