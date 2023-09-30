package org.icroco.picture.views.util;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.text.Font;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.model.ERotation;
import org.jooq.lambda.Unchecked;

import javax.imageio.*;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import static javafx.embed.swing.SwingFXUtils.fromFXImage;
import static javafx.embed.swing.SwingFXUtils.toFXImage;
import static org.icroco.picture.thumbnail.IThumbnailGenerator.DEFAULT_THUMB_SIZE;

@Slf4j
@UtilityClass
public class ImageUtils {
    public static volatile Image                  LOADING      = loadImage(MediaLoader.class.getResource("/images/loading-icon-png-9.jpg"));
    public static          AtomicReference<Image> NO_THUMBNAIL = new AtomicReference<>(null);

    private static Image loadImage(URL url) {
        return Unchecked.function((URL u) -> new Image(u.toURI().toString(), 64, 64, true, true),
                                  t -> log.error("Cannot load image at url: '{}'", url, t))
                        .apply(url);
    }

    public static Image getNoThumbnailImage() {
        if (Platform.isFxApplicationThread()) {
            var image = NO_THUMBNAIL.get();
            if (NO_THUMBNAIL.get() == null) {
                image = createImage("No thumbail\navailable\ninside image.");
                NO_THUMBNAIL.set(image);
            }
            return image;
        } else {
            return LOADING;
        }
    }

