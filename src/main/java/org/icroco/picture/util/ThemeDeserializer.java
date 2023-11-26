package org.icroco.picture.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.icroco.picture.views.theme.SamplerTheme;
import org.icroco.picture.views.theme.ThemeRepository;

import java.io.IOException;

public class ThemeDeserializer extends StdDeserializer<SamplerTheme> {
    private final ThemeRepository themeRepository;

    public ThemeDeserializer(ThemeRepository themeRepository) {
        this(null, themeRepository);
    }

    @Override
    public SamplerTheme deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        if (jsonParser.hasToken(JsonToken.VALUE_STRING)) {
            var text = jsonParser.getText();
            return themeRepository.getAll()
                                  .stream()
                                  .filter(t -> t.getName().equals(text))
                                  .findFirst()
                                  .orElseGet(themeRepository::getDefault);
        }
        throw new JsonParseException("Cannot read theme attribute (String)");
    }

    public ThemeDeserializer(Class<SamplerTheme> t, ThemeRepository themeRepository) {
        super(t);
        this.themeRepository = themeRepository;
    }

}
