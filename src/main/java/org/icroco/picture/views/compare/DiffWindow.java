package org.icroco.picture.views.compare;

import javafx.application.Platform;
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
import org.icroco.picture.views.pref.UserPreferenceService;
import org.icroco.picture.views.util.MediaLoader;
import org.icroco.picture.views.util.widget.ZoomDragPane;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DiffWindow {
    private final HBox  hBox  = new HBox();
    private final Scene scene = new Scene(hBox);

    @Getter
    private Stage stage;

//    private final ImageView left = new ImageView();
//    private final ImageView right = new ImageView();

    private ZoomDragPane left;
    private ZoomDragPane right;

    private final MediaLoader mediaLoader;

    public DiffWindow(MediaLoader mediaLoader, UserPreferenceService userPrefSvc) {
        this.mediaLoader = mediaLoader;
        Platform.runLater(() -> {
            stage = new Stage();
            stage.setScene(scene);
            stage.setTitle("Compare images");
            stage.initModality(Modality.NONE);

            left = new ZoomDragPane();
            right = new ZoomDragPane();
            hBox.getChildren().addAll(left, right);
            HBox.setHgrow(left, Priority.ALWAYS);
            HBox.setHgrow(right, Priority.ALWAYS);
            left.getView().fitHeightProperty().bind(stage.heightProperty());
            left.getView().fitWidthProperty().bind(stage.widthProperty().divide(2));
            right.getView().fitHeightProperty().bind(stage.heightProperty());
            right.getView().fitWidthProperty().bind(stage.widthProperty().divide(2));
            userPrefSvc.getUserPreference().getDiffWindow().restoreWindowDimension(stage);
        });
    }

    public void show() {
        if (stage.isShowing()) {
            stage.hide();
        } else {
            stage.show();
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
            mediaLoader.getOrLoadImage2(event.getLeft(), this::updateLeft);
        }
        if (event.getRight() != null) {
            mediaLoader.getOrLoadImage2(event.getRight(), this::updateRight);
        }
    }

    private void updateLeft(MediaFile mediaFile, Image image) {
        left.setImage(mediaFile, image);
    }

    private void updateRight(MediaFile mediaFile, Image image) {
        log.info("updateRight: {}, image null: {}", mediaFile, image == null);
        right.setImage(mediaFile, image);
    }
}
