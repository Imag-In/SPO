package org.icroco.picture.views.pref;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import jakarta.annotation.PreDestroy;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Slf4j

public class UserPreferenceService {
    private static final Path FILENAME = Path.of(System.getProperty("icroco.picture.home",
                                                                    System.getProperty("user.home")
                                                                    + File.separatorChar
                                                                    + ".icroco"
                                                                    + File.separatorChar
                                                                    + "configuration.yml"));

    private final ObjectMapper   mapper;
    @Getter
    private       UserPreference userPreference = new UserPreference();

    @Getter
    private final BooleanProperty notYetImplemented = new SimpleBooleanProperty(true);

    public UserPreferenceService() {
        this.mapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER))
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);

        readConf(FILENAME);
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
        Files.createDirectories(FILENAME.getParent());

        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(FILENAME.toFile(), false))) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(out, getUserPreference());
            out.flush();
            log.info("Configuration saved into: {}", FILENAME);
        } catch (IOException ex) {
            log.error("Failed to serialized configuration: {}", FILENAME, ex);
        }
    }
}
