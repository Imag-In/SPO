package org.icroco.picture.ui.util;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.*;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Locale;

import static javafx.embed.swing.SwingFXUtils.fromFXImage;
import static javafx.embed.swing.SwingFXUtils.toFXImage;

@Slf4j
@UtilityClass
public class ImageUtils {

//    public static Image fromByte(byte[] rawPixels) {
//        javafx.embed.swing.SwingFXUtils.toFXImage(bufferedImage, null)
//        ByteBuffer bgraPixels = convertToBgra(rawPixels); // convert your byte[] array into a ByteBuffer in bgra format.
//        Image      img        = new WritableImage(new PixelBuffer<>(width, height, bgraImage, WritablePixelFormat.getByteBgraPreInstance()));
//
//    }

//    static {
//        for (String codec : ImageIO.getReaderFormatNames()) {
//            log.info("Supported codex: {}", codec);
//        }
//    }

    public static void readImageIoCodec() {
        for (String codec : ImageIO.getReaderFormatNames()) {
            log.info("Supported codec: {}", codec);
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(codec);
            while (readers.hasNext()) {
                log.info("    reader: {}", readers.next());
            }
        }
    }

    public static Image getJavaFXImageSlow(byte[] rawPixels, int width, int height) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(createBufferedImage(rawPixels, width, height), "png", out);
            out.flush();
        }
        catch (IOException ex) {
            log.error("Cannot convert byte array to Image", ex);
        }
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        return new javafx.scene.image.Image(in);
    }

    public static Image getJavaFXImage(byte[] rawPixels, int width, int height) {
        return toFXImage(createBufferedImage(rawPixels, width, height), null);
    }

    public static Image mapAsJpg(byte[] image) {
        if (image == null || image.length == 0) {
            log.error("Cannot read  empty or null byte array: ");
            return null;
        }
        try (InputStream is = new ByteArrayInputStream(image)) {
            BufferedImage bi = ImageIO.read(is);
            return SwingFXUtils.toFXImage(bi, null);
        }
        catch (IOException ex) {
            log.error("Cannot read byte[] image");
        }
        return null;
    }

    public static byte[] mapAsJpg(Image image) {
        var           bi     = SwingFXUtils.fromFXImage(image, null);

        BufferedImage result = new BufferedImage((int) image.getWidth(), (int) image.getHeight(), BufferedImage.TYPE_INT_RGB);
        result.createGraphics().drawImage(bi, 0, 0, Color.WHITE, null);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(result, "jpg", baos);
//            baos.flush();
            return baos.toByteArray();
        }
        catch (IOException e) {
            log.error("Cannot comvert image: " + image.getUrl() + " to byte array");
        }
        return null;
    }

    public static byte[] toByteArray(BufferedImage bi, String format)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bi, format, baos);
        return baos.toByteArray();
    }

    // convert byte[] to BufferedImage
    public static BufferedImage toBufferedImage(byte[] bytes)
            throws IOException {
        InputStream is = new ByteArrayInputStream(bytes);
        return ImageIO.read(is);
    }

    public static byte[] getRawImage(Image image) {
        int w = (int) image.getWidth();
        int h = (int) image.getHeight();

// Create a new Byte Buffer, but we'll use BGRA (1 byte for each channel) //

        byte[] buf = new byte[w * h * 4];

/* Since you can get the output in whatever format with a WritablePixelFormat,
  we'll use an already created one for ease-of-use. */

        image.getPixelReader().getPixels(0, 0, w, h, PixelFormat.getByteBgraInstance(), buf, 0, w * 4);
        return buf;
    }


    public Image getJavaFXImageNotWorking(byte[] rawPixels, int width, int height) {
//        ByteBuffer bgraPixels = convertToBgra(rawPixels); // convert your byte[] array into a ByteBuffer in bgra format.
//        return new WritableImage(new PixelBuffer<>(width, height, bgraImage, WritablePixelFormat.getByteBgraPreInstance()));
        return null;
    }


    private static BufferedImage createBufferedImage(byte[] pixels, int width, int height) {
        SampleModel     sm     = getIndexSampleModel(width, height);
        DataBuffer      db     = new DataBufferByte(pixels, width * height, 0);
        WritableRaster  raster = Raster.createWritableRaster(sm, db, null);
        IndexColorModel cm     = getDefaultColorModel();

        return new BufferedImage(cm, raster, false, null);
    }

    private static SampleModel getIndexSampleModel(int width, int height) {
        IndexColorModel icm         = getDefaultColorModel();
        WritableRaster  wr          = icm.createCompatibleWritableRaster(1, 1);
        SampleModel     sampleModel = wr.getSampleModel();
        sampleModel = sampleModel.createCompatibleSampleModel(width, height);
        return sampleModel;
    }

    private static IndexColorModel getDefaultColorModel() {
        byte[] r = new byte[256];
        byte[] g = new byte[256];
        byte[] b = new byte[256];
        for (int i = 0; i < 256; i++) {
            r[i] = (byte) i;
            g[i] = (byte) i;
            b[i] = (byte) i;
        }
        return new IndexColorModel(8, 256, r, g, b);
    }

    public static byte[] imageToJPEGByteArray(Image aImage) throws IOException {
        return imageToJPEGByteArray(aImage, -100, -100, 0);
    }


    // Converts image to JPEG a byte array of the resulting JPEG. Ability to resize and adjust JPEG quality.
