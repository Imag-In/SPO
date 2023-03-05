package org.icroco.picture.ui.util.hash;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

@Slf4j
public class JdkHashGenerator implements IHashGenerator {

    private final MessageDigest digest;

    {
        final var name = "SHA3-256";
        try {
            digest = MessageDigest.getInstance(name);
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Cannot finf jdk MessageDigest: " + name, e);
        }
    }

    @Override
    public Optional<String> compute(Path path) {
        try {
            return Optional.ofNullable(digest.digest(Files.readAllBytes(path)))
                           .map(JdkHashGenerator::bytesToHex);
        }
        catch (IOException e) {
            log.warn("Cannot compute hash for file: '{}', error: {}", path, e.getLocalizedMessage());
        }

        return Optional.empty();
    }

    private static String bytesToHex(byte[] hash) {
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
