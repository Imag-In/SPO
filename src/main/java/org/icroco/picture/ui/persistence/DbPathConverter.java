package org.icroco.picture.ui.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.nio.file.Path;
import java.nio.file.Paths;

@Converter
public class DbPathConverter implements AttributeConverter<Path, String> {
    @Override
    public String convertToDatabaseColumn(Path path) {
        return path.toString();
    }

    @Override
    public Path convertToEntityAttribute(String s) {
        return Paths.get(s);
    }
}
