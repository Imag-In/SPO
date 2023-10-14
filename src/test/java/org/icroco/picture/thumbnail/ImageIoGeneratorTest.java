package org.icroco.picture.thumbnail;

import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.hash.JdkHashGenerator;
import org.icroco.picture.metadata.DefaultMetadataExtractor;
import org.icroco.picture.metadata.TagManagerTest;
import org.icroco.picture.model.Dimension;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;

@Slf4j
class ImageIoGeneratorTest {
    ImgscalrGenerator generator = new ImgscalrGenerator(new JdkHashGenerator(), new DefaultMetadataExtractor(TagManagerTest.TAG_MANAGER));

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