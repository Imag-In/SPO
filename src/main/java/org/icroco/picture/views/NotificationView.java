package org.icroco.picture.views;

import atlantafx.base.controls.Notification;
import atlantafx.base.theme.Styles;
import atlantafx.base.util.Animations;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.event.ImportDirectoryEvent;
import org.icroco.picture.event.NotificationEvent;
import org.icroco.picture.event.UsbStorageDeviceEvent;
import org.icroco.picture.views.task.TaskService;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2OutlinedAL;
import org.kordamp.ikonli.material2.Material2OutlinedMZ;
import org.kordamp.ikonli.materialdesign2.MaterialDesignI;
import org.springframework.stereotype.Service;

import java.nio.file.Files;

@Service
@Slf4j
public class NotificationView {
    private final TaskService taskService;
    private final StackPane   notificationPane;
    private final VBox notificationBar = new VBox();

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
                                                     """.formatted(event.getDeviceName())
            );
            customizeIcon(NotificationEvent.NotificationType.QUESTION, msg, new FontIcon(FontAwesomeSolid.SD_CARD));

            msg.getStyleClass().addAll(Styles.ACCENT, Styles.ELEVATED_1);
            msg.setPrefHeight(Region.USE_PREF_SIZE);
            msg.setMaxHeight(Region.USE_PREF_SIZE);

            var yesBtn = getButton(event, msg);
            msg.setPrimaryActions(yesBtn);

            addAlert(msg, Duration.seconds(15));

        }
    }

    private Button getButton(UsbStorageDeviceEvent event, Notification msg) {
        var yesBtn = new Button("Yes");
        yesBtn.setOnMouseClicked(_ -> {
            msg.setVisible(false);
            var dcim = event.getRootDirectory().resolve("DCIM");
            taskService.sendEvent(ImportDirectoryEvent.builder()
                                                      .rootDirectory(Files.exists(dcim) ? dcim : event.getRootDirectory())
                                                      .source(this)
                                                      .build());
        });
        return yesBtn;
    }

    private void addAlert(Notification msg, Duration showDuration) {
        if (!notificationBar.getChildren().contains(msg)) {
            if (notificationBar.getChildren().size() > 5) {
                notificationBar.getChildren().removeFirst();
            }
            HBox.setHgrow(msg, Priority.ALWAYS);
            notificationBar.getChildren().add(msg);
            msg.setOnClose(_ -> {
                var close = Animations.slideOutUp(msg, Duration.millis(250));
                close.setOnFinished(_ -> removeAlert(msg));
                close.playFromStart();
            });

            var in  = Animations.slideInDown(msg, Duration.millis(250));
            var out = Animations.slideOutUp(msg, Duration.millis(250));
            out.setOnFinished(f -> removeAlert(msg));
            var seqTransition = new SequentialTransition(in,
                                                         new PauseTransition(showDuration),
                                                         out);
            seqTransition.play();
        }
    }

    private void removeAlert(Notification msg) {
        notificationBar.getChildren().remove(msg);
//        alerts.remove(msg);
    }

    private void customizeIcon(NotificationEvent.NotificationType type, Notification msg, FontIcon icon) {
        msg.getStyleClass().add(Styles.ELEVATED_1);

        switch (type) {
            case QUESTION -> {
                msg.getStyleClass().add(Styles.ACCENT);
                if (icon == null) {
                    icon = new FontIcon(Material2OutlinedAL.HELP_OUTLINE);
                }
            }
            case SUCCESS -> {
                msg.getStyleClass().add(Styles.SUCCESS);
                if (icon == null) {
                    icon = new FontIcon(Material2OutlinedAL.CHECK_CIRCLE);
                }
            }
            case WARNING -> {
                msg.getStyleClass().add(Styles.WARNING);
                if (icon == null) {
                    msg.setGraphic(new FontIcon(Material2OutlinedMZ.WARNING));
                }
            }
            case ERROR -> {
                msg.getStyleClass().add(Styles.DANGER);
                if (icon == null) {
                    icon = new FontIcon(Material2OutlinedAL.ERROR_OUTLINE);
                }
            }
            case null, default -> {
                msg.getStyleClass().add(Styles.ACCENT);
                if (icon == null) {
                    icon = new FontIcon(MaterialDesignI.INFORMATION_OUTLINE);
                }
            }
        }
        msg.setGraphic(icon);
    }

    /////////////////////////////////////////////
    //// Event Listeners (Fx Thread guaranted)
    /////////////////////////////////////////////
    @FxEventListener
    public void listenEvent(NotificationEvent event) {
        // TODO: Historize latest alerts into DB.
        log.debug("Notification: {}", event);

        if (!notificationPane.getChildren().contains(notificationBar)) {
            notificationPane.getChildren().add(notificationBar);
            StackPane.setAlignment(notificationBar, Pos.TOP_RIGHT);
            notificationBar.setSpacing(10);
            notificationBar.setMaxHeight(0);
            notificationBar.setMaxWidth(400);
            notificationBar.setMinWidth(300);
            notificationBar.setPadding(new Insets(20, 20, 20, 20));
        }

        final var msg = new Notification(event.getMessage(), new FontIcon(Material2OutlinedAL.HELP_OUTLINE));
        customizeIcon(event.getType(), msg, null);
        msg.setPrefHeight(Region.USE_PREF_SIZE);
        msg.setMaxHeight(Region.USE_PREF_SIZE);

        addAlert(msg, Duration.seconds(event.getTimeoutInSeconds()));
    }

}
