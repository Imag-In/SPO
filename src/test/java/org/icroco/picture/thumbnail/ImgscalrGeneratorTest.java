package org.icroco.picture.thumbnail;

import org.icroco.picture.hash.JdkHashGenerator;
import org.icroco.picture.metadata.DefaultMetadataExtractor;
import org.icroco.picture.metadata.KeywordManager;
import org.icroco.picture.model.Dimension;
import org.icroco.picture.persistence.KeywordRepository;
import org.icroco.picture.persistence.mapper.KeywordMapperImpl;
import org.icroco.picture.views.task.TaskService;
import org.icroco.picture.views.util.ImageUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Collections;

import static org.mockito.Mockito.mock;

class ImgscalrGeneratorTest {

    @Test
    @Disabled
    void compute_thumbnail_size() {
        var
                generator =
                new ImgscalrGenerator(new JdkHashGenerator(), new DefaultMetadataExtractor(new KeywordManager(mock(KeywordRepository.class),
                                                                                                              new KeywordMapperImpl(),
                                                                                                              Collections.emptyMap()),
                                                                                           mock(TaskService.class)));
        var
                thumbnail  =
                generator.generate(Path.of("src/test/resources/images/benchmark/Corse 2015-20072015-036.jpg"), new Dimension(320, 302));
        var     dataBuffer = ImageUtils.getRawImage(thumbnail.getImage());

// Each bank element in the data buffer is a 32-bit integer
        long sizeBytes = ((long) dataBuffer.length) * 4l;
        long sizeMB    = sizeBytes / (1024l * 1024l);
    }
}