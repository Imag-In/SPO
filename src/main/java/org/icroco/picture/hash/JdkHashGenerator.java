package org.icroco.picture.hash;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.icroco.picture.util.Constant;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.concurrent.Semaphore;

@Slf4j
public class JdkHashGenerator implements IHashGenerator {

    private final Semaphore semaphore = new Semaphore(Constant.NB_CORE);

    private final MessageDigest digest;

    {
        final var name = "SHA3-256";
        try {
            digest = MessageDigest.getInstance(name);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(STR."Cannot find jdk MessageDigest: \{name}", e);
        }
    }

    @Override
    public Optional<String> compute(Path path) {
        semaphore.acquireUninterruptibly();

        try {
            return Optional.of(DigestUtils.md5Hex(Files.newInputStream(path)));
//            return Optional.ofNullable(digest.digest(Files.readAllBytes(path)))
//                           .map(JdkHashGenerator::bytesToHex);
        } catch (IOException e) {
            log.warn("Cannot compute hash for file: '{}', error: {}", path, e.getLocalizedMessage());
        } finally {
            semaphore.release();
        }

        return Optional.empty();
    }

    static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
