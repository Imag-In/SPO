package org.icroco.picture.views.util;


import org.springframework.lang.Nullable;

public class LangUtils {

    public static boolean isBlank(@Nullable String value) {
        return value == null || value.isBlank();
    }

    public static boolean isNotBlank(@Nullable String value) {
        return !isBlank(value);
    }
}
