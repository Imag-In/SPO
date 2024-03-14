package org.icroco.picture.util;


import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.threeten.extra.AmountFormats;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.SequencedCollection;
import java.util.stream.Stream;

@Slf4j
@UtilityClass
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

    public static <T> SequencedCollection<T> safeCollection(SequencedCollection<T> collection) {
        return collection == null || collection.isEmpty()
               ? Collections.emptyList()
               : collection;
    }

    public static <T> Collection<T> safeCollection(Collection<T> collection) {
        return collection == null || collection.isEmpty()
               ? Collections.emptyList()
               : collection;
    }

    public static boolean isNullOrEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

}
