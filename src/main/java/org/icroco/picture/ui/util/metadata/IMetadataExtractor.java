package org.icroco.picture.ui.util.metadata;

import org.slf4j.Logger;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;

public interface IMetadataExtractor {
    Logger log = org.slf4j.LoggerFactory.getLogger(DefaultMetadataExtractor.class);

    default Optional<Integer> orientation(Path path) {
        try (var input = new BufferedInputStream(new FileInputStream(path.toFile()))) {
            return orientation(input);
        }
        catch (IOException e) {
            throw new RuntimeException("Path: " + path, e);
        }
    }

    Optional<Integer> orientation(InputStream input);


    default Optional<MetadataHeader> header(Path path) {
        try (var input = new BufferedInputStream(new FileInputStream(path.toFile()))) {
            return header(path, input);
        }
        catch (Throwable t) {
            log.warn("Cannot read header for file: {}, message: {}", path, t.getLocalizedMessage());
            return Optional.empty();
        }
    }

    Optional<MetadataHeader> header(Path path, InputStream input);


}
