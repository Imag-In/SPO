package org.icroco.picture.metadata;

import org.icroco.picture.model.Keyword;

public interface IKeywordManager {
    Keyword findOrCreateTag(String name);
}
