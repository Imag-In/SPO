package org.icroco.picture.model.converter;

import javafx.util.StringConverter;
import org.icroco.picture.model.Keyword;

public class KeywordStringConverter extends StringConverter<Keyword> {
    @Override
    public String toString(Keyword object) {
        return object == null || object.name() == null ? "" : object.name();
    }

    @Override
    public Keyword fromString(String string) {
        return string == null ? null : new Keyword(null, string.strip());
    }
}
