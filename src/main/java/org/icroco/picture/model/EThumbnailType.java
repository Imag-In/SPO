package org.icroco.picture.model;

import lombok.Getter;

@Getter
public enum EThumbnailType {
    ABSENT('A'),
    EXTRACTED('E'),
    GENERATED('G');

    private final char code;

    EThumbnailType(char code) {
        this.code = code;
    }

    public static EThumbnailType fromCode(char code) {
        return switch (code) {
            case 'G' -> GENERATED;
            case 'E' -> EXTRACTED;
            default -> ABSENT;
        };
    }
}
