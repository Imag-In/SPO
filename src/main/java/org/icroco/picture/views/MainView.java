package org.icroco.picture.views;

import atlantafx.base.controls.Notification;
import atlantafx.base.theme.Styles;
import atlantafx.base.util.Animations;
import jakarta.annotation.PostConstruct;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icroco.javafx.SceneReadyEvent;
import org.icroco.picture.event.UsbStorageDeviceEvent;
import org.icroco.picture.util.Resources;
import org.icroco.picture.views.navigation.NavigationView;
import org.icroco.picture.views.organize.OrganizeView;
import org.icroco.picture.views.status.StatusBarView;
import org.icroco.picture.views.util.FxView;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import org.scenicview.ScenicView;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.prefs.BackingStoreException;

@Slf4j
@RequiredArgsConstructor
@Component
public class MainView implements FxView<StackPane> {

    private final StackPane root = new StackPane();
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
        var borderPane = new BorderPane();
        borderPane.setTop(navView.getRootContent());
        borderPane.setBottom(statusView.getRootContent());
        borderPane.setCenter(organizeView.getRootContent());
        root.getChildren().add(borderPane);
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
    public StackPane getRootContent() {
        return root;
    }

    @FxEventListener
    public void listenEvent(UsbStorageDeviceEvent event) {
        final var msg = new Notification("""
                                                 USB Storage detected: '%s'"
                                                   Do you want to import files ?
                                                 """.formatted(event.getDeviceName()),
                                         new FontIcon(FontAwesomeSolid.SD_CARD));
        msg.getStyleClass().addAll(Styles.ACCENT, Styles.ELEVATED_1);
        msg.setPrefHeight(Region.USE_PREF_SIZE);
        msg.setMaxHeight(Region.USE_PREF_SIZE);

        var yesBtn = new Button("Yes");
        yesBtn.setOnMouseClicked(event1 -> {
            log.info("Yes inport into Image'in");
            msg.setVisible(true);
            // TODO: Send an event to Import view
        });

        msg.setPrimaryActions(yesBtn);

        StackPane.setAlignment(msg, Pos.TOP_RIGHT);
        StackPane.setMargin(msg, new Insets(10, 10, 0, 0));

        var out = Animations.slideOutUp(msg, Duration.millis(250));
        out.setOnFinished(f -> root.getChildren().remove(msg));

        var in = Animations.slideInDown(msg, Duration.millis(250));
        if (!root.getChildren().contains(msg)) {
            root.getChildren().add(msg);
        }

        var seqTransition = new SequentialTransition(in,
                                                     new PauseTransition(Duration.millis(5000)),
                                                     out);
        seqTransition.play();
    }
}
