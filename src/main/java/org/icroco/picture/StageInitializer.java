package org.icroco.picture;

import fr.brouillard.oss.cssfx.CSSFX;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.infra.github.GitHubClient;
import org.icroco.picture.util.Env;
import org.icroco.picture.util.Resources;
import org.icroco.picture.util.SceneReadyEvent;
import org.icroco.picture.util.StageReadyEvent;
import org.icroco.picture.views.FxEventListener;
import org.icroco.picture.views.MainView;
import org.icroco.picture.views.StageRepository;
import org.icroco.picture.views.compare.DiffWindow;
import org.icroco.picture.views.pref.UserPreference;
import org.icroco.picture.views.pref.UserPreferenceService;
import org.icroco.picture.views.task.TaskService;
import org.icroco.picture.views.theme.ThemeManager;
import org.scenicview.ScenicView;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class StageInitializer {

    private final ConfigurableApplicationContext context;
    private final UserPreferenceService userPref;
    private final GitHubClient          gitHubClient;
    private final ThemeManager          themeManager;
    private final StageRepository stageRepository;
    private final MainView                       mainView;
    private final Env                            env;
    private final DiffWindow      diffWindow;
    private final TaskService           taskService;

//    @EventListener
//    public void prepareEnv(ApplicationContextInitializedEvent event) {
//
//        ConfigurableEnvironment environment = event.getApplicationContext().getEnvironment();
//        Properties              props       = new Properties();
//        props.put("foo", "bar");
//        environment.getPropertySources().addFirst(new PropertiesPropertySource("DB", props));
//        log.info("DB?: {}", environment.getProperty("foo", "???"));
//    }

    @EventListener
    public void applicationStarted(ApplicationStartedEvent event) {
        ConfigurableEnvironment environment = event.getApplicationContext().getEnvironment();
        log.info("DB: {}/{}, className: {}",
                 environment.getProperty("SPO_DB_PATH", "??"),
                 environment.getProperty("SPO_DB_NAME", "??"),
                 environment.getClass().getName());

    }

    @FxEventListener
    public void onApplicationEvent(StageReadyEvent event) {
        Stage primaryStage = event.getStage();
        primaryStage.setOnCloseRequest(this::closeMainApplication);
        primaryStage.setTitle("Imag'In");

        var antialiasing = Platform.isSupported(ConditionalFeature.SCENE3D)
                           ? SceneAntialiasing.BALANCED
                           : SceneAntialiasing.DISABLED;
        var            scene = new Scene(mainView.getRootContent(), 1200, 800, false, antialiasing);
        KeyCombination kc    = new KeyCodeCombination(KeyCode.P, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_ANY, KeyCombination.SHIFT_ANY);
        scene.getAccelerators().put(kc, () -> {
            throw new IllegalStateException("This is a test");
        });
        primaryStage.setScene(scene);
        if (Boolean.getBoolean("SCENIC")) {
            ScenicView.show(scene);
            ScenicView.show(diffWindow.getStage().getScene());
        }
        if (env.isDev()) {
//            CSSFXLogger.setLogLevel(CSSFXLogger.LogLevel.DEBUG);
            CSSFX.start();
//            CSSFXLogger.setLoggerFactory((loggerName) -> (level, message, args) -> {
//                System.out.println(STR."\{loggerName}: original message: \{String.format(message, args)}");
//            });
            scene.focusOwnerProperty().addListener((_, _, newValue) -> log.debug("Focus onwer: {}", newValue));
        }
        themeManager.setTheme(Optional.ofNullable(userPref.getUserPreference().getMainWindow().getTheme())
                                      .orElseGet(themeManager::getDefaultTheme));
        stageRepository.addStage(primaryStage);

        restoreWindowDimension(scene, primaryStage);
        scene.getStylesheets().addAll(Resources.resolve("/styles/empty.css"), Resources.resolve("/styles/index.css"));

        context.publishEvent(new SceneReadyEvent(scene, primaryStage));

//            Platform.runLater(() -> applicationContext.publishEvent(new StageReadyEvent(primaryStage)));
//        primaryStage.getScene().getRoot().setOpacity(0);
        primaryStage.show();
//        Animations.fadeIn(primaryStage.getScene().getRoot(), Duration.millis(1000)).playFromStart();
    }

    private void restoreWindowDimension(Scene scene, Stage primaryStage) {
        userPref.getUserPreference().getMainWindow().restoreWindowDimension(primaryStage);
    }

    protected void closeMainApplication(final WindowEvent windowEvent) {
        if (windowEvent.getTarget() instanceof Stage stage) {
            saveWindow(stage, userPref.getUserPreference().getMainWindow());
            saveWindow(diffWindow.getStage(), userPref.getUserPreference().getDiffWindow());
            diffWindow.close();
        }
        if (Boolean.getBoolean("SCENIC")) {
            System.exit(0);
        }
    }

    private void saveWindow(Stage stage, UserPreference.Window windowSettings) {
        var window = stage.getScene().getWindow();
        var firstScreen = Screen.getScreensForRectangle(new Rectangle2D(window.getX(),
                                                                        window.getY(),
                                                                        window.getWidth(),
                                                                        window.getHeight()))
                                .stream()
                                .findFirst();

        ObservableList<Screen> screens = Screen.getScreens();
        windowSettings.setScreenIdx(-1);
        for (int i = 0; i < screens.size(); i++) {
            if (screens.get(i).equals(firstScreen.orElse(null))) {
                windowSettings.setScreenIdx(i);
                break;
            }
        }
        windowSettings.setMaximized(stage.isMaximized());
        windowSettings.setPosX(window.getX());
        windowSettings.setPosY(window.getY());
        windowSettings.setWidth(window.getWidth());
        windowSettings.setHeight(window.getHeight());
        windowSettings.setTheme(themeManager.getTheme());
    }
}
