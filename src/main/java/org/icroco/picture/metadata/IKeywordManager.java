package org.icroco.picture.metadata;

import org.icroco.picture.model.Keyword;

import java.util.Collection;
import java.util.Set;

public interface IKeywordManager {
    Keyword findOrCreateTag(String name);

    default Collection<String> getAllAsString() {
        return getAll().stream().map(Keyword::name).toList();
    }

    Collection<Keyword> getAll();

    Set<Keyword> addMissingKw(Collection<Keyword> keywords);
}
