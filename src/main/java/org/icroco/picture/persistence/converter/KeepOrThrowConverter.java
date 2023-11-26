package org.icroco.picture.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.icroco.picture.model.EKeepOrThrow;

@Converter
public class KeepOrThrowConverter implements AttributeConverter<EKeepOrThrow, Short> {
    @Override
    public Short convertToDatabaseColumn(EKeepOrThrow attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getCode();
    }

    @Override
    public EKeepOrThrow convertToEntityAttribute(Short code) {
        if (code == null) {
            return null;
        }

        return EKeepOrThrow.fromCode(code);
    }
}
