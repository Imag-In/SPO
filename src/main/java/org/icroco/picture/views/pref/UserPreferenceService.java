package org.icroco.picture.views.pref;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import jakarta.annotation.PreDestroy;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@Component
@Slf4j

public class UserPreferenceService {
    private static final File FILENAME = new File(System.getProperty("icroco.picture.home",
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
        ;

        readConf(FILENAME);
    }

//    @PostConstruct
//    private void initFromFile() {
//        initFromFile();
//    }

    private void readConf(File fileName) {
        log.info("Read configuration from: {}", fileName);
        if (fileName.exists()) {
            try {
                userPreference = mapper.readValue(fileName, UserPreference.class);
                //mapper.readerForUpdating(this).readValue(f);
            }
            catch (IOException e) {
                log.error("Failed to parse config file: {}, message: {}", fileName, e.getLocalizedMessage());
            }
        }
        if (userPreference == null) {
            userPreference = new UserPreference();
        }
    }

    @PreDestroy
    private void saveConfiguration() {
        FILENAME.getParentFile().mkdirs();

        try (FileOutputStream out = new FileOutputStream(FILENAME, false)) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(out, getUserPreference());
            log.info("Configuration saved into: {}", FILENAME);
        }
        catch (IOException ex) {
            log.error("Failed to serialized configuration: {}", FILENAME, ex);
        }
    }
}
