package org.icroco.picture.ui.gallery;

import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.GridCell;
import org.icroco.picture.ui.model.MediaFile;
import org.icroco.picture.ui.util.MediaLoader;

@Slf4j
public class MediaFileGridCell extends GridCell<MediaFile> {
    private final ImageView loadingView;
    @Getter
    private final ImageView   imageView;
    private final boolean     preserveImageProperties;
    private final MediaLoader mediaLoader;

    public final StackPane root;

    public MediaFileGridCell(boolean preserveImageProperties, MediaLoader mediaLoader) {
        this.preserveImageProperties = preserveImageProperties;
        this.mediaLoader = mediaLoader;
        getStyleClass().add("image-grid-cell");
        loadingView = new ImageView(MediaLoader.LOADING);
        loadingView.maxHeight(128);
        loadingView.maxWidth(128);
        loadingView.setImage(MediaLoader.LOADING);
        imageView = new ImageView();
        imageView.fitHeightProperty().bind(this.heightProperty().subtract(10));
        imageView.fitWidthProperty().bind(this.widthProperty().subtract(10));
        this.imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
//        root = Borders.wrap(this.imageView).lineBorder().innerPadding(5, 5, 5,5).color(Color.WHITE).build().build();
        root = new StackPane(loadingView);
//        imageView.setVisible(false);

    }

    @Override
    protected void updateItem(MediaFile item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            this.setGraphic(null);
        } else {
//            log.info("Update: {}, isSelected: {}", item.fullPath(), isSelected());

//            if (this.preserveImageProperties) {
//                this.imageView.setPreserveRatio(true);
//                imageView.setSmooth(true);
////                this.imageView.setPreserveRatio(item.isPreserveRatio());
////                this.imageView.setSmooth(item.isSmooth());
//            }
//            log.info("Image updated: {}", item.fullPath());
//            mediaLoader.loadThumbnail(item, this::setImage, this::setImage);
            root.getChildren().clear();
            if (item.getThumbnail().get() == null) {
                root.getChildren().add(loadingView);
            } else {
//                log.info("Grid Cell updated: {}", item.fullPath());
                imageView.setImage(item.getThumbnail().get().getImage());
                root.getChildren().add(imageView);
            }
            updateSelected(item.isSelected());
            setGraphic(root);
//            if (item.isLoading()) {
//                imageView.setImage(MediaLoader.LOADING);
//            } else {
//                imageView.setImage(item.getThumbnail());
//            }
//            setGraphic(imageView);
        }
    }

}
