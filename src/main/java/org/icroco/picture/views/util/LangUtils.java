package org.icroco.picture.views.util;


import org.springframework.lang.Nullable;
import org.threeten.extra.AmountFormats;

import java.time.Duration;
import java.util.Collection;
import java.util.Locale;
import java.util.stream.Stream;

public class LangUtils {

    public static final String EMPTY_STRING = "";

    public static boolean isBlank(@Nullable String value) {
        return value == null || value.isBlank();
    }

    public static boolean isNotBlank(@Nullable String value) {
        return !isBlank(value);
    }

    public static String wordBased(Duration duration) {
        return AmountFormats.wordBased(duration, Locale.getDefault());
    }


    public static <T> Stream<T> safeStream(Collection<T> collection) {
        return collection == null || collection.isEmpty()
               ? Stream.empty()
               : collection.stream();
    }
}
