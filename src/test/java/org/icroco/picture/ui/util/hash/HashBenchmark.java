package org.icroco.picture.ui.util.hash;

import org.apache.commons.codec.digest.DigestUtils;
import org.jooq.lambda.Unchecked;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.TimeUnit;

// French Tutoriel: https://www.loicmathieu.fr/wordpress/informatique/introduction-a-jmh-java-microbenchmark-harness/
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class HashBenchmark {

    List<Path> paths;
    private final MessageDigest digestMd5;
    private final MessageDigest digestSha2_256;
    private final MessageDigest digestSha3_256;
    private final DigestUtils   digestApacheSha3_256;

    {
        try {
            digestMd5 = MessageDigest.getInstance("MD5");
            digestSha2_256 = MessageDigest.getInstance("SHA-256");
            digestSha3_256 = MessageDigest.getInstance("SHA3-256");
            digestApacheSha3_256 = new DigestUtils("SHA3-256");
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Setup
    public void setup() {
        paths = List.of(Paths.get("src/test/resources/images/benchmark/Corse 2015-20072015-036.jpg"),
                        Paths.get("src/test/resources/images/benchmark/Corse 2015-24072015-275.jpg"));
    }


    @Benchmark
    public void SHA3_256_jdk(Blackhole blackhole) {
        paths.stream()
             .map(path -> Unchecked.supplier(() -> digestSha3_256.digest(Files.readAllBytes(path))))
             .forEach(blackhole::consume);
    }

    @Benchmark
    public void SHA2_256_jdk(Blackhole blackhole) {
        paths.stream()
             .map(path -> Unchecked.supplier(() -> digestSha2_256.digest(Files.readAllBytes(path))))
             .forEach(blackhole::consume);
    }

    @Benchmark
    public void MD5_jdk(Blackhole blackhole) {
        paths.stream()
             .map(path -> Unchecked.supplier(() -> digestMd5.digest(Files.readAllBytes(path))))
             .forEach(blackhole::consume);
    }


    @Benchmark
    public void SHA2_256_apache(Blackhole blackhole) {
        paths.stream()
             .map(path -> Unchecked.supplier(() -> DigestUtils.sha256Hex(Files.newInputStream(path))))
             .forEach(blackhole::consume);
    }

    @Benchmark
    public void SHA3_256_apache(Blackhole blackhole) {
        paths.stream()
             .map(path -> Unchecked.supplier(() -> digestApacheSha3_256.digest(Files.newInputStream(path))))
             .forEach(blackhole::consume);
    }

    @Benchmark
    public void MD5_apache(Blackhole blackhole) {
        paths.stream()
             .map(path -> Unchecked.supplier(() -> DigestUtils.md5(Files.newInputStream(path))))
             .forEach(blackhole::consume);
    }


    public static void main(String[] args) throws Exception {
//        org.openjdk.jmh.Main.java.main(args);
        Options opt = new OptionsBuilder()
                .include(HashBenchmark.class.getSimpleName())
//                .forks(1)
                .build();

        new Runner(opt).run();
    }
}