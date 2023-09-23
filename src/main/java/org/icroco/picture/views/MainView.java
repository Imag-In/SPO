package org.icroco.picture.views;

import jakarta.annotation.PostConstruct;
import javafx.scene.layout.BorderPane;
import javafx.stage.Screen;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icroco.javafx.SceneReadyEvent;
import org.icroco.picture.util.Resources;
import org.icroco.picture.views.navigation.NavigationView;
import org.icroco.picture.views.organize.OrganizeView;
import org.icroco.picture.views.status.StatusBarView;
import org.icroco.picture.views.util.FxView;
import org.scenicview.ScenicView;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.prefs.BackingStoreException;

@Slf4j
@RequiredArgsConstructor
@Component
public class MainView implements FxView<BorderPane> {

    private final BorderPane     root = new BorderPane();
    private final NavigationView navView;
    private final StatusBarView  statusView;
    private final OrganizeView   organizeView;

    @PostConstruct
    protected void initializedOnce() {
        log.info("Primary screen: {}", Screen.getPrimary());
        Screen.getScreens().forEach(screen -> {
            log.info("Screen: {}", screen);
        });
        root.getStyleClass().add("v-main");

        root.setTop(navView.getRootContent());
        root.setBottom(statusView.getRootContent());
        root.setCenter(organizeView.getRootContent());
    }

    @EventListener(SceneReadyEvent.class)
    public void sceneReady(SceneReadyEvent event) throws BackingStoreException {
        log.info("READY, source: {}", event.getSource());
        event.getScene().getStylesheets().addAll(Resources.resolve("/styles/index.css"));

//        Resources.getPreferences().put("FOO", "BAR");
//        Resources.getPreferences().flush();
//        Resources.printPreferences(Resources.getPreferences(), "");
        if (Boolean.getBoolean("SCENIC")) {
            ScenicView.show(event.getScene());
        }
//        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
//            log.info("call: {}", element);
//        }
    }

    @Override
    public BorderPane getRootContent() {
        return root;
    }
}
