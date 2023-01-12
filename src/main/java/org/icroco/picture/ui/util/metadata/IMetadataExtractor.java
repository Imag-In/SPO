package org.icroco.picture.ui.util.metadata;

import org.apache.commons.io.input.ReaderInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public interface IMetadataExtractor {

    default Optional<Integer> orientation(Path path) {
        try {
            return orientation(new ReaderInputStream(Files.newBufferedReader(path), Charset.defaultCharset()));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    ;

    Optional<Integer> orientation(InputStream input);
}
