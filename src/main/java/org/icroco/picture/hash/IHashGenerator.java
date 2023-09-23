package org.icroco.picture.hash;

import java.nio.file.Path;
import java.util.Optional;

public interface IHashGenerator {
    Optional<String> compute(Path path);
}
