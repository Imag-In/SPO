package org.icroco.picture.ui.util.metadata;

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
    }

}