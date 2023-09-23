package org.icroco.picture.model;

import org.icroco.picture.views.AbstractDataTest;

public class TagTest extends AbstractDataTest<Tag> {
    public static Tag DUMMY = new TagTest().buildInstance();

    @Override
    public Tag buildInstance() {
        return Tag.builder()
                .id(24)
                .name("Landscape")
                .build();
    }
}