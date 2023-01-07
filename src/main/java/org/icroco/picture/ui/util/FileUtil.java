package org.icroco.picture.ui.util;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;
import java.util.stream.Stream;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
@UtilityClass
@Slf4j
public class FileUtil {
    public static boolean isEmpty(Path path)  {
        if (Files.isDirectory(path)) {
            try (Stream<Path> entries = Files.list(path)) {
                return entries.findFirst().isEmpty();
            }
            catch (IOException ex) {
                log.debug("Un-expected error", ex);
                return true;
            }
        }

        return false;
    }

    public static boolean isLastDir(Path path)  {
        if (Files.isDirectory(path)) {
            try (Stream<Path> entries = Files.list(path).filter(Files::isDirectory)) {
                return entries.findFirst().isEmpty();
            }
            catch (IOException ex) {
                log.debug("Un-expected error", ex);
                return true;
            }
        }

        return false;
    }

    public static boolean isNotEmpty(Path path) {
        return !isEmpty(path);
    }


}
