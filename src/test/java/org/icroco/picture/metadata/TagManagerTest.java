package org.icroco.picture.metadata;

import org.icroco.picture.model.Keyword;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class TagManagerTest {

    public final static Keyword KEYWORD = new Keyword(0, "fakeTag");

    public static IKeywordManager TAG_MANAGER = new IKeywordManager() {
        @Override
        public Keyword findOrCreateTag(String name) {
            return KEYWORD;
        }

        @Override
        public Collection<Keyword> getAll() {
            return Collections.emptyList();
        }

        @Override
        public Set<Keyword> addMissingKw(Collection<Keyword> keywords) {
            return Set.copyOf(keywords);
        }
    };

}