package org.icroco.picture.model;

import org.assertj.core.api.SoftAssertions;
import org.icroco.picture.views.AbstractDataTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public class MediaFileTest extends AbstractDataTest<MediaFile> {
    public static MediaFile DUMMY = new MediaFileTest().buildInstance();

    @Override
    public MediaFile buildInstance() {
        return MediaFile.builder()
                        .id(42L)
                        .keywords(Set.of(TagTest.DUMMY, Keyword.builder().id(1).name("foo").build()))
                        .geoLocation(GeoLocation.builder().longitude(1).latitude(2).build())
                        .build();
    }

    @Test
    void should_copy_all_attributes() {
        MediaFile mf1 = createMedialFile(1L,
                                         Path.of("/foo/bar.txt"),
                                         "bar.txt",
                                         LocalDateTime.of(2023, 11, 25, 18, 55),
                                         Set.of(Keyword.builder().id(1).name("foo").build()),
                                         GeoLocation.builder().latitude(10).longitude(20).build(),
                                         "H1",
                                         LocalDate.of(2023, 12, 25),
                                         Dimension.builder().width(50).height(50).build(),
                                         (short) 1,
                                         Camera.builder().make("SONY").model("RX100").build(),
                                         1,
                                         false,
                                         EKeepOrThrow.KEEP,
                                         EThumbnailType.EXTRACTED);
        MediaFile mf2 = createMedialFile(2L,
                                         Path.of("/bar/foo.txt"),
                                         "foo.txt",
                                         LocalDateTime.of(2023, 12, 25, 18, 55),
                                         Set.of(Keyword.builder().id(2).name("bar").build()),
                                         GeoLocation.builder().latitude(20).longitude(40).build(),
                                         "H2",
                                         LocalDate.of(2023, 11, 25),
                                         Dimension.builder().width(100).height(100).build(),
                                         (short) 2,
                                         Camera.builder().make("CANON").model("EXOS").build(),
                                         2,
                                         true,
                                         EKeepOrThrow.THROW,
                                         EThumbnailType.GENERATED);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(mf1).isNotEqualTo(mf2);
        soft.assertThat(mf1).usingRecursiveAssertion().isNotEqualTo(mf2);

        mf1.initFrom(mf2);
        soft.assertThat(mf1).isEqualTo(mf2);
        soft.assertThat(mf1).usingRecursiveAssertion().isEqualTo(mf2);
        soft.assertAll();
    }

    static MediaFile createMedialFile(Long id,
                                      Path fullPath,
                                      String fileName,
                                      LocalDateTime originalDate,
                                      Set<Keyword> keywords,
                                      GeoLocation geoLocation,
                                      String hash,
                                      LocalDate hashDate,
                                      Dimension dimension,
                                      short orientation,
                                      Camera camera,
                                      int collectionId,
                                      boolean selected,
                                      EKeepOrThrow keepOrThrow,
                                      EThumbnailType thumbnailType) {
        return MediaFile.builder()
                        .id(id)
                        .fullPath(fullPath)
                        .fileName(fileName)
                        .originalDate(originalDate)
                        .keywords(keywords)
                        .geoLocation(geoLocation)
                        .hash(hash)
                        .hashDate(hashDate)
                        .dimension(dimension)
                        .orientation(orientation)
                        .camera(camera)
                        .collectionId(collectionId)
                        .selected(selected)
                        .keepOrThrow(keepOrThrow)
                        .thumbnailType(thumbnailType)
                        .rating(ERating.FIVE)
                        .reference(UUID.randomUUID())
                        .build();
    }
}