package org.icroco.picture.model;

import lombok.Getter;

@Getter
public enum EKeepOrThrow {
    KEEP((short) 1) {
        @Override
        public EKeepOrThrow next() {
            return THROW;
        }
    },
    UNKNOW((short) 0) {
        @Override
        public EKeepOrThrow next() {
            return KEEP;
        }
    },
    THROW((short) -1) {
        @Override
        public EKeepOrThrow next() {
            return UNKNOW;
        }
    };

    public abstract EKeepOrThrow next();
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
