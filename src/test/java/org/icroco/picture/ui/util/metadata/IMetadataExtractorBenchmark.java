package org.icroco.picture.ui.util.metadata;

import org.openjdk.jmh.annotations.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class IMetadataExtractorBenchmark {

    DefaultMetadataExtractor defaultMetadataExtractor = new DefaultMetadataExtractor();
    Path                     path                     = Paths.get("src/test/resources/images/benchmark/Corse 2015-24072015-275.jpg");

    @Benchmark
    public int DefaultMetadataExtractor_orientation() {
        return defaultMetadataExtractor.orientation(path).orElse(-1);
    }

    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }

}