package org.icroco.picture.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.nio.file.Path;

public class PathSerializer extends StdSerializer<Path> {

    public PathSerializer() {
        this(null);
    }

    public PathSerializer(Class<Path> t) {
        super(t);
    }

    @Override
    public void serialize(Path path, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
//        jgen.writeStartObject();
//        jgen.writeNumberField("id", value.id);
//        jgen.writeStringField("itemName", value.itemName);
//        jgen.writeNumberField("owner", value.owner.id);
//        jgen.writeEndObject();
        jsonGenerator.writeString(path.toString());
    }
}
