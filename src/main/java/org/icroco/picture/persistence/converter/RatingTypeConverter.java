package org.icroco.picture.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.icroco.picture.model.ERating;

@Converter
public class RatingTypeConverter implements AttributeConverter<ERating, Short> {
    @Override
    public Short convertToDatabaseColumn(ERating type) {
        if (type == null) {
            return 0;
        }
        return type.getCode();
    }

    @Override
    public ERating convertToEntityAttribute(Short code) {
        if (code == null) {
            return ERating.ABSENT;
        }

        return ERating.fromCode(code);
    }
}
