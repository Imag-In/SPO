package org.icroco.picture.model;

import org.icroco.picture.views.AbstractDataTest;

import java.util.Set;

public class MediaFileTest extends AbstractDataTest<MediaFile> {
    public static MediaFile DUMMY = new MediaFileTest().buildInstance();

    @Override
    public MediaFile buildInstance() {
        return MediaFile.builder()
                .id(42)
                .tags(Set.of(TagTest.DUMMY, Tag.builder().id(1).name("foo").build()))
                .geoLocation(GeoLocation.builder().longitude(1).latitude(2).build())
                .build();
    }
}