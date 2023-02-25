package org.icroco.picture.ui.util.thumbnail;

import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.ui.util.Dimension;
import org.icroco.picture.ui.util.metadata.DefaultMetadataExtractor;
import org.junit.jupiter.api.Test;

import java.io.IOException;

@Slf4j
class ImageIoGeneratorTest {
    ImgscalrGenerator generator = new ImgscalrGenerator(new DefaultMetadataExtractor());

    @Test
    void read_thumbnail() throws IOException {
        ThumbnailatorGeneratorTest.getImages()
                                  .forEach(path -> {
                                      long start = System.currentTimeMillis();
                                      var  bi    = generator.foo(path, new Dimension(300, 300));
                                      log.info("{} w={}, h={}, time: {}", path, bi.getWidth(), bi.getHeight(), (System.currentTimeMillis() - start));
                                  });
    }

}