package org.icroco.picture.util;

public class SpoException extends RuntimeException {
    public SpoException(String message) {
        super(message);
    }

    public SpoException(String message, Throwable cause) {
        super(message, cause);
    }
}
