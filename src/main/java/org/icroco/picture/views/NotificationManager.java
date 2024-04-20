package org.icroco.picture.views;

import atlantafx.base.controls.Notification;
import atlantafx.base.theme.Styles;
import com.dlsc.gemsfx.infocenter.InfoCenterPane;
import com.dlsc.gemsfx.infocenter.NotificationGroup;
import com.dlsc.gemsfx.infocenter.NotificationView;
import javafx.scene.control.Button;
import javafx.util.Callback;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.event.ImportDirectoryEvent;
import org.icroco.picture.event.NotificationEvent;
import org.icroco.picture.event.ShowViewEvent;
import org.icroco.picture.event.UsbStorageDeviceEvent;
import org.icroco.picture.views.task.TaskService;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2OutlinedAL;
import org.kordamp.ikonli.material2.Material2OutlinedMZ;
import org.kordamp.ikonli.materialdesign2.MaterialDesignI;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.nio.file.Files;

@Service
@Slf4j
public class NotificationManager {
    private final TaskService                                                         taskService;
    private final InfoCenterPane                                                      infoCenterPane;
    //    private final VBox           notificationBar = new VBox();
    private final NotificationGroup<NotificationEvent, NotificationEventNotification> errorGroup   = new NotificationGroup<>("Error");
    private final NotificationGroup<UsbStorageDeviceEvent, NotificationUsb>           devicesGroup = new NotificationGroup<>("Devices");

    public NotificationManager(TaskService taskService, MainView mainView) {
        this.taskService = taskService;
        infoCenterPane = mainView.getInfoCenterPane();

        infoCenterPane.getInfoCenterView().getGroups().addAll(errorGroup, devicesGroup);
        errorGroup.setSortOrder(0);
        errorGroup.setViewFactory(errorViewFactory());

        devicesGroup.setSortOrder(1);
        devicesGroup.setViewFactory(deviceViewFactory());

        infoCenterPane.setAutoHideDuration(Duration.seconds(5));
        infoCenterPane.getInfoCenterView().transparentProperty().set(true);

//        notificationPane.getChildren().add(notificationBar);
//        StackPane.setAlignment(notificationBar, Pos.TOP_RIGHT);
//        notificationBar.setSpacing(10);
//        notificationBar.setMaxHeight(0);
//        notificationBar.setMaxWidth(400);
//        notificationBar.setMinWidth(400);
//        notificationBar.setPadding(new Insets(20, 20, 20, 20));
    }

    private @NonNull Callback<NotificationEventNotification, NotificationView<NotificationEvent, NotificationEventNotification>> errorViewFactory() {
        return n -> {
            var view = new NotificationView<>(n);
            updateErrorView(view);
            return view;
        };
    }

    private @NonNull Callback<NotificationUsb, NotificationView<UsbStorageDeviceEvent, NotificationUsb>> deviceViewFactory() {
        return n -> {
            var view = new NotificationView<>(n);
            view.setGraphic(new FontIcon(Material2OutlinedMZ.USB));
            return view;
        };
    }

    @FxEventListener
    public void listenEvent(ShowViewEvent event) {
        if (event.getViewId().equals(ViewConfiguration.V_NOTIFICATION) && event.getEventType() == ShowViewEvent.EventType.SHOW) {
            infoCenterPane.setShowInfoCenter(!infoCenterPane.isShowInfoCenter());
        }
    }

    @FxEventListener
    public void listenEvent(UsbStorageDeviceEvent event) {
        if (event.getType() == UsbStorageDeviceEvent.EventType.CONNECTED) {
            devicesGroup.getNotifications().add(new NotificationUsb(event));
//            final var msg = new Notification("""
//                                                     USB Storage detected: '%s'"
//                                                       Do you want to import files ?
//                                                     """.formatted(event.getDeviceName())
//            );
//            customizeIcon(NotificationEvent.NotificationType.QUESTION, msg, new FontIcon(FontAwesomeSolid.SD_CARD));
//
//            msg.getStyleClass().addAll(Styles.ACCENT, Styles.ELEVATED_1);
//            msg.setPrefHeight(Region.USE_PREF_SIZE);
//            msg.setMaxHeight(Region.USE_PREF_SIZE);
//
//            var yesBtn = getButton(event, msg);
//            msg.setPrimaryActions(yesBtn);
//
//            addAlert(msg, Duration.seconds(30));
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

//    private void addAlert(Notification msg, Duration showDuration) {
//        msg.setPrefWidth(300);
//        if (!notificationBar.getChildren().contains(msg)) {
//            if (notificationBar.getChildren().size() > 5) {
//                notificationBar.getChildren().removeFirst();
//            }
//            HBox.setHgrow(msg, Priority.ALWAYS);
//            notificationBar.getChildren().add(msg);
//
//            msg.setOnClose(_ -> {
//                var close = Animations.slideOutUp(msg, Duration.millis(250));
//                close.setOnFinished(_ -> removeAlert(msg));
//                close.playFromStart();
//            });
//
//            var in  = Animations.slideInDown(msg, Duration.millis(250));
//            var out = Animations.slideOutUp(msg, Duration.millis(250));
//            out.setOnFinished(f -> removeAlert(msg));
//            var seqTransition = new SequentialTransition(in,
//                                                         new PauseTransition(showDuration),
//                                                         out);
//            seqTransition.play();
//        }
//    }

//    private void removeAlert(Notification msg) {
//        notificationBar.getChildren().remove(msg);
////        alerts.remove(msg);
//    }


    private void updateErrorView(NotificationView<NotificationEvent, NotificationEventNotification> view) {
        if (view.getNotification().getUserObject() == null) {
            log.warn("Notification should have userObject: {}", view.getNotification());
            view.setGraphic(new FontIcon(Material2OutlinedMZ.WARNING));
            return;
        }
        FontIcon icon = switch (view.getNotification().getUserObject().getType()) {
            case QUESTION -> new FontIcon(Material2OutlinedAL.HELP_OUTLINE);
            case SUCCESS -> new FontIcon(Material2OutlinedAL.CHECK_CIRCLE);
            case WARNING -> new FontIcon(Material2OutlinedMZ.WARNING);
            case ERROR -> new FontIcon(Material2OutlinedAL.ERROR_OUTLINE);
            case null, default -> new FontIcon(MaterialDesignI.INFORMATION_OUTLINE);
        };
        view.setGraphic(icon);
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

        errorGroup.getNotifications().add(new NotificationEventNotification(event));

//        final var msg = new Notification(event.getMessage(), new FontIcon(Material2OutlinedAL.HELP_OUTLINE));
//        customizeIcon(event.getType(), msg, null);
//        msg.setPrefHeight(Region.USE_PREF_SIZE);
//        msg.setMaxHeight(Region.USE_PREF_SIZE);
//
//        addAlert(msg, Duration.seconds(event.getTimeoutInSeconds()));
    }

    private static class NotificationEventNotification extends com.dlsc.gemsfx.infocenter.Notification<NotificationEvent> {
        public NotificationEventNotification(NotificationEvent event) {
            super("Error", event.getMessage(), event.getDateTime());
            setOnClick(_ -> OnClickBehaviour.NONE);
        }
    }

    private static class NotificationUsb extends com.dlsc.gemsfx.infocenter.Notification<UsbStorageDeviceEvent> {
        public NotificationUsb(UsbStorageDeviceEvent event) {
            super(event.getDeviceName(), STR."\{event.getType().toString()}: \{event.getRootDirectory()}", event.getDateTime());
            setOnClick(_ -> OnClickBehaviour.REMOVE);
        }
    }
}
