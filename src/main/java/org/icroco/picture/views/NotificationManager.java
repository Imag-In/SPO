package org.icroco.picture.views;

import atlantafx.base.controls.Notification;
import atlantafx.base.theme.Styles;
import com.dlsc.gemsfx.infocenter.InfoCenterPane;
import com.dlsc.gemsfx.infocenter.NotificationAction;
import com.dlsc.gemsfx.infocenter.NotificationGroup;
import com.dlsc.gemsfx.infocenter.NotificationView;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.Duration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.event.*;
import org.icroco.picture.model.MediaFile;
import org.icroco.picture.views.task.TaskService;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2OutlinedAL;
import org.kordamp.ikonli.material2.Material2OutlinedMZ;
import org.kordamp.ikonli.materialdesign2.MaterialDesignI;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.time.ZonedDateTime;
import java.util.Optional;

import static java.time.Duration.ofSeconds;

@Service
@Slf4j
public class NotificationManager {
    private final TaskService                                                         taskService;
    private final InfoCenterPane                                                      infoCenterPane;
    //    private final VBox           notificationBar = new VBox();
    private final NotificationGroup<MediaFile, MediaFileNotification>                 imgGroup     = new NotificationGroup<>("Images");// I18N:
    private final NotificationGroup<UsbStorageDeviceEvent, NotificationUsb>           devicesGroup = new NotificationGroup<>("Devices");// I18N:
    private final NotificationGroup<NotificationEvent, NotificationEventNotification> defaultGroup = new NotificationGroup<>("General"); // I18N:

    public NotificationManager(TaskService taskService, MainView mainView) {
        this.taskService = taskService;
        infoCenterPane = mainView.getInfoCenterPane();

        infoCenterPane.getInfoCenterView().getGroups().addAll(defaultGroup, imgGroup, devicesGroup);
        defaultGroup.setSortOrder(0);
        defaultGroup.setViewFactory(errorViewFactory());

        imgGroup.setSortOrder(1);
        imgGroup.setViewFactory(mfBotifFactory());

        devicesGroup.setSortOrder(2);
        devicesGroup.setViewFactory(deviceViewFactory());

        infoCenterPane.setAutoHideDuration(Duration.seconds(5));
        infoCenterPane.getInfoCenterView().transparentProperty().set(false);


        infoCenterPane.getInfoCenterView().getUnmodifiableNotifications()
                      .addListener((ListChangeListener.Change<?> _) -> {
                                       int size = infoCenterPane.getInfoCenterView().getUnmodifiableNotifications().size();
                                       taskService.sendEvent(NotificationSizeEvent.builder().size(size).source(this).build());
                                   }
                      );

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
            view.getGraphic().setId("notifcationGroupIcon");

            return view;
        };
    }

    private @NonNull Callback<NotificationUsb, NotificationView<UsbStorageDeviceEvent, NotificationUsb>> deviceViewFactory() {
        return n -> {
            var view = new NotificationView<>(n);
            FontIcon graphic = new FontIcon(Material2OutlinedMZ.USB);
            graphic.setId("notifcationGroupIcon");
            var vbox = new VBox(graphic);
            vbox.setAlignment(Pos.CENTER);
            view.setGraphic(graphic);
            return view;
        };
    }

    private @NonNull Callback<MediaFileNotification, NotificationView<MediaFile, MediaFileNotification>> mfBotifFactory() {
        return n -> {
            var view = new NotificationView<>(n);
            view.getGraphic().setId("notifcationGroupIcon");

            return view;
        };
    }

    /////////////////////////////////////////////
    //// Event Listeners (Fx Thread guaranted)
    /////////////////////////////////////////////
    @FxEventListener
    public void listenEvent(NotificationEvent event) {
        // TODO: Historize latest alerts into DB.
        log.debug("Notification: {}", event);

        defaultGroup.getNotifications().add(new NotificationEventNotification(event));
    }

    @FxEventListener
    public void listenEvent(ShowViewEvent event) {
        if (event.getViewId().equals(ViewConfiguration.V_NOTIFICATION) && event.getEventType() == ShowViewEvent.EventType.SHOW) {
            infoCenterPane.setShowInfoCenter(!infoCenterPane.isShowInfoCenter());
        }
    }

    @FxEventListener
    public void listenEvent(UsbStorageDeviceEvent event) {
        NotificationUsb e = new NotificationUsb(event);
        devicesGroup.getNotifications().add(e);
        e.setExpanded(true);
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

    @Getter
    public static class AbstractNotification<N> extends com.dlsc.gemsfx.infocenter.Notification<N> {
        private Optional<java.time.Duration> autoCloseAfter;

        public AbstractNotification(String title, String summary, ZonedDateTime dateTime, java.time.Duration autoCloseAfter) {
            super(title, summary, dateTime);
            setAutoCloseAfter(autoCloseAfter);
        }

        public AbstractNotification(String title, String summary, ZonedDateTime dateTime) {
            this(title, summary, dateTime, null);
        }


        public AbstractNotification(String title, String summary) {
            this(title, summary, ZonedDateTime.now(), null);
        }

        public AbstractNotification(String title, String summary, java.time.Duration autoCloseAfter) {
            this(title, summary, ZonedDateTime.now(), null);
        }

        final void setAutoCloseAfter(@Nullable java.time.Duration duration) {
            this.autoCloseAfter = Optional.ofNullable(duration);
            this.autoCloseAfter.ifPresent(d -> {
                Thread.ofVirtual().name("AutoCloseNotif").start(() -> {
                    try {
                        Thread.sleep(d);
                        Platform.runLater(AbstractNotification.this::remove);
                    } catch (Throwable e) {
                        log.error("Cannot autoclose notification: {}", getTitle(), e);
                    }
                });
            });
        }
    }

    private static class NotificationEventNotification extends AbstractNotification<NotificationEvent> {
        public NotificationEventNotification(NotificationEvent event) {
            super(event.getType().toString(), event.getMessage(), event.getDateTime());
            setUserObject(event);
            setOnClick(_ -> OnClickBehaviour.NONE);
            if (event.getType() == NotificationEvent.NotificationType.INFO && event.getTimeoutInSeconds() > 0) {
                setAutoCloseAfter(java.time.Duration.ofSeconds(event.getTimeoutInSeconds()));
            }
        }
    }

    private class NotificationUsb extends AbstractNotification<UsbStorageDeviceEvent> {
        public NotificationUsb(UsbStorageDeviceEvent event) {
            super(STR."USB Storage detected: '\{event.getDeviceName()}'",
                  STR."Mount directory: \{event.getRootDirectory()}",
                  event.getDateTime(),
                  ofSeconds(20)); // I18N:
            setOnClick(_ -> OnClickBehaviour.NONE);
            if (event.getType() == UsbStorageDeviceEvent.EventType.CONNECTED) {
                var openUsb = new NotificationAction<>("Import", (_) -> { // I18N:
                    var dcim = event.getRootDirectory().resolve("DCIM");
                    taskService.sendEvent(ImportDirectoryEvent.builder()
                                                              .rootDirectory(Files.exists(dcim) ? dcim : event.getRootDirectory())
                                                              .source(this)
                                                              .build());
                    return OnClickBehaviour.HIDE_AND_REMOVE;
                });

                getActions().add(openUsb);
            }

        }
    }

    private static class MediaFileNotification extends AbstractNotification<MediaFile> {
        public MediaFileNotification(MediaFile mediaFile) {
            super("Image", mediaFile.getFullPath().toString());
            setUserObject(mediaFile);
            setOnClick(_ -> OnClickBehaviour.REMOVE);
        }
    }
}
