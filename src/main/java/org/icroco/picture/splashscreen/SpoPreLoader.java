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

import java.util.List;
import java.util.stream.Stream;

@Slf4j
public class SpoPreLoader extends Preloader {
    private final StackPane   root        = new StackPane();
    private final ProgressBar progressBar = new ProgressBar(0);
    private final Label       progressLbl = new Label("");
    private Stage stage;

    public static List<Image> getIcons() {
        return Stream.of("spo-1024x1024.png",
                         "spo-512x512.png",
                         "spo-256x256.png",
                         "spo-128x128.png",
                         "spo-64x64.png",
                         "spo-32x32.png",
                         "spo-16x16.png")
                     .map(s -> "/images/" + s)
                     .map(Image::new)
                     .toList();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;
        primaryStage.setTitle("Simple Photo Organizer loading ....");
        primaryStage.getIcons().addAll(getIcons());
        primaryStage.initStyle(StageStyle.TRANSPARENT);

//        ImageView imageView = new ImageView("/images/IMG_0373-EDIT.jpg");
        ImageView imageView = new ImageView("/images/PXL_20230115_220040106-EDIT.jpg");
        imageView.setFitHeight(400);

        imageView.setPreserveRatio(true);

        progressBar.setOpacity(.9);
        progressLbl.getStyleClass().add(Styles.SMALL);

        BorderPane borderPane = new BorderPane();
        borderPane.setPadding(new Insets(2));

        HBox hb = new HBox();
        progressBar.setMaxHeight(10);
        progressBar.setMinWidth(400);
        hb.getChildren().add(progressBar);
        HBox.setHgrow(progressBar, Priority.ALWAYS);
        borderPane.setBottom(hb);

        root.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0);" +
                "-fx-effect: dropshadow(gaussian, white, 50, 0, 0, 0);" +
                "-fx-background-insets: 0;"
        );
        imageView.setOpacity(1);
        root.getChildren().addAll(imageView, borderPane);
        var scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles/splash.css").toExternalForm());
        scene.setFill(Color.TRANSPARENT);
        primaryStage.setHeight(400);
        primaryStage.setScene(scene);
        primaryStage.setAlwaysOnTop(true);
        progressBar.setPrefWidth(.1);
        primaryStage.show();
    }

    @Override
    public void handleProgressNotification(ProgressNotification pn) {
        if (pn.getProgress() != 1.0) {
            progressBar.setProgress(pn.getProgress());
        }
    }

    @Override
    public void handleStateChangeNotification(StateChangeNotification scn) {
        log.debug("type: {}", scn.getType());
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
        log.debug("Application notification: {}", pn);
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