// Negative width and height values = %, such that -50 = 50% and -200 = 200%
// Positive width and height values = pixel dimensions
// If one value (either width or height) are 0, then the other value is proportionally
//  calculated from the first.
// If both width and height are 0 or -100, no resizing takes place
// If quality = -1, use Java's default quality
    public static byte[] imageToJPEGByteArray(Image aImage, int width, int height, int qualityPercent) throws IOException {
        byte[] imageBytes = new byte[0];

        if ((qualityPercent < -1) || (qualityPercent > 100)) {
            throw new IllegalArgumentException("Quality out of bounds!");
        }
        float quality = qualityPercent / 100f;

        double oldWidth  = aImage.getWidth();
        double oldHeight = aImage.getHeight();
        if (oldWidth == 0 || oldHeight == 0) {
            throw new IllegalArgumentException("Source image with 0 width and/or height!");
        }

        boolean resize = (width != 0 || height != 0) && (width != -100 || height != -100);

        BufferedImage destImage;
        if (resize) {
            double newWidth  = (double) width;
            double newHeight = (double) height;
            // Calculate new dimensions
            if (newWidth < 0) newWidth = -1 * oldWidth * newWidth / 100;
            if (newHeight < 0) newHeight = -1 * oldHeight * newHeight / 100;
            if (newWidth == 0) newWidth = oldWidth * newHeight / oldHeight;
            if (newHeight == 0) newHeight = oldHeight * newWidth / oldWidth;
            // Convert JavaFX image to BufferedImage and transform according to new dimensions
            destImage = new BufferedImage((int) newWidth, (int) newHeight, BufferedImage.TYPE_INT_RGB);
            BufferedImage   srcImage = fromFXImage(aImage, null);
            Graphics2D      g        = destImage.createGraphics();
            AffineTransform at       = AffineTransform.getScaleInstance(newWidth / oldWidth, newHeight / oldHeight);
            g.drawRenderedImage(srcImage, at);
            g.dispose();
        } else {
            destImage = fromFXImage(aImage, null);
        }

        // Output JPEG byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        if (qualityPercent != -1) {
            // Start to create JPEG with quality option
            ImageWriter           writer = null;
            Iterator<ImageWriter> iter   = ImageIO.getImageWritersByFormatName("jpg");
            if (iter.hasNext()) {
                writer = (ImageWriter) iter.next();
            }
            ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
            writer.setOutput(ios);
            ImageWriteParam iwparam = new JPEGImageWriteParam(Locale.getDefault());
            iwparam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            iwparam.setCompressionQuality(quality);
            writer.write(null, new IIOImage(destImage, null, null), iwparam);
            ios.flush();
            writer.dispose();
            ios.close();
            // Done creating JPEG with quality option
        } else {
            // This one line below created a JPEG file without quality option
            ImageIO.write(destImage, "jpg", baos);
        }

        baos.flush();
        imageBytes = baos.toByteArray();
        baos.close();

        // Done
        return imageBytes;
    }
}
