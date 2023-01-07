package org.icroco.picture.ui.model;

import org.icroco.picture.ui.AbstractDataTest;

import static org.junit.jupiter.api.Assertions.*;

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