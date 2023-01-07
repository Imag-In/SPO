package org.icroco.picture.ui;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import nu.pattern.OpenCV;
import org.controlsfx.dialog.ExceptionDialog;
import org.icroco.javafx.AbstractJavaFxApplication;
import org.icroco.javafx.ViewAutoConfiguration;
import org.icroco.picture.ui.pref.UserPreferenceService;
import org.icroco.picture.ui.util.Error;
import org.icroco.picture.ui.util.Nodes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@ImportAutoConfiguration(classes = ViewAutoConfiguration.class)
public class PictureApplication extends AbstractJavaFxApplication {
    // Application startup analysis: https://www.amitph.com/spring-boot-startup-monitoring/#applicationstartup_metrics_with_java_flight_recorder
    // Icon IRes: https://dlsc.com/2017/08/29/javafx-tip-27-hires-retina-icons/

    @Autowired
    UserPreferenceService userPref;

    @Override
    protected void preStart(final Stage primaryStage) {
//        OpenCV.loadShared();
        Nodes.setStageSizeAndPos(primaryStage,
                                 userPref.getUserPreference().getMainWindow().getPosX(),
                                 userPref.getUserPreference().getMainWindow().getPosY(),
                                 userPref.getUserPreference().getMainWindow().getWidth(),
                                 userPref.getUserPreference().getMainWindow().getHeight());
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
        Application.launch(PictureApplication.class, args);
    }
}
