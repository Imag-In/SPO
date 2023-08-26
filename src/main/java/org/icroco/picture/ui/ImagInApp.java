package org.icroco.picture.ui;

import atlantafx.base.theme.NordDark;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.dialog.ExceptionDialog;
import org.icroco.javafx.AbstractJavaFxApplication;
import org.icroco.javafx.ViewAutoConfiguration;
import org.icroco.picture.ui.pref.UserPreferenceService;
import org.icroco.picture.ui.util.Error;
import org.icroco.picture.ui.util.ImageUtils;
import org.icroco.picture.ui.util.Nodes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;


@SpringBootApplication()
@EnableAsync
@ImportAutoConfiguration(classes = ViewAutoConfiguration.class)
@Slf4j
public class ImagInApp extends AbstractJavaFxApplication {
    public static final String IMAGES_128_PX_GNOME_PHOTOS_LOGO_2019_SVG_PNG = "/images/128px-GNOME_Photos_logo_2019.svg.png";
    // Application startup analysis: https://www.amitph.com/spring-boot-startup-monitoring/#applicationstartup_metrics_with_java_flight_recorder
    // Icon IRes: https://dlsc.com/2017/08/29/javafx-tip-27-hires-retina-icons/

    @Autowired
    UserPreferenceService userPref;

    AtomicBoolean scenicOnce = new AtomicBoolean(true);

    @Override
    protected void preStart(final Stage primaryStage) {
//        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Image'In");
//        System.setProperty("apple.laf.useScreenMenuBar", "true");
//        OpenCV.loadShared();
        ImageUtils.readImageIoCodec();
        var icon = getClass().getResourceAsStream(IMAGES_128_PX_GNOME_PHOTOS_LOGO_2019_SVG_PNG);
        primaryStage.getIcons().add(new Image(icon));
        if (Taskbar.isTaskbarSupported()) {
            var taskbar = Taskbar.getTaskbar();
            if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                final Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
                var           dockIcon       = defaultToolkit.getImage(getClass().getResource(IMAGES_128_PX_GNOME_PHOTOS_LOGO_2019_SVG_PNG));
                taskbar.setIconImage(dockIcon);
            }
        }
        primaryStage.setTitle("Image'In");
//        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/240px-GNOME_Photos_logo_2019.svg.png")));
        Nodes.setStageSizeAndPos(primaryStage,
                                 userPref.getUserPreference().getMainWindow().getPosX(),
                                 userPref.getUserPreference().getMainWindow().getPosY(),
                                 userPref.getUserPreference().getMainWindow().getWidth(),
                                 userPref.getUserPreference().getMainWindow().getHeight());
    }

    @Override
    protected void postStart(Stage primaryStage) {
//        Application.setUserAgentStylesheet(new NordLight().getUserAgentStylesheet());
        Application.setUserAgentStylesheet(new NordDark().getUserAgentStylesheet());
    }


    @Override
    protected void showErrorToUser(final Throwable throwable) {
        Throwable       t   = Error.findOwnedException(throwable);
        ExceptionDialog dlg = new ExceptionDialog(t);
        Nodes.showDialog(dlg);
    }

    @Override
    protected void closeRequest(final WindowEvent windowEvent) {
        if (windowEvent.getTarget() instanceof Stage stage) {
            userPref.getUserPreference().getMainWindow().setPosX(stage.getX());
            userPref.getUserPreference().getMainWindow().setPosY(stage.getY());
            userPref.getUserPreference().getMainWindow().setWidth(stage.getWidth());
            userPref.getUserPreference().getMainWindow().setHeight(stage.getHeight());
        }
    }


    public static void main(String[] args) {
        Application.launch(ImagInApp.class, args);
    }
}
