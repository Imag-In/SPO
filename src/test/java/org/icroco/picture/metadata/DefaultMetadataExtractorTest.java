package org.icroco.picture.metadata;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.views.util.Collections;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;

@Slf4j
class DefaultMetadataExtractorTest {

    IMetadataExtractor extractor = new DefaultMetadataExtractor();

    @Test
//    @Disabled
    void should_read_orientation() {
        log.info("orientation: {}", extractor.orientation(Paths.get("/Users/christophe/Pictures/foo/bar/RP Felie.png")));
        log.info("orientation: {}", extractor.orientation(Paths.get("/Users/christophe/Pictures/foo/test/Espagne-25072017-138.jpg")));
    }

    @Test
    void should_read_header() {
        log.info("header: {}", extractor.header(Paths.get("/Users/christophe/Pictures/foo/bar/RP Felie.png")));
        log.info("header: {}", extractor.header(Paths.get("/Users/christophe/Pictures/foo/test/Espagne-25072017-138.jpg")));
    }

    @Test
    void printAllByDirectory() throws ImageProcessingException, IOException {
        Metadata
                metadata =
                ImageMetadataReader.readMetadata(Paths.get("/Users/christophe/Pictures/foo/test/Espagne-25072017-138.jpg").toFile());

        Collections.toStream(metadata.getDirectories())
                   .forEach(d -> {
                       log.info("Directory: {}", d.getClass());
                       d.getTags()
                        .forEach(t -> {
                            log.info("   tags: {}({}) = {}", t.getTagName(), t.getTagType(), t.getDescription());
                        });
                   });
    }


}