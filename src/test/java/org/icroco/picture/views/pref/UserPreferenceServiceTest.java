package org.icroco.picture.views.pref;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.icroco.picture.util.Env;
import org.icroco.picture.util.ThemeDeserializer;
import org.icroco.picture.util.ThemeSerializer;
import org.icroco.picture.views.theme.SamplerTheme;
import org.icroco.picture.views.theme.ThemeRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.data.geo.GeoModule;

import java.nio.file.Path;


@Slf4j
class UserPreferenceServiceTest {

    @Test
    @Disabled
    void chekk_conf_file() {
        log.info("Config: {}", UserPreferenceService.PREF_FILENAME);
        Assertions.assertThat(UserPreferenceService.PREF_FILENAME).endsWith(Path.of("configuration.yml"));
    }

    @Test
    void should_be_serailizable() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER))
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(SamplerTheme.class, new ThemeSerializer());
        simpleModule.addDeserializer(SamplerTheme.class, new ThemeDeserializer(new ThemeRepository(new Env(null))));
        mapper.registerModule(new GeoModule());
        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(simpleModule);

        UserPreference pref = new UserPreference();

        log.info(mapper.writeValueAsString(pref));
    }
}