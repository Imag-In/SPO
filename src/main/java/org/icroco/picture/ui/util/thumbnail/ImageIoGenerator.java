package org.icroco.picture.ui.util.thumbnail;

import javafx.scene.image.Image;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.ui.util.Dimension;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

@Slf4j
public class ImageIoGenerator extends AbstractThumbnailGenerator {
    @Override
    public Image generate(Path path, Dimension dim) {
        return new Image(path.toUri().toString(), dim.width(), 0, true, true);
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
