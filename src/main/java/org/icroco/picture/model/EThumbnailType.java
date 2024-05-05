package org.icroco.picture.model;

import lombok.Getter;

@Getter
public enum EThumbnailType {
    ABSENT('A'),
    EXTRACTED('E'),
    GENERATED('G'),
    EXTRACTED_ERROR('Y'),
    GENERATED_ERROR('Z');

    private final char code;

    EThumbnailType(char code) {
        this.code = code;
    }

    public static EThumbnailType fromCode(char code) {
        return switch (code) {
            case 'G' -> GENERATED;
            case 'E' -> EXTRACTED;
            case 'Y' -> EXTRACTED_ERROR;
            case 'Z' -> GENERATED_ERROR;
            default -> ABSENT;
        };
    }

    public boolean isAbsent() {
        return this == ABSENT;
    }

    public boolean isAbsentOrError() {
        return this == ABSENT || this == GENERATED_ERROR || this == EXTRACTED_ERROR;
    }

    public boolean isAbsentOrExtractedError() {
        return this == ABSENT || this == EXTRACTED_ERROR;
    }

    public boolean isNotGenerated() {
        return this != GENERATED && this != GENERATED_ERROR;
    }

    public boolean isNotExtracted() {
        return this != EXTRACTED;
    }
}
