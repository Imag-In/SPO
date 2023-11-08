package org.icroco.picture.splashscreen;

import atlantafx.base.theme.Styles;
import atlantafx.base.util.Animations;
import javafx.application.Preloader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SpoPreLoader extends Preloader {
    private final StackPane   root        = new StackPane();
    private final ProgressBar progressBar = new ProgressBar(0);
    private final Label       progressLbl = new Label("");

    private Stage stage;


    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;
        primaryStage.setTitle("Simple Photo Organizer loading ....");
        primaryStage.getIcons().add(new Image("/images/128px-GNOME_Photos_logo_2019.svg.png"));
        primaryStage.initStyle(StageStyle.UNDECORATED);

//        root.setStyle("-fx-background-radius: 15; -fx-background-color: transparent");
//        root.setStyle("-fx-background-radius: 15; -fx-background-color: rgba(229, 36, 66, 0.8)");

        ImageView imageView = new ImageView("/images/IMG_0373-EDIT.jpg");
//        imageView.setFitWidth(500);;
        imageView.setFitHeight(400);

        imageView.setPreserveRatio(true);

//        progressBar.setPrefWidth(450);
        progressBar.setOpacity(.9);
//        progressBar.setStyle("-fx-background-insets: 1, 1, 2, 1; -fx-padding: 0.20em");
        progressLbl.getStyleClass().add(Styles.SMALL);

//        progressLbl.setPrefWidth(450);
//        progressLbl.setTextFill(Color.WHITE);

//        StackPane.setMargin(imageView, new Insets(0, 20, 0, 20));
//        StackPane.setMargin(progressLbl, new Insets(200, 20, 0, 20));
//        StackPane.setMargin(progressBar, new Insets(250, 20, 0, 20));
        BorderPane borderPane = new BorderPane();
        borderPane.setPadding(new Insets(2));

        HBox hb = new HBox();
        progressBar.setMaxHeight(10);
        progressBar.setMinWidth(400);
        hb.getChildren().add(progressBar);
        HBox.setHgrow(progressBar, Priority.ALWAYS);
        borderPane.setBottom(hb);
        borderPane.setPrefHeight(400);
        root.getChildren().addAll(imageView, borderPane);

        var scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles/splash.css").toExternalForm());
        scene.setFill(Color.TRANSPARENT);
        primaryStage.setHeight(400);
//        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setScene(scene);
        primaryStage.setAlwaysOnTop(true);
        progressBar.setPrefWidth(.1);
        primaryStage.show();
//        ScenicView.show(scene);
    }

    @Override
    public void handleProgressNotification(ProgressNotification pn) {
        if (pn.getProgress() != 1.0) {
            progressBar.setProgress(pn.getProgress());
        }
    }

    @Override
    public void handleStateChangeNotification(StateChangeNotification scn) {
        log.info("type: {}", scn.getType());
        if (scn.getType() == StateChangeNotification.Type.BEFORE_START) {
            var fadeOut = Animations.fadeOut(root, Duration.millis(1000));
            fadeOut.setOnFinished(_ -> {
                stage.hide();
                stage.close();
            });
            fadeOut.playFromStart();
        }
    }

    @Override
    public boolean handleErrorNotification(ErrorNotification info) {
        log.error("Preloader error: {}", info);
        return super.handleErrorNotification(info);
    }

    @Override
    public void handleApplicationNotification(PreloaderNotification pn) {
        log.info("Application notification: {}", pn);
        if (pn instanceof LoaderProgressNotification lpn) {
            progressBar.progressProperty().setValue(lpn.getProgress());
            progressLbl.textProperty().setValue(lpn.getDetails());
        } else if (pn instanceof StateChangeNotification _) {
            var fadeOut = Animations.fadeOut(root, Duration.millis(1000));

            fadeOut.setOnFinished(_ -> stage.hide());
            fadeOut.playFromStart();
        }
    }
}
