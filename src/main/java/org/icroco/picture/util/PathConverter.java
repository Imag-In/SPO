package org.icroco.picture.util;

import javafx.util.StringConverter;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PathConverter extends StringConverter<Path> {
    @Override
    public String toString(Path path) {
        return path.getFileName().toString();
    }

    @Override
    public Path fromString(String path) {
        return Paths.get(path);
    }
}
