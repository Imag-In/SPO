package org.icroco.picture.views.compare;

import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.event.ShowDiffEvent;
import org.icroco.picture.model.MediaFile;
import org.icroco.picture.views.FxEventListener;
import org.icroco.picture.views.StageRepository;
import org.icroco.picture.views.pref.UserPreferenceService;
import org.icroco.picture.views.task.TaskService;
import org.icroco.picture.views.util.FxPlatformExecutor;
import org.icroco.picture.views.util.MediaLoader;
import org.icroco.picture.views.util.widget.ZoomDragPane;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DiffWindow {
    private final TaskService taskService;
    private final UserPreferenceService userPrefSvc;
    private final StageRepository       stageRepository;
    private final MediaLoader mediaLoader;

    private final HBox         hBox  = new HBox();
    private final Scene        scene = new Scene(hBox);
    @Getter
    private       Stage        stage;
    private       ZoomDragPane left;
    private       ZoomDragPane right;

//    private ImageView    left;
//    private ImageView right;


    public DiffWindow(TaskService taskService,
                      UserPreferenceService userPrefSvc,
                      StageRepository stageRepository,
                      MediaLoader mediaLoader) {
        this.userPrefSvc = userPrefSvc;
        this.stageRepository = stageRepository;
        this.taskService = taskService;
        this.mediaLoader = mediaLoader;

        left = new ZoomDragPane();
        right = new ZoomDragPane();
//        left = new ImageView();
//        right = new ImageView();
//        left.setExtendViewport(true);
        hBox.getChildren().addAll(left, right);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);

        FxPlatformExecutor.fxRun(this::createStage);
    }

    public void show() {
        createStage();
        if (stage.isShowing()) {
            stage.hide();
        } else {
            stage.show();
        }
    }

    private void createStage() {
        if (stage == null) {
            stage = new Stage();
            stage.setScene(scene);
            stage.setTitle("Compare images");
            stage.initModality(Modality.NONE);
            stageRepository.addStage(stage);
            hBox.maxHeightProperty().bind(stage.heightProperty());
            hBox.maxWidthProperty().bind(stage.widthProperty());
//            left.setPreserveRatio(true);
//            right.setPreserveRatio(true);
//            left.fitHeightProperty().bind(stage.heightProperty());
//            left.fitWidthProperty().bind(stage.widthProperty().divide(2));
//            right.fitHeightProperty().bind(stage.heightProperty());
//            right.fitWidthProperty().bind(stage.widthProperty().divide(2));
            stage.heightProperty().addListener((_, _, newValue) -> {
                hBox.setPrefHeight(newValue.doubleValue());
                left.setPrefHeight(newValue.doubleValue());
                right.setPrefHeight(newValue.doubleValue());
                left.setMaxHeight(newValue.doubleValue());
                right.setMaxHeight(newValue.doubleValue());
                left.setMinHeight(newValue.doubleValue());
                right.setMinHeight(newValue.doubleValue());
            });
            stage.widthProperty().addListener((_, _, newValue) -> {
                var size = newValue.doubleValue() / 2D;
                hBox.setPrefWidth(newValue.doubleValue());
                left.setPrefWidth(size);
                right.setPrefWidth(size);
                left.setMaxWidth(size);
                right.setMaxWidth(size);
                left.setMinWidth(size);
                right.setMinWidth(size);
            });
            userPrefSvc.getUserPreference().getDiffWindow().restoreWindowDimension(stage);
        }
    }

    public void close() {
        stage.close();
    }

    @FxEventListener
    public void updateImages(ShowDiffEvent event) {
        log.info("Update images: {}", event);
        stage.show();
        if (event.getLeft() != null) {
            left.clearImage();
            left.setImage(taskService, mediaLoader, event.getLeft());
//            mediaLoader.getOrLoadImage2(event.getLeft(), this::updateLeft);
        }
        if (event.getRight() != null) {
            right.clearImage();
            right.setImage(taskService, mediaLoader, event.getRight());

//            mediaLoader.getOrLoadImage2(event.getRight(), this::updateRight);
        }
        log.info("Update images done: {}", event);

    }

    private void updateLeft(MediaFile mediaFile, Image image) {
        double      newMeasure = Math.min(image.getWidth(), image.getHeight());
        double      x          = (image.getWidth() - newMeasure) / 2;
        double      y          = (image.getHeight() - newMeasure) / 2;
        Rectangle2D rect       = new Rectangle2D(x, y, newMeasure, newMeasure);
//        left.setViewport(rect);
//        left.setImage(image);
        left.setImage(mediaFile, image);
        stage.toFront();
    }

    private void updateRight(MediaFile mediaFile, Image image) {
//        right.setImage(image);
        right.setImage(mediaFile, image);
        stage.toFront();
    }
}
