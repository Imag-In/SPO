package org.icroco.picture.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.icroco.picture.model.EThumbnailType;

@Converter
public class ThumbnailTypeConverter implements AttributeConverter<EThumbnailType, Character> {
    @Override
    public Character convertToDatabaseColumn(EThumbnailType type) {
        if (type == null) {
            return null;
        }
        return type.getCode();
    }

    @Override
    public EThumbnailType convertToEntityAttribute(Character code) {
        if (code == null) {
            return null;
        }

        return EThumbnailType.fromCode(code);
    }
}
