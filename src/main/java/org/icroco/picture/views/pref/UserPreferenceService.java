package org.icroco.picture.views.pref;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import jakarta.annotation.PreDestroy;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.icroco.picture.util.Constant;
import org.icroco.picture.util.ThemeDeserializer;
import org.icroco.picture.util.ThemeSerializer;
import org.icroco.picture.views.theme.SamplerTheme;
import org.icroco.picture.views.theme.ThemeRepository;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

//@Component
@Slf4j
public class UserPreferenceService {

    private static final Path OLD_FILENAME = Path.of(System.getProperty("imagin.spo.home",
                                                                        STR."\{System.getProperty("user.home")}\{File.separatorChar}.icroco\{File.separatorChar}configuration.yml"));
    static final Path PREF_FILENAME = Constant.SPO_HOMEDIR.resolve("configuration.yml");

    private final ObjectMapper mapper;
    @Getter
    private final BooleanProperty notYetImplemented = new SimpleBooleanProperty(true);

    @Getter
    private UserPreference userPreference = new UserPreference();

    public UserPreferenceService(ThemeRepository themeRepository) {
        this.mapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER))
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(SamplerTheme.class, new ThemeSerializer());
        simpleModule.addDeserializer(SamplerTheme.class, new ThemeDeserializer(themeRepository));
        mapper.registerModule(simpleModule);
        migrateConf();
        readConf(PREF_FILENAME);
    }

    private void migrateConf() {
        if (Files.exists(OLD_FILENAME)) {
            if (Files.exists(PREF_FILENAME)) {
                log.warn("Cannot migrate: '{}', bacause '{}' already exist. Delete one or other", OLD_FILENAME, PREF_FILENAME);
            } else {
                try {
                    Files.createDirectories(PREF_FILENAME.getParent());
                    FileUtils.copyDirectory(OLD_FILENAME.getParent().toFile(), PREF_FILENAME.getParent().toFile());
                    log.info("Configuration and database migrated from: '{}' to: '{}'",
                             OLD_FILENAME.getParent(),
                             PREF_FILENAME.getParent());
                } catch (IOException e) {
                    log.error("Cannot move directory: '{}' into: '{}'", OLD_FILENAME, PREF_FILENAME, e);
                }
//                FileUtils.deleteQuietly(OLD_FILENAME)
            }
        }
    }

    private void readConf(Path fileName) {
        log.info("Read configuration from: {}", fileName);
        if (Files.exists(fileName) && Files.isRegularFile(fileName)) {
            try {
                userPreference = mapper.readValue(fileName.toFile(), UserPreference.class);
                //mapper.readerForUpdating(this).readValue(f);
            } catch (IOException e) {
                log.error("Failed to parse config file: {}, message: {}", fileName, e.getLocalizedMessage());
            }
        }
        if (userPreference == null) {
            userPreference = new UserPreference();
        }
    }

    @PreDestroy
    @SneakyThrows
    private void saveConfiguration() {
        Files.createDirectories(PREF_FILENAME.getParent());

        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(PREF_FILENAME.toFile(), false))) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(out, getUserPreference());
            out.flush();
            log.info("Configuration saved into: {}", PREF_FILENAME);
        } catch (IOException ex) {
            log.error("Failed to serialized configuration: {}", PREF_FILENAME, ex);
        }
    }
}
