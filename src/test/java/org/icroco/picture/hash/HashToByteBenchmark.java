package org.icroco.picture.hash;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.jooq.lambda.Unchecked;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
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
public class HashToByteBenchmark {

    List<Path> paths;

    List<byte[]> bytes;

    private final MessageDigest digestMd5;

    {
        try {
            digestMd5 = MessageDigest.getInstance("MD5");
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private final DigestUtils digestApacheSha3_256 = new DigestUtils("SHA3-256");

    @Setup
    public void setup() {
        paths = List.of(Paths.get("src/test/resources/images/benchmark/Corse 2015-20072015-036.jpg"),
                        Paths.get("src/test/resources/images/benchmark/Corse 2015-24072015-275.jpg"));

        bytes = paths.stream()
                     .map(path -> {
                         try {
                             return digestApacheSha3_256.digest(Files.readAllBytes(path));
                         }
                         catch (IOException e) {
                             throw new RuntimeException(e);
                         }
                     })
                     .toList();
    }


    @Benchmark
    public void toHexCustom(Blackhole blackhole) {
        paths.stream()
             .map(Unchecked.function(Files::readAllBytes))
             .map(JdkHashGenerator::bytesToHex)
             .forEach(blackhole::consume);
    }

    @Benchmark
    public void toHexApache(Blackhole blackhole) {
        paths.stream()
             .map(Unchecked.function(Files::readAllBytes))
             .map(Hex::encodeHexString)
             .forEach(blackhole::consume);
    }

    @Benchmark
    public void allBytesToHexApache(Blackhole blackhole) {
        paths.stream()
             .map(Unchecked.function(Files::readAllBytes))
             .map(Hex::encodeHexString)
             .forEach(blackhole::consume);
    }

    @Benchmark
    public void streamToMd5Apache(Blackhole blackhole) {
        paths.stream()
             .map(Unchecked.function(this::calcMD5))
             .forEach(blackhole::consume);
    }

    @Benchmark
    public void streamToMd5Apache2(Blackhole blackhole) {
        paths.stream()
//             .map(Unchecked.function(Files::readAllBytes))
             .map(p -> Unchecked.supplier(() -> DigestUtils.md5Hex(Files.newInputStream(p))))
             .forEach(blackhole::consume);
    }


    public String calcMD5(Path p) throws Exception {
        byte[] buffer = new byte[8192 * 2];
        try (DigestInputStream dis = new DigestInputStream(Files.newInputStream(p), digestMd5)) {
            while (dis.read(buffer) != -1) ;
        }

        byte[] bytes = digestMd5.digest();
        return Hex.encodeHexString(bytes);
    }

    public String calcMD5(Path p, byte[] buffer) throws Exception {
        try (DigestInputStream dis = new DigestInputStream(Files.newInputStream(p), digestMd5)) {
            while (dis.read(buffer) != -1) ;
        }
        byte[] bytes = digestMd5.digest();
        return Hex.encodeHexString(bytes);
    }


    @Benchmark
    public void streamToHexApache(Blackhole blackhole) {
        paths.stream()
             .map(p -> {
                 try (InputStream is = Files.newInputStream(p)) {
                     return org.apache.commons.codec.digest.DigestUtils.md5Hex(is);
                 }
                 catch (IOException e) {
                     throw new RuntimeException(e);
                 }
             })
             .forEach(blackhole::consume);
    }

    public static void main(String[] args) throws Exception {
//        org.openjdk.jmh.Main.java.main(args);
        Options opt = new OptionsBuilder()
                .include(HashToByteBenchmark.class.getSimpleName())
//                .forks(1)
                .build();

        new Runner(opt).run();
    }
}