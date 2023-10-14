package org.icroco.picture.model;

import org.icroco.picture.views.AbstractDataTest;

public class TagTest extends AbstractDataTest<Keyword> {
    public static Keyword DUMMY = new TagTest().buildInstance();

    @Override
    public Keyword buildInstance() {
        return Keyword.builder()
                      .id(24)
                      .name("Landscape")
                      .build();
    }
}