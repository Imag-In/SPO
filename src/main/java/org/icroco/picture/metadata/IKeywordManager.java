package org.icroco.picture.metadata;

import org.icroco.picture.model.Keyword;

import java.util.Collection;

public interface IKeywordManager {
    Keyword findOrCreateTag(String name);

    Collection<String> getAll();
}