    static Image createImage(String text) {
        int             width  = DEFAULT_THUMB_SIZE.width();
        int             height = DEFAULT_THUMB_SIZE.height();
        Canvas          canvas = new javafx.scene.canvas.Canvas(width, height);
        GraphicsContext gc     = canvas.getGraphicsContext2D();

        gc.setFill(javafx.scene.paint.Color.GRAY);
        gc.setStroke(javafx.scene.paint.Color.BLACK);
        gc.setFont(Font.font("Arial", 24));
        gc.fillText(text, 20, (height / 2D) - 24 * 1.5);
        gc.strokeLine(0, 0, width, height);
        gc.strokeLine(0, height, width, 0);

        gc.strokeLine(0, 0, width, 0);
        gc.strokeLine(width, 0, width, height);
        gc.strokeLine(width, height, 0, height);
        gc.strokeLine(0, height, 0, 0);

        final SnapshotParameters params = new SnapshotParameters();
        params.setFill(javafx.scene.paint.Color.TRANSPARENT);
        return canvas.snapshot(params, null);
    }

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
            log.debug("Supported codec: {}", codec);
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(codec);
            while (readers.hasNext()) {
                log.debug("    reader: {}", readers.next());
            }
        }
    }

    public static Image getJavaFXImageSlow(byte[] rawPixels, int width, int height) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(createBufferedImage(rawPixels, width, height), "png", out);
            out.flush();
        } catch (IOException ex) {
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
        } catch (IOException ex) {
            log.error("Cannot read byte[] image");
        }
        return null;
    }

    public static byte[] mapAsJpg(Image image) {
        var bi = SwingFXUtils.fromFXImage(image, null);

        BufferedImage result = new BufferedImage((int) image.getWidth(), (int) image.getHeight(), BufferedImage.TYPE_INT_RGB);
        result.createGraphics().drawImage(bi, 0, 0, Color.WHITE, null);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(result, "jpg", baos);
//            baos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
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
            if (newWidth < 0) {
                newWidth = -1 * oldWidth * newWidth / 100;
            }
            if (newHeight < 0) {
                newHeight = -1 * oldHeight * newHeight / 100;
            }
            if (newWidth == 0) {
                newWidth = oldWidth * newHeight / oldHeight;
            }
            if (newHeight == 0) {
                newHeight = oldHeight * newWidth / oldWidth;
            }
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
            // This one line below created a JPEG mf without quality option
            ImageIO.write(destImage, "jpg", baos);
        }

        baos.flush();
        imageBytes = baos.toByteArray();
        baos.close();

        // Done
        return imageBytes;
    }

    public static BufferedImage rotate(BufferedImage src, ERotation[] rotations,
                                       BufferedImageOp... ops) throws IllegalArgumentException,
            ImagingOpException {
        for (ERotation rotation : rotations) {
            src = rotate(src, rotation, ops);
        }
        return src;
    }

    public static BufferedImage rotate(BufferedImage src, ERotation rotation,
                                       BufferedImageOp... ops) throws IllegalArgumentException,
            ImagingOpException {
        long t = -1;
        if (src == null) {
            throw new IllegalArgumentException("src cannot be null");
        }
        if (rotation == null) {
            throw new IllegalArgumentException("rotation cannot be null");
        }

        /*
         * Setup the default width/height values from our image.
         *
         * In the case of a 90 or 270 (-90) degree rotation, these two values
         * flip-flop and we will correct those cases down below in the switch
         * statement.
         */
        int newWidth  = src.getWidth();
        int newHeight = src.getHeight();

        /*
         * We create a transform per operation request as (oddly enough) it ends
         * up being faster for the VM to create, use and destroy these instances
         * than it is to re-use a single AffineTransform per-thread via the
         * AffineTransform.setTo(...) methods which was my first choice (less
         * object creation); after benchmarking this explicit case and looking
         * at just how much code gets run inside of setTo() I opted for a new AT
         * for every rotation.
         *
         * Besides the performance win, trying to safely reuse AffineTransforms
         * via setTo(...) would have required ThreadLocal instances to avoid
         * race conditions where two or more resize threads are manipulating the
         * same transform before applying it.
         *
         * Misusing ThreadLocals are one of the #1 reasons for memory leaks in
         * server applications and since we have no nice way to hook into the
         * init/destroy Servlet cycle or any other initialization cycle for this
         * library to automatically call ThreadLocal.remove() to avoid the
         * memory leak, it would have made using this library *safely* on the
         * server side much harder.
         *
         * So we opt for creating individual transforms per rotation op and let
         * the VM clean them up in a GC. I only clarify all this reasoning here
         * for anyone else reading this code and being tempted to reuse the AT
         * instances of performance gains; there aren't any AND you get a lot of
         * pain along with it.
         */
        AffineTransform tx = new AffineTransform();

        switch (rotation) {
            case CW_90:
                /*
                 * A 90 or -90 degree rotation will cause the height and width to
                 * flip-flop from the original image to the rotated one.
                 */
                newWidth = src.getHeight();
                newHeight = src.getWidth();

                // Reminder: newWidth == result.getHeight() at this point
                tx.translate(newWidth, 0);
                tx.quadrantRotate(1);

                break;

            case CW_270:
                /*
                 * A 90 or -90 degree rotation will cause the height and width to
                 * flip-flop from the original image to the rotated one.
                 */
                newWidth = src.getHeight();
                newHeight = src.getWidth();

                // Reminder: newHeight == result.getWidth() at this point
                tx.translate(0, newHeight);
                tx.quadrantRotate(3);
                break;

            case CW_180:
                tx.translate(newWidth, newHeight);
                tx.quadrantRotate(2);
                break;

            case FLIP_HORZ:
                tx.translate(newWidth, 0);
                tx.scale(-1.0, 1.0);
                break;

            case FLIP_VERT:
                tx.translate(0, newHeight);
                tx.scale(1.0, -1.0);
                break;
        }

        // Create our target image we will render the rotated result to.
        BufferedImage result = createOptimalImage(src, newWidth, newHeight);
        Graphics2D    g2d    = (Graphics2D) result.createGraphics();

        /*
         * Render the resultant image to our new rotatedImage buffer, applying
         * the AffineTransform that we calculated above during rendering so the
         * pixels from the old position are transposed to the new positions in
         * the resulting image correctly.
         */
        g2d.drawImage(src, tx, null);
        g2d.dispose();

        // Apply any optional operations (if specified).
        if (ops != null && ops.length > 0) {
            result = apply(result, ops);
        }

        return result;
    }

    static BufferedImage createOptimalImage(BufferedImage src,
                                            int width, int height) throws IllegalArgumentException {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width [" + width
                                               + "] and height [" + height + "] must be > 0");
        }

        return new BufferedImage(width,
                                 height,
                                 (src.getTransparency() == Transparency.OPAQUE ? BufferedImage.TYPE_INT_RGB
                                                                               : BufferedImage.TYPE_INT_ARGB));
    }

    public static BufferedImage apply(BufferedImage src, BufferedImageOp... ops)
            throws IllegalArgumentException, ImagingOpException {
        long t = -1;


        if (src == null) {
            throw new IllegalArgumentException("src cannot be null");
        }
        if (ops == null || ops.length == 0) {
            throw new IllegalArgumentException("ops cannot be null or empty");
        }

        int type = src.getType();

        /*
         * Ensure the src image is in the best supported image type before we
         * continue, otherwise it is possible our calls below to getBounds2D and
         * certainly filter(...) may fail if not.
         *
         * Java2D makes an attempt at applying most BufferedImageOps using
         * hardware acceleration via the ImagingLib internal library.
         *
         * Unfortunately may of the BufferedImageOp are written to simply fail
         * with an ImagingOpException if the operation cannot be applied with no
         * additional information about what went wrong or attempts at
         * re-applying it in different ways.
         *
         * This is assuming the failing BufferedImageOp even returns a null
         * image after failing to apply; some simply return a corrupted/black
         * image that result in no exception and it is up to the user to
         * discover this.
         *
         * In internal testing, EVERY failure I've ever seen was the result of
         * the source image being in a poorly-supported BufferedImage Type like
         * BGR or ABGR (even though it was loaded with ImageIO).
         *
         * To avoid this nasty/stupid surprise with BufferedImageOps, we always
         * ensure that the src image starts in an optimally supported format
         * before we try and apply the filter.
         */
        if (!(type == BufferedImage.TYPE_INT_RGB || type == BufferedImage.TYPE_INT_ARGB)) {
            src = copyToOptimalImage(src);
        }

        boolean hasReassignedSrc = false;

        for (int i = 0; i < ops.length; i++) {
            long subT = -1;

            BufferedImageOp op = ops[i];

            // Skip null ops instead of throwing an exception.
            if (op == null) {
                continue;
            }

            /*
             * Must use op.getBounds instead of src.getWidth and src.getHeight
             * because we are trying to create an image big enough to hold the
             * result of this operation (which may be to scale the image
             * smaller), in that case the bounds reported by this op and the
             * bounds reported by the source image will be different.
             */
            Rectangle2D resultBounds = op.getBounds2D(src);

            // Watch out for flaky/misbehaving ops that fail to work right.
            if (resultBounds == null) {
                throw new ImagingOpException(
                        "BufferedImageOp ["
                        + op.toString()
                        +
                        "] getBounds2D(src) returned null bounds for the target image; this should not happen and indicates a problem with application of this type of op.");
            }

            /*
             * We must manually create the target image; we cannot rely on the
             * null-destination filter() method to create a valid destination
             * for us thanks to this JDK bug that has been filed for almost a
             * decade:
             * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4965606
             */
            BufferedImage dest = createOptimalImage(src,
                                                    (int) Math.round(resultBounds.getWidth()),
                                                    (int) Math.round(resultBounds.getHeight()));

            // Perform the operation, update our result to return.
            BufferedImage result = op.filter(src, dest);

            /*
             * Flush the 'src' image ONLY IF it is one of our interim temporary
             * images being used when applying 2 or more operations back to
             * back. We never want to flush the original image passed in.
             */
            if (hasReassignedSrc) {
                src.flush();
            }

            /*
             * Incase there are more operations to perform, update what we
             * consider the 'src' reference to our last result so on the next
             * iteration the next op is applied to this result and not back
             * against the original src passed in.
             */
            src = result;

            /*
             * Keep track of when we re-assign 'src' to an interim temporary
             * image, so we know when we can explicitly flush it and clean up
             * references on future iterations.
             */
            hasReassignedSrc = true;

        }

        return src;
    }

    static BufferedImage copyToOptimalImage(BufferedImage src)
            throws IllegalArgumentException {
        if (src == null) {
            throw new IllegalArgumentException("src cannot be null");
        }

        // Calculate the type depending on the presence of alpha.
        int type = (src.getTransparency() == Transparency.OPAQUE
                    ? BufferedImage.TYPE_INT_RGB
                    : BufferedImage.TYPE_INT_ARGB);
        BufferedImage result = new BufferedImage(src.getWidth(), src.getHeight(), type);

        // Render the src image into our new optimal source.
        Graphics g = result.getGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();

        return result;
    }

}
