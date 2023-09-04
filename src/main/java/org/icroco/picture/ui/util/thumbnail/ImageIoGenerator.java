package org.icroco.picture.ui.util.thumbnail;

import javafx.scene.image.Image;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.ui.model.Dimension;
import org.icroco.picture.ui.model.EThumbnailType;
import org.icroco.picture.ui.model.Thumbnail;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;

@Slf4j
public class ImageIoGenerator extends AbstractThumbnailGenerator {
    @Override
    public Thumbnail generate(Path path, Dimension dim) {
        return new Thumbnail(0,
                             path,
                             new Image(path.toUri().toString(), dim.width(), 0, true, true),
                             EThumbnailType.GENERATED,
                             LocalDateTime.now());
    }

    @Override
    public ThumbnailOutput generateJpg(Path path, Dimension dim) {
        throw new RuntimeException("Not Yet Implemented");
    }

    @Override
    public void generate(Path source, Path target, Dimension dim) {
        try {
            var bImage = ImageIO.read(source.toFile());
            deleteFile(target);
            BufferedImage img = new BufferedImage(dim.width(), dim.height(), BufferedImage.TYPE_INT_RGB);
            img.createGraphics().drawImage(bImage.getScaledInstance(dim.width(), dim.width(), java.awt.Image.SCALE_SMOOTH), 0, 0, null);
            ImageIO.write(img, "jpg", target.toFile());
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
