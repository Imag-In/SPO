package org.icroco.picture.views;

import atlantafx.base.controls.Notification;
import atlantafx.base.theme.Styles;
import atlantafx.base.util.Animations;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.icroco.picture.event.ImportDirectoryEvent;
import org.icroco.picture.event.NotificationEvent;
import org.icroco.picture.event.UsbStorageDeviceEvent;
import org.icroco.picture.views.task.TaskService;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2OutlinedAL;
import org.springframework.stereotype.Service;

import java.nio.file.Files;

@Service
public class NotificationView {
    private final TaskService taskService;
    private final StackPane   notificationPane;

    public NotificationView(TaskService taskService, MainView mainView) {
        this.taskService = taskService;
        notificationPane = mainView.getRootContent();
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
            msg.setOnClose(e1 -> {
                var close = Animations.slideOutUp(msg, Duration.millis(250));
                close.setOnFinished(f -> notificationPane.getChildren().remove(msg));
                close.playFromStart();
            });

            var yesBtn = getButton(event, msg);

            msg.setPrimaryActions(yesBtn);

            StackPane.setAlignment(msg, Pos.TOP_RIGHT);
            StackPane.setMargin(msg, new Insets(10, 10, 0, 0));

            var out = Animations.slideOutUp(msg, Duration.millis(250));
            out.setOnFinished(f -> notificationPane.getChildren().remove(msg));

            var in = Animations.slideInDown(msg, Duration.millis(250));
            if (!notificationPane.getChildren().contains(msg)) {
                notificationPane.getChildren().add(msg);
            }

            var seqTransition = new SequentialTransition(in,
                                                         new PauseTransition(Duration.millis(15000)),
                                                         out);
            seqTransition.play();
        }
    }

    private Button getButton(UsbStorageDeviceEvent event, Notification msg) {
        var yesBtn = new Button("Yes");
        yesBtn.setOnMouseClicked(event1 -> {
            msg.setVisible(false);
            var dcim = event.getRootDirectory().resolve("DCIM");
            taskService.sendEvent(ImportDirectoryEvent.builder()
                                                      .rootDirectory(Files.exists(dcim) ? dcim : event.getRootDirectory())
                                                      .source(this)
                                                      .build());
        });
        return yesBtn;
    }

    @FxEventListener
    public void listenEvent(NotificationEvent event) {
        // TODO: Historize latest alerts int DB.
        final var msg = new Notification(event.getMessage(), new FontIcon(Material2OutlinedAL.HELP_OUTLINE));
        msg.getStyleClass().addAll(map(event.getType()), Styles.ELEVATED_1);
        msg.setPrefHeight(Region.USE_PREF_SIZE);
        msg.setMaxHeight(Region.USE_PREF_SIZE);

        StackPane.setAlignment(msg, Pos.TOP_RIGHT);
        StackPane.setMargin(msg, new Insets(10, 10, 0, 0));

        var in = Animations.slideInDown(msg, Duration.millis(250));
        if (!notificationPane.getChildren().contains(msg)) {
            notificationPane.getChildren().add(msg);
        }

        var out = Animations.slideOutUp(msg, Duration.millis(250));
        out.setOnFinished(f -> notificationPane.getChildren().remove(msg));

        var seqTransition = new SequentialTransition(in,
                                                     new PauseTransition(Duration.millis(5000)),
                                                     out);
        seqTransition.play();
    }

    private static String map(NotificationEvent.NotificationType type) {
        return switch (type) {
            case INFO -> Styles.ACCENT;
            case SUCCESS -> Styles.SUCCESS;
            case WARNING -> Styles.WARNING;
            case null, default -> Styles.DANGER;
        };
    }
}
