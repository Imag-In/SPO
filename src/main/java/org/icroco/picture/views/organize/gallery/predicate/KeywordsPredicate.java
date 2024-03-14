package org.icroco.picture.views.organize.gallery.predicate;

import lombok.AllArgsConstructor;
import org.icroco.picture.model.Keyword;
import org.icroco.picture.model.MediaFile;

import java.util.List;
import java.util.function.Predicate;

@AllArgsConstructor
public class KeywordsPredicate implements Predicate<MediaFile> {
    private final List<Keyword> keywords;

    @Override
    public boolean test(MediaFile mediaFile) {
        return mediaFile.getKeywords().containsAll(keywords);
    }
}
