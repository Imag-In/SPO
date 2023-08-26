package org.icroco.picture.ui.util.thumbnail;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.icroco.picture.ui.model.Thumbnail;
import org.icroco.picture.ui.util.Dimension;
import org.springframework.lang.NonNull;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public interface IThumbnailGenerator {

    //    Dimension DEFAULT_THUMB_SIZE = new Dimension(600, 600);
    Dimension DEFAULT_THUMB_SIZE = new Dimension(320, 320);

    record ThumbnailOutput(Path p, byte[] data, Throwable error) {}

    Thumbnail generate(Path path, Dimension dim);

    default Image generate(@NonNull Thumbnail thumbnail) {
//        var newThumb = generate(thumbnail.getFullPath(), DEFAULT_THUMB_SIZE);
//        thumbnail.setThumbnail(newThumb.getThumbnail());
//        thumbnail.setHashDate(newThumb.getHashDate());
//        thumbnail.setHash(newThumb.getHash());
//        return  thumbnail;

        return generate(thumbnail.getFullPath(), DEFAULT_THUMB_SIZE).getImage();
    }

    default List<Thumbnail> generate(List<Path> paths, Dimension dim) {
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

    Thumbnail extractThumbnail(Path path);

    default List<Thumbnail> extractThumbnail(List<Path> paths) {
        return paths.stream()
                    .map(this::extractThumbnail)
                    .toList();
    }


    void generate(Path source, Path target, Dimension dim);

    default void generate(Path source, Dimension dim, Consumer<BufferedImage> consummer) {
        consummer.accept(SwingFXUtils.fromFXImage(generate(source, dim).getImage(), null));
    }

    default void generate(List<Path> source, Dimension dim, Consumer<BufferedImage> consummer) {
        source.forEach(s -> generate(s, dim, consummer));
    }
}
