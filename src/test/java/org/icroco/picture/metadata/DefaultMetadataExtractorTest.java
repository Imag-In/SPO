package org.icroco.picture.metadata;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.assertj.core.api.Assertions;
import org.icroco.picture.views.task.TaskService;
import org.icroco.picture.views.util.Collections;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

@Slf4j
class DefaultMetadataExtractorTest {

    IMetadataExtractor extractor = new DefaultMetadataExtractor(TagManagerTest.TAG_MANAGER, Mockito.mock(TaskService.class));

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

//        var path = Path.of("/Users/christophe/Pictures/foo/json/Imag'In-Icon_Only-128x128-FF.png");
        var path = Path.of("/Users/christophe/Pictures/foo/5stars/Grece 2016-27042016-123.jpg");

        extractor.getAllByDirectory(path)
                 .forEach(md -> {
                     log.info("Directory: '{}'", md.simpleName());
                     md.entries().forEach((key, value) -> log.info("   {}: '{}'", key, value));
                 });
//        Assertions.assertThatThrownBy(() -> {
//            Metadata
//                    metadata =
//                    ImageMetadataReader.readMetadata(path.toFile());
//
//            Collections.toStream(metadata.getDirectories())
//                       .forEach(d -> {
//                           log.info("Directory: {}", d.getClass());
//                           d.getTags()
//                            .forEach(t -> {
//                                log.info("   tags: {}({}) = {}", t.getTagName(), t.getTagType(), t.getDescription());
//                            });
//                       });
//        }).isInstanceOf(ImageProcessingException.class);
    }

    @Test
    @Disabled
    void multiKeyMap_test() {
        MultiKeyMap<Object, Object> map = new MultiKeyMap<>();

        map.put("Foo", "Bar", "FooBarValue");
        map.put("Foo", "Bar2", "FooBar2Value");
        map.put("Foo", "Bar", "42", "FooBar2Value");

        map.forEach((multiKey, o) -> log.info("{}: {}", Arrays.toString(multiKey.getKeys()), o));

        log.info("Key foo: {}", map.get("Foo", "Bar"));
        map.removeMultiKey("Foo", "Bar");
        log.info("Key foo: {}", map.get("Foo", "Bar"));
        log.info("Key foo: {}", map.get("Foo", "Bar", "42"));


    }

}