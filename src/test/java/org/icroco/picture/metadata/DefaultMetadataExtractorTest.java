package org.icroco.picture.metadata;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.icroco.picture.views.util.Collections;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;

@Slf4j
class DefaultMetadataExtractorTest {

    IMetadataExtractor extractor = new DefaultMetadataExtractor(TagManagerTest.TAG_MANAGER);

    @Test
    @Disabled
    void should_read_orientation() {
        log.info("orientation: {}", extractor.orientation(Paths.get("/Users/christophe/Pictures/foo/bar/RP Felie.png")));
        log.info("orientation: {}", extractor.orientation(Paths.get("/Users/christophe/Pictures/foo/test/Espagne-25072017-138.jpg")));
    }

    @Test
    @Disabled
    void should_read_header() {
        log.info("header: {}", extractor.header(Paths.get("/Users/christophe/Pictures/foo/bar/RP Felie.png")));
        log.info("header: {}", extractor.header(Paths.get("/Users/christophe/Pictures/foo/test/Espagne-25072017-138.jpg")));
    }

    @Test
    void should_throw_exception_when_image_unreadable() throws ImageProcessingException, IOException {
        //Users/christophe/Pictures/foo/test/Espagne-25072017-138.jpg
        //src/test/resources/images/IMG_20180527_160832.jpg
//        Metadata
//                metadata =
//                ImageMetadataReader.readMetadata(Paths.get("/Users/christophe/Pictures/foo/json/IMGP8950.JPG").toFile());

        Assertions.assertThatThrownBy(() -> {
            Metadata
                    metadata =
                    ImageMetadataReader.readMetadata(Paths.get("src/test/resources/images/IMG_20180527_160832.jpg").toFile());

            Collections.toStream(metadata.getDirectories())
                       .forEach(d -> {
                           log.info("Directory: {}", d.getClass());
                           d.getTags()
                            .forEach(t -> {
                                log.info("   tags: {}({}) = {}", t.getTagName(), t.getTagType(), t.getDescription());
                            });
                       });
        }).isInstanceOf(ImageProcessingException.class);
    }


    @Test
    @Disabled
    void printAllByDirectory() throws ImageProcessingException, IOException {
        //Users/christophe/Pictures/foo/test/Espagne-25072017-138.jpg
        //src/test/resources/images/IMG_20180527_160832.jpg
//        Metadata
//                metadata =
//                ImageMetadataReader.readMetadata(Paths.get("/Users/christophe/Pictures/foo/json/IMGP8950.JPG").toFile());

        Assertions.assertThatThrownBy(() -> {
            Metadata
                    metadata =
                    ImageMetadataReader.readMetadata(Paths.get("/Users/christophe/Pictures/foo/json/Imag'In-Icon_Only-128x128-FF.png")
                                                          .toFile());

            Collections.toStream(metadata.getDirectories())
                       .forEach(d -> {
                           log.info("Directory: {}", d.getClass());
                           d.getTags()
                            .forEach(t -> {
                                log.info("   tags: {}({}) = {}", t.getTagName(), t.getTagType(), t.getDescription());
                            });
                       });
        }).isInstanceOf(ImageProcessingException.class);
    }

}