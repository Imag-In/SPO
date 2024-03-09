package org.icroco.picture.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@UtilityClass
@Slf4j
public class Constant {

    // We save 1 thread for FxPlatform (UX)
    public static final int NB_CORE = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);

    public static final long MAX_MEM = Runtime.getRuntime().maxMemory();

    /**
     * get the supported Locales.
     *
     * @return List of Locale objects.
     */
    public static List<Locale> getSupportedLocales() {
        return new ArrayList<>(Arrays.asList(Locale.ENGLISH, Locale.FRENCH));
    }

    /**
     * get the default locale. This is the systems default if contained in the supported locales, english otherwise.
     *
     * @return the defaukt local.
     */
    public static Locale getDefaultLocale() {
        Locale sysDefault = Locale.getDefault();
        return Constant.getSupportedLocales().contains(sysDefault) ? sysDefault : Locale.ENGLISH;
    }

    static {
        log.info("Nb Core: '{}',  Max mem: '{}'", NB_CORE, FileUtils.byteCountToDisplaySize(MAX_MEM));
    }
//    {
//        Runtime env = Runtime.getRuntime();
//
//        System.out.println("Max Heap Size = maxMemory() = " + env.maxMemory()); //max heap size from -Xmx, i.e. is constant during runtime
//        System.out.println("Current Heap Size = totalMemory() = " + env.totalMemory()); //currently assigned  heap
//        System.out.println("Available in Current Heap = freeMemory() = "
//                           + env.freeMemory()); //current heap will extend if no more freeMemory to a maximum of maxMemory
//        System.out.println("Currently Used Heap = " + (env.totalMemory() - env.freeMemory()));
//        System.out.println("Unassigned Heap = " + (env.maxMemory() - env.totalMemory()));
//        System.out.println("Currently Totally Available Heap Space = " + ((env.maxMemory() - env.totalMemory())
//                                                                          + env.freeMemory())); //available=unassigned + free
//    }

    public Pattern SUPPORTED_EXT = Pattern.compile(".*\\.(png|jpg|jpeg)$", Pattern.CASE_INSENSITIVE);
//    public              Pattern SUPPORTED_EXT = Pattern.compile(".*\\.(png|jpg|jpeg|tiff|tif)$", Pattern.CASE_INSENSITIVE);

    public static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
    public static DateTimeFormatter DATE_FORMATTER      = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

    public static final String SPO_DIR = System.getProperty("imagin.spo.home", STR.".imagin\{File.separatorChar}spo");

    public static final Path SPO_HOMEDIR = Path.of(System.getProperty("imagin.spo.home",
                                                                      STR."\{System.getProperty("user.home")}\{File.separatorChar}\{SPO_DIR}"))
                                               .normalize()
                                               .toAbsolutePath();

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

    private final Consumer<?> EMPTY_CONSUMER = s -> {
    };

    private static boolean isSupportedExtension(Path path) {
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
