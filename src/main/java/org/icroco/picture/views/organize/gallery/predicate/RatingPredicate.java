package org.icroco.picture.views.organize.gallery.predicate;

import lombok.AllArgsConstructor;
import org.icroco.picture.model.ERating;
import org.icroco.picture.model.MediaFile;

import java.util.function.Predicate;

@AllArgsConstructor
public class RatingPredicate implements Predicate<MediaFile> {
    private final ERating value;

    @Override
    public boolean test(MediaFile mediaFile) {
        return mediaFile.getRating().getCode() >= value.getCode();
    }
}
