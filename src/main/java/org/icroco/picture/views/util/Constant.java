package org.icroco.picture.views.util;

import lombok.experimental.UtilityClass;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@UtilityClass
public class Constant {

    public static final int     NB_CORE       = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
    public              Pattern SUPPORTED_EXT = Pattern.compile(".*\\.(png|jpg|jpeg)$", Pattern.CASE_INSENSITIVE);

    private static final double HALF          = 0.5d;
    /**
     * This is the number of Zoom-In operations required to
     * <b><i>almost exactly</i></b>
     * halve the size of the Viewport.
     */
    public static final  int    ZOOM_N        = 9; // TODO try.: 1 <= ZOOM_N <= 20"-ish"
    /**
     * This factor guarantees that after
     * {@link  #ZOOM_N}
     * times Zoom-In, the Viewport-size halves
     * <b><i>almost exactly</i></b>.<br>
     * (HALF was chosen to - perhaps? - avoid excessive Image degradation when zooming)<br>
     * For ZOOM_N = 9 the factor value is approximately 93%
     */
    public static final  double ZOOM_IN_SCALE = Math.pow(HALF, 1.0d / ZOOM_N);

    private static final String imageTypes = "jpg,tif,tiff,jpeg,png,psd,cr2,nef,raf,dng,x3f,heic";
    private static final String videoTypes = "mp4,mov";

    private final Consumer<?> EMPTY_CONSUMER = s -> {};

    public static boolean isSupportedExtension(Path path) {
        return SUPPORTED_EXT.matcher(path.getFileName().toString()).matches();
    }

    @SuppressWarnings("unchecked")
    public static <T> Consumer<T> emptyConsumer() {
        return (Consumer<T>) EMPTY_CONSUMER;
    }

    public static int split(int size) {
        // minus one core to get one free thread to dispatch event.
        return Math.max(1, size / (Constant.NB_CORE - 1));
    }
}
