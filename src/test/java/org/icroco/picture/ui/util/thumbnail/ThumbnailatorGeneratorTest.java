package org.icroco.picture.ui.util.thumbnail;

import org.icroco.picture.ui.util.Constant;
import org.icroco.picture.ui.util.Dimension;
import org.icroco.picture.ui.util.metadata.DefaultMetadataExtractor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.StopWatch;
import org.threeten.extra.AmountFormats;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Locale;

class ThumbnailatorGeneratorTest {
    //    public static final String IMAGE_DIR = "/Users/christophe/Pictures/Holidays/Ete/Espagne 2017";
    public static final String TARGET = "/Users/christophe/dev/github/image-in/target/thumbnails";


    public static final String    IMAGE_DIR = "/Users/christophe/Pictures/foo/test";
    public static final Dimension DIM       = new Dimension(600, 600);

    @AfterEach
    void afterEach() {
        System.out.println();
        System.out.println();
    }

    @Test
    void generate_thumbnails_thumbnailator() throws IOException {
        IThumbnailGenerator generator = new ThumbnailatorGenerator();

        StopWatch watch  = new StopWatch("Thumbnailator generator");
        Path      root   = Paths.get(IMAGE_DIR);
        Path      target = Paths.get(TARGET, "Thumbnailator", root.getFileName().toString());
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
            print(watch);
        }
    }

    // ImgscalrGenerator generator Time: 3 minutes, 55 secondes et 449 millisecondes
    @Test
    void generate_thumbnails_imgscalr() throws IOException {
        IThumbnailGenerator generator = new ImgscalrGenerator(new DefaultMetadataExtractor());

        StopWatch watch  = new StopWatch("ImgscalrGenerator generator");
        Path      root   = Paths.get(IMAGE_DIR);
        Path      target = Paths.get(TARGET, "ImgscalrGenerator", root.getFileName().toString());
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
            print(watch);
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
            print(watch);
        }
    }

    private static void print(StopWatch watch) {
        System.out.println(
                watch.getId() + " Time: " + AmountFormats.wordBased(Duration.ofMillis(watch.getTotalTimeMillis()), Locale.getDefault()));
        for (StopWatch.TaskInfo t : watch.getTaskInfo()) {
            System.out.println("   " +
                               t.getTaskName() + " Time: " + AmountFormats.wordBased(Duration.ofMillis(t.getTimeMillis()), Locale.getDefault()));
        }
    }
}