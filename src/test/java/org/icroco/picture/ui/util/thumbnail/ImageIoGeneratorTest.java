package org.icroco.picture.ui.util.thumbnail;

import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.ui.util.Dimension;
import org.icroco.picture.ui.util.hash.JdkHashGenerator;
import org.icroco.picture.ui.util.metadata.DefaultMetadataExtractor;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;

@Slf4j
class ImageIoGeneratorTest {
    ImgscalrGenerator generator = new ImgscalrGenerator(new JdkHashGenerator(), new DefaultMetadataExtractor());

    @Test
    void read_thumbnail() throws IOException {
        ThumbnailatorGeneratorTest.getImages()
                                  .forEach(path -> {
                                      long start = System.currentTimeMillis();
                                      var  bi    = generator.generate(path, new Dimension(300, 300));
                                      log.info("{} w={}, h={}, time: {}",
                                               path,
                                               bi.getImage().getWidth(),
                                               bi.getImage().getHeight(),
                                               (System.currentTimeMillis() - start));
                                  });
    }

    @Test
    void extract_thumbnail() {
        generator.extractThumbnail(Paths.get("/Users/christophe/Pictures/Holidays/Ete/Espagne 2017/Corse-29072017-267.jpg"));
    }

}