package org.icroco.picture.ui.util;

import lombok.experimental.UtilityClass;

import java.util.Arrays;

@UtilityClass
public class Error {
    public static Throwable findOwnedException(Throwable throwable) {
        if (Arrays.stream(throwable.getStackTrace())
                  .anyMatch(stackTraceElement -> stackTraceElement.getClass()
                                                                  .getPackageName()
                                                                  .startsWith("org.icroco"))) {
            return throwable;
        }
        if (throwable.getCause() == null) {
            return throwable;
        }

        return findOwnedException(throwable.getCause());
    }
}
