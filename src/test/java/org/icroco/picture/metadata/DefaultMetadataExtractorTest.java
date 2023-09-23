package org.icroco.picture.metadata;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

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


}