package org.icroco.picture.ui.util.thumbnail;

import javafx.embed.swing.SwingFXUtils;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.resizers.configurations.Antialiasing;
import org.icroco.picture.ui.model.Dimension;
import org.icroco.picture.ui.model.Thumbnail;
import org.icroco.picture.ui.util.ImageUtils;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Component
public class ThumbnailatorGenerator extends AbstractThumbnailGenerator {
    @Override
    public Thumbnail generate(Path path, Dimension dim) {
        try {
            SwingFXUtils.toFXImage(Thumbnails.of(path.toFile())
                                             .size(dim.width(), dim.height())
                                             .keepAspectRatio(true)
                                             .antialiasing(Antialiasing.ON)
                                             .asBufferedImage(), null);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public ThumbnailOutput generateJpg(Path path, Dimension dim) {
        try {
            return new ThumbnailOutput(path,
                                       ImageUtils.toByteArray(Thumbnails.of(path.toFile())
                                                                        .size(dim.width(), dim.height())
                                                                        .keepAspectRatio(true)
                                                                        .antialiasing(Antialiasing.ON)
                                                                        .asBufferedImage(), "jpg"),
                                       null);
        }
        catch (IOException e) {
            return new ThumbnailOutput(path, null, e);
        }
    }

    @Override
    public void generate(List<Path> source, Dimension dim, Consumer<BufferedImage> consummer) {
        try {
            Thumbnails.fromFiles(source.stream().map(Path::toFile).toList())
                      .size(dim.width(), dim.height())
                      .keepAspectRatio(true)
                      .antialiasing(Antialiasing.ON)
                      .allowOverwrite(true)
                      .asBufferedImages()
                      .forEach(consummer);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void generate(Path source, Path target, Dimension dim) {
        try {
            Thumbnails.of(source.toFile())
                      .size(dim.width(), dim.height())
                      .keepAspectRatio(true)
                      .antialiasing(Antialiasing.ON)
                      .allowOverwrite(true)
                      .toFile(target.toFile());
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
