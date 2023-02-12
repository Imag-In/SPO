package org.icroco.picture.ui.util.thumbnail;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.icroco.picture.ui.util.Dimension;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public interface IThumbnailGenerator {
    record ThumbnailOutput(Path p, byte[] data, Throwable error) {}

    Image generate(Path path, Dimension dim);

    default List<Image> generate(List<Path> paths, Dimension dim) {
        return paths.stream()
                    .map(p -> generate(p, dim))
                    .toList();
    }

    ThumbnailOutput generateJpg(Path path, Dimension dim);

    default List<ThumbnailOutput> generateJpg(List<Path> paths, Dimension dim) {
        return paths.stream()
                    .map(p -> generateJpg(p, dim))
                    .toList();
    }


    void generate(Path source, Path target, Dimension dim);

    default void generate(Path source, Dimension dim, Consumer<BufferedImage> consummer) {
        consummer.accept(SwingFXUtils.fromFXImage(generate(source, dim), null));
    }

    default void generate(List<Path> source, Dimension dim, Consumer<BufferedImage> consummer) {
        source.forEach(s -> generate(s, dim, consummer));
    }
}
