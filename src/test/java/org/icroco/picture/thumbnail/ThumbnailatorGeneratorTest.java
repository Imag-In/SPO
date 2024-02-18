package org.icroco.picture.thumbnail;

import javafx.application.Platform;
import org.icroco.picture.metadata.DefaultMetadataExtractor;
import org.icroco.picture.metadata.TagManagerTest;
import org.icroco.picture.model.Dimension;
import org.icroco.picture.util.Constant;
import org.icroco.picture.util.FileUtil;
import org.icroco.picture.views.task.TaskService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.util.StopWatch;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

class ThumbnailatorGeneratorTest {
    //    public static final String IMAGE_DIR = "/Users/christophe/Pictures/Holidays/Ete/Espagne 2017";
    public static final String TARGET = "./target/generated-images";


    public static final String IMAGE_DIR = "./src/test/resources/images/valid";
    public static final Dimension DIM       = new Dimension(600, 600);

    @BeforeAll
    static void beforeAll() {
        System.setProperty("java.awt.headless", "true");
    }

    @AfterAll
    static void afterAll() {
        System.setProperty("java.awt.headless", "false");
    }

    @AfterEach
    void afterEach() {
        System.out.println();
        System.out.println();
    }

    static List<Path> getImages() throws IOException {
        Path root = Paths.get(IMAGE_DIR);
        try (var images = Files.walk(root)) {
            return images.filter(p -> !Files.isDirectory(p))   // not a directory
                         .filter(Constant::isSupportedExtension)
                         .toList();
        }
    }

    @Test
    void generate_thumbnails_thumbnailator() throws IOException {
        IThumbnailGenerator generator = new ThumbnailatorGenerator();

        StopWatch watch  = new StopWatch("Thumbnailator generator");
        Path      root   = Paths.get(IMAGE_DIR);
        Path      target = Paths.get(TARGET, "Thumbnailator", root.getFileName().toString());
        target.toFile().mkdirs();

        getImages().forEach(i -> {
                                watch.start(i.getFileName().toString());
                                generator.generate(i,
                                                   Paths.get(target.toString(), i.getFileName().toString()),
                                                   DIM);
                                watch.stop();
                            }
        );
        FileUtil.print(watch);
    }

    // ImgscalrGenerator generator Time: 3 minutes, 55 secondes et 449 millisecondes
    @Test
    void generate_thumbnails_imgscalr() throws IOException {
        IThumbnailGenerator
                generator =
                new ImgscalrGenerator(new DefaultMetadataExtractor(TagManagerTest.TAG_MANAGER, Mockito.mock(TaskService.class)));

        StopWatch watch  = new StopWatch("ImgscalrGenerator generator");
        Path      root   = Paths.get(IMAGE_DIR);
        Path      target = Paths.get(TARGET, "ImgscalrGenerator", root.getFileName().toString());
        target.toFile().mkdirs();

        getImages().forEach(i -> {
                                watch.start(i.getFileName().toString());
                                generator.generate(i,
                                                   Paths.get(target.toString(), i.getFileName().toString()),
                                                   DIM);
                                watch.stop();
                            }
        );
        FileUtil.print(watch);
    }

    @Test
    void generate_thumbnails_imgscalr_bytes() throws IOException {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        var generator = new ImgscalrGenerator(new DefaultMetadataExtractor(TagManagerTest.TAG_MANAGER,
                                                                                                   Mockito.mock(TaskService.class)));
        Dimension dimension = new Dimension(600, 600);

        try {
            Platform.runLater(() -> {
                for (Path path : List.of(Paths.get("src/test/resources/images/benchmark/Corse 2015-20072015-036.jpg"),
                                         Paths.get("src/test/resources/images/benchmark/Corse 2015-24072015-275.jpg"))) {
                    System.out.println(generator.generateJpg(path, dimension).data().length);
                }
            });
        } catch (Exception ex) {
            Platform.startup(() -> {
            });
        }
    }


    @Test
    void generate_thumbnails_imageio() throws IOException {
        IThumbnailGenerator generator = new ImageIoGenerator();

        StopWatch watch  = new StopWatch("JavaFx generator");
        Path      root   = Paths.get(IMAGE_DIR);
        Path      target = Paths.get(TARGET, "ImageIoGenerator", root.getFileName().toString());
        target.toFile().mkdirs();

        try (var images = Files.walk(root)) {
            images.filter(p -> !Files.isDirectory(p))   // not a directory
                  .filter(Constant::isSupportedExtension)
                  .forEach(i -> {
                               watch.start(i.getFileName().toString());
                               generator.generate(i,
                                                  Paths.get(target.toString(), i.getFileName().toString()),
                                                  DIM);
                               watch.stop();
                           }
                  );
            FileUtil.print(watch);
        }
    }
}