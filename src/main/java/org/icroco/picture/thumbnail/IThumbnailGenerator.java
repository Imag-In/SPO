package org.icroco.picture.thumbnail;

import org.icroco.picture.model.Dimension;
import org.icroco.picture.model.Thumbnail;
import org.springframework.lang.NonNull;

import java.nio.file.Path;
import java.util.List;

public interface IThumbnailGenerator {

    //    Dimension DEFAULT_THUMB_SIZE = new Dimension(600, 600);
    Dimension DEFAULT_THUMB_SIZE = new Dimension(320, 320);

    record ThumbnailOutput(Path p, byte[] data, Throwable error) {
    }

    Thumbnail generate(Path path, Dimension dim);

    default Thumbnail generate(Path path) {
        return generate(path, DEFAULT_THUMB_SIZE);
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

    @NonNull
    Thumbnail extractThumbnail(Path path);

    @NonNull
    default List<Thumbnail> extractThumbnail(List<Path> paths) {
        return paths.stream()
                    .map(this::extractThumbnail)
                    .toList();
    }


    void generate(Path source, Path target, Dimension dim);

//    default void generate(Path source, Dimension dim, Consumer<BufferedImage> consummer) {
//
//        var thumbnail  = generate(source, dim);
//
//        .ifLeftOrElse(throwable -> { /* TODO */},
//                                           thumbnail ->
//                                                   consummer.accept(SwingFXUtils.fromFXImage(thumbnail.getImage(), null)));
//    }
//
//    default void generate(List<Path> source, Dimension dim, Consumer<BufferedImage> consummer) {
//        source.forEach(s -> generate(s, dim, consummer));
//    }
}
