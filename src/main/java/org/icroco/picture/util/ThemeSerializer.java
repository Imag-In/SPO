package org.icroco.picture.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.icroco.picture.views.theme.SamplerTheme;

import java.io.IOException;

public class ThemeSerializer extends StdSerializer<SamplerTheme> {

    public ThemeSerializer() {
        this(null);
    }

    public ThemeSerializer(Class<SamplerTheme> t) {
        super(t);
    }

    @Override
    public void serialize(SamplerTheme theme, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeString(theme.getName());
    }
}
