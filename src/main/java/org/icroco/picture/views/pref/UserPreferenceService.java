package org.icroco.picture.views.pref;

import com.dlsc.preferencesfx.PreferencesFx;
import com.dlsc.preferencesfx.model.Category;
import com.dlsc.preferencesfx.model.Group;
import com.dlsc.preferencesfx.model.Setting;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import jakarta.annotation.PreDestroy;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.ImagInApp;
import org.icroco.picture.util.SceneReadyEvent;
import org.icroco.picture.util.ThemeDeserializer;
import org.icroco.picture.util.ThemeSerializer;
import org.icroco.picture.views.FxEventListener;
import org.icroco.picture.views.theme.SamplerTheme;
import org.icroco.picture.views.theme.ThemeManager;
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

    private final ThemeManager  themeManager;
    private       PreferencesFx preferencesFx;
    private       Stage         primaryStage;

    private final ObjectMapper   mapper;
    @Getter
    private       UserPreference userPreference = new UserPreference();

    @Getter
    private final BooleanProperty notYetImplemented = new SimpleBooleanProperty(true);

    public UserPreferenceService(ThemeManager themeManager) {
        this.themeManager = themeManager;
        this.mapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER))
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(SamplerTheme.class, new ThemeSerializer());
        simpleModule.addDeserializer(SamplerTheme.class, new ThemeDeserializer(themeManager.getRepository()));
        mapper.registerModule(simpleModule);
        readConf(FILENAME);
//        preferencesFx = createPreferences();


//        Preferences preferences = Preferences.userNodeForPackage(ImagInApp.class);
//        try {
//            for (String key : preferences.keys()) {
//                log.info("Found keys: {}: {}", key, preferences.get(key, "-"));
//            }
//        } catch (BackingStoreException e) {
//            throw new RuntimeException(e);
//        }
    }

    @FxEventListener
    void init(SceneReadyEvent event) {
        log.info("Create pref");
//        Platform
//        primaryStage = event.getStage();
//        themeManager.setScene(primaryStage.getScene());
//        preferencesFx = createPreferences();
    }

//    public void show() {
//        preferencesFx.show(true);
//    }

    private PreferencesFx createPreferences() {
        // Combobox, Single Selection, with ObservableList
        var
                resolutionItems =
                FXCollections.observableArrayList(themeManager.getRepository().getAll().stream().map(SamplerTheme::getName).toList());
        var     theme           = new SimpleObjectProperty<>(themeManager.getDefaultTheme().getName());
        theme.addListener((observable, oldValue, newValue) -> themeManager.getRepository().getAll()
                                                                          .stream()
                                                                          .filter(t -> t.getName().equals(newValue))
                                                                          .findFirst()
                                                                          .ifPresent(themeManager::setTheme));
        // asciidoctor Documentation - tag::setupPreferences[]
        return PreferencesFx.of(ImagInApp.class,
                                Category.of("General"
//                                            Group.of("Greeting",
//                                                     Setting.of("Welcome Text", welcomeText)
//                                            ),
//                                            Group.of("Display",
//                                                     Setting.of("Brightness", brightness),
//                                                     Setting.of("Night mode", nightMode)
//                                            ),
//                                            Group.of("Secrets",
//                                                     Setting.of("Password", somePasswordControl, somePassword)
//                                            )
                                ),
                                Category.of("Appearance and Behaviour")
                                        .expand()
                                        .subCategories(
                                                Category.of("Appearence",
                                                            Group.of(Setting.of("themes", resolutionItems, theme)
                                                            ).description("Screen Options")
                                                )
                                        )
                            )
                            .instantPersistent(true)
                            .persistApplicationState(true)
                            .saveSettings(true)
                            .debugHistoryMode(true)
                            .buttonsVisibility(true);
        // asciidoctor Documentation - end::setupPreferences[]
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
