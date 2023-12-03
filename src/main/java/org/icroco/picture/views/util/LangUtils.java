package org.icroco.picture.views.util;


import org.springframework.lang.Nullable;
import org.threeten.extra.AmountFormats;

import java.time.Duration;
import java.util.Locale;

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
}
