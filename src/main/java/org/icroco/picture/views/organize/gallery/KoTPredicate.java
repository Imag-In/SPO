package org.icroco.picture.views.organize.gallery;

import lombok.AllArgsConstructor;
import org.icroco.picture.model.EKeepOrThrow;
import org.icroco.picture.model.MediaFile;

import java.util.function.Predicate;

@AllArgsConstructor
public class KoTPredicate implements Predicate<MediaFile> {
    private final EKeepOrThrow value;

    @Override
    public boolean test(MediaFile mediaFile) {
        return mediaFile.getKeepOrThrow() == value;
    }
}
