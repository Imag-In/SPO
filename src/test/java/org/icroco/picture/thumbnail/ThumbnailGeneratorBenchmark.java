package org.icroco.picture.thumbnail;

import org.icroco.picture.hash.JdkHashGenerator;
import org.icroco.picture.metadata.DefaultMetadataExtractor;
import org.icroco.picture.metadata.IMetadataExtractor;
import org.icroco.picture.metadata.TagManagerTest;
import org.icroco.picture.model.Dimension;
import org.icroco.picture.views.task.TaskService;
import org.mockito.Mockito;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

// French Tutoriel: https://www.loicmathieu.fr/wordpress/informatique/introduction-a-jmh-java-microbenchmark-harness/
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class ThumbnailGeneratorBenchmark {

    List<Path>             paths;

    IMetadataExtractor metadataExtractor = new DefaultMetadataExtractor(TagManagerTest.TAG_MANAGER,
                                                                        Mockito.mock(TaskService.class));
    ImgscalrGenerator      imgscalrGenerator      = new ImgscalrGenerator(new JdkHashGenerator(), metadataExtractor);
    ThumbnailatorGenerator thumbnailatorGenerator = new ThumbnailatorGenerator();

    static Dimension dimension = new Dimension(600, 600);

    @Setup
    public void setup() {
        paths = List.of(Paths.get("src/test/resources/images/benchmark/Corse 2015-20072015-036.jpg"),
                        Paths.get("src/test/resources/images/benchmark/Corse 2015-24072015-275.jpg"));
    }

    @Benchmark
    public int imgscalr() {

        return paths.stream()
                    .map(path -> imgscalrGenerator.generate(path, dimension))
                    .mapToInt(thumbnail -> (int) thumbnail.getImage().getHeight())
                    .sum();
    }

    @Benchmark
    public void Imgscalr_bytes(Blackhole blackhole) {

        paths.stream()
             .map(path -> imgscalrGenerator.generateJpg(path, dimension))
             .mapToInt(image -> image.data().length)
             .forEach(blackhole::consume);
    }

    @Benchmark
    public int Thumbnailator_bytes() {

        return paths.stream()
                    .map(path -> thumbnailatorGenerator.generateJpg(path, dimension))
                    .mapToInt(image -> image.data().length)
                    .sum();
    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(ThumbnailGeneratorBenchmark.class.getSimpleName())
//                .forks(1)
                .build();
    }
}