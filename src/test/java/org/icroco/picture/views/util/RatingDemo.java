package org.icroco.picture.views.util;


import fr.brouillard.oss.cssfx.CSSFX;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class RatingDemo extends Application {
    @Override
    public void start(Stage stage) {
        Rating rating = new Rating();


        Scene scene = new Scene(rating);
        CSSFX.start();

        stage.setTitle("Rating");
        stage.setScene(scene);
        stage.sizeToScene();
        stage.centerOnScreen();
        stage.show();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}