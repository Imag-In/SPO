package org.icroco.picture.ui.util;

import lombok.experimental.UtilityClass;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@UtilityClass
public class Constant {

    public static       int     NB_CORE                       = Runtime.getRuntime().availableProcessors();
    public              Pattern SUPPORTED_EXT                 = Pattern.compile(".*\\.(png|jpg|jpeg)$", Pattern.CASE_INSENSITIVE);

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
}
