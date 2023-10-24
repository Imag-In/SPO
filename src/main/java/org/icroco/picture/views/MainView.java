package org.icroco.picture.views;

import atlantafx.base.controls.Notification;
import atlantafx.base.theme.Styles;
import atlantafx.base.util.Animations;
import jakarta.annotation.PostConstruct;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
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
import org.icroco.picture.event.ImportDirectoryEvent;
import org.icroco.picture.event.UsbStorageDeviceEvent;
import org.icroco.picture.util.Resources;
import org.icroco.picture.views.ext_import.ImportView;
import org.icroco.picture.views.navigation.NavigationView;
import org.icroco.picture.views.organize.OrganizeView;
import org.icroco.picture.views.status.StatusBarView;
import org.icroco.picture.views.task.TaskService;
import org.icroco.picture.views.util.FxView;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import org.scenicview.ScenicView;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.prefs.BackingStoreException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
@Component
public class MainView implements FxView<StackPane> {

    private final NavigationView       navView;
    private final StatusBarView        statusView;
    private final OrganizeView         organizeView;
    private final ImportView           importView;
    private final TaskService          taskService;
    @Qualifier(ViewConfiguration.CURRENT_VIEW)
    private final SimpleStringProperty currentView;

    private final StackPane root       = new StackPane();
    private final StackPane centerView = new StackPane();

    private final Map<String, FxView<?>> views = new HashMap<>(10);

    @PostConstruct
    protected void initializedOnce() {
        log.info("Primary screen: {}", Screen.getPrimary());
        Screen.getScreens().forEach(screen -> log.info("Screen: {}", screen));
        root.getStyleClass().add("v-main");
        var borderPane = new BorderPane();
        borderPane.setTop(navView.getRootContent());
        borderPane.setBottom(statusView.getRootContent());

        views.putAll(Stream.<FxView<?>>of(importView, organizeView)
                           .collect(Collectors.toMap(fxView -> fxView.getRootContent().getId(), Function.identity())));

        centerView.getChildren().addAll(views.values()
                                             .stream()
                                             .peek(fxView -> fxView.getRootContent().setVisible(false))
                                             .map(FxView::getRootContent).toList());
        currentView.addListener(this::currentViewListener);

        borderPane.setCenter(centerView);
        root.getChildren().add(borderPane);
        Platform.runLater(() -> currentViewListener(null, null, currentView.get()));
    }

    private void currentViewListener(ObservableValue<? extends String> observableValue, String oldView, String newView) {
        if (oldView != null) {
            Optional.ofNullable(views.get(oldView)).ifPresent(v -> v.getRootContent().setVisible(false));
        }
        Optional.ofNullable(views.get(newView)).ifPresent(v -> v.getRootContent().setVisible(true));
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
        if (event.getType() == UsbStorageDeviceEvent.EventType.CONNECTED) {
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
                msg.setVisible(false);
                var dcim = event.getRootDirectory().resolve("DCIM");
                taskService.sendEvent(ImportDirectoryEvent.builder()
                                                          .rootDirectory(Files.exists(dcim) ? dcim : event.getRootDirectory())
                                                          .source(this)
                                                          .build());
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
}
