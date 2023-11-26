package org.icroco.picture;

import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.util.Resources;
import org.icroco.picture.util.SceneReadyEvent;
import org.icroco.picture.util.StageReadyEvent;
import org.icroco.picture.views.FxEventListener;
import org.icroco.picture.views.MainView;
import org.icroco.picture.views.pref.UserPreferenceService;
import org.icroco.picture.views.theme.ThemeManager;
import org.icroco.picture.views.util.Nodes;
import org.scenicview.ScenicView;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class StageInitializer {

    private final ConfigurableApplicationContext context;
    private final UserPreferenceService          userPref;
    private final ThemeManager                   themeManager;

    private final MainView mainView;

    @FxEventListener
    public void onApplicationEvent(StageReadyEvent event) {
        Stage primaryStage = event.getStage();
        primaryStage.setOnCloseRequest(this::closeRequest);
        primaryStage.setTitle("Imag'In");
//        Application.setUserAgentStylesheet(new NordDark().getUserAgentStylesheet());
//        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());


        var antialiasing = Platform.isSupported(ConditionalFeature.SCENE3D)
                           ? SceneAntialiasing.BALANCED
                           : SceneAntialiasing.DISABLED;
        var scene = new Scene(mainView.getRootContent(), 1200, 800, false, antialiasing);
        themeManager.setScene(scene);
        themeManager.setTheme(Optional.ofNullable(userPref.getUserPreference().getMainWindow().getTheme())
                                      .orElseGet(themeManager::getDefaultTheme));

        if (userPref.getUserPreference().getMainWindow().exist()) {
            Nodes.setStageSizeAndPos(primaryStage,
                                     userPref.getUserPreference().getMainWindow().getPosX(),
                                     userPref.getUserPreference().getMainWindow().getPosY(),
                                     userPref.getUserPreference().getMainWindow().getWidth(),
                                     userPref.getUserPreference().getMainWindow().getHeight());
        } else {
            primaryStage.centerOnScreen();
            primaryStage.setWidth(Screen.getPrimary().getBounds().getWidth() - 100);
            primaryStage.setHeight(Screen.getPrimary().getBounds().getHeight() - 50);
        }
        if (userPref.getUserPreference().getMainWindow().isMaximized()) {
            primaryStage.setMaximized(true);
        }
        primaryStage.setScene(scene);

        scene.getStylesheets().addAll(Resources.resolve("/styles/index.css"));
        if (Boolean.getBoolean("SCENIC")) {
            ScenicView.show(scene);
        }

        context.publishEvent(new SceneReadyEvent(scene, primaryStage));

//            Platform.runLater(() -> applicationContext.publishEvent(new StageReadyEvent(primaryStage)));
//        primaryStage.getScene().getRoot().setOpacity(0);
        primaryStage.show();
//        Animations.fadeIn(primaryStage.getScene().getRoot(), Duration.millis(1000)).playFromStart();
    }

    protected void closeRequest(final WindowEvent windowEvent) {
        if (windowEvent.getTarget() instanceof Stage stage) {
            log.info("Save stage: {}.{}", stage.getX(), stage.getY());
            userPref.getUserPreference().getMainWindow().setMaximized(stage.isMaximized());
            userPref.getUserPreference().getMainWindow().setPosX(stage.getX());
            userPref.getUserPreference().getMainWindow().setPosY(stage.getY());
            userPref.getUserPreference().getMainWindow().setWidth(stage.getWidth());
            userPref.getUserPreference().getMainWindow().setHeight(stage.getHeight());
            userPref.getUserPreference().getMainWindow().setTheme(themeManager.getTheme());
        }
    }
}
