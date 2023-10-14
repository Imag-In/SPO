package org.icroco.picture.metadata;

import org.icroco.picture.model.Keyword;

public class TagManagerTest {

    public final static Keyword KEYWORD = new Keyword(0, "fakeTag");

    public static IKeywordManager TAG_MANAGER = new IKeywordManager() {
        @Override
        public Keyword findOrCreateTag(String name) {
            return KEYWORD;
        }
    };

}