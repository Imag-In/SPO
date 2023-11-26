package org.icroco.picture.model;

import lombok.Getter;

@Getter
public enum EKeepOrThrow {
    KEEP((short) 1),
    UNKNOW((short) 0),
    THROW((short) -1);

    private final short code;

    EKeepOrThrow(short code) {
        this.code = code;
    }

    public static EKeepOrThrow fromCode(Short code) {
        return switch (code) {
            case 1 -> KEEP;
            case -1 -> THROW;
            default -> UNKNOW;
        };
    }
}
