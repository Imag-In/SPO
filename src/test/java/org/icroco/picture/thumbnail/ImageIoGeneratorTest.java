package org.icroco.picture.thumbnail;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.icroco.picture.hash.JdkHashGenerator;
import org.icroco.picture.metadata.DefaultMetadataExtractor;
import org.icroco.picture.metadata.TagManagerTest;
import org.icroco.picture.model.Dimension;
import org.icroco.picture.model.Thumbnail;
import org.icroco.picture.views.task.TaskService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Paths;

@Slf4j
class ImageIoGeneratorTest {
    ImgscalrGenerator generator = new ImgscalrGenerator(new JdkHashGenerator(),
                                                        new DefaultMetadataExtractor(TagManagerTest.TAG_MANAGER,
                                                                                     Mockito.mock(TaskService.class)));

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
        var thumbnail = generator.extractThumbnail(Paths.get("./src/test/resources/images/benchmark/Corse 2015-24072015-275.jpg"));
        Assertions.assertThat(thumbnail).isNotNull()
                  .extracting(Thumbnail::getImage).isNotNull();
    }

}