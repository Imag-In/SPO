package org.icroco.picture.ui.model;

import org.icroco.picture.ui.AbstractDataTest;

import java.util.Set;

public class MediaFileTest extends AbstractDataTest<MediaFile> {
    public static MediaFile DUMMY = new MediaFileTest().buildInstance();

    @Override
    public MediaFile buildInstance() {
        return MediaFile.builder()
                .id(42)
                .tags(Set.of(TagTest.DUMMY, Tag.builder().id(1).name("foo").build()))
                .build();
    }
}