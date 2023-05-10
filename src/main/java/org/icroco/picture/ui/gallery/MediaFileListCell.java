package org.icroco.picture.ui.gallery;

import javafx.geometry.Rectangle2D;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.ui.model.MediaFile;
import org.icroco.picture.ui.model.Thumbnail;
import org.icroco.picture.ui.util.MediaLoader;

@Slf4j
public class MediaFileListCell extends ListCell<MediaFile> {
    private final static ImageView loadingView = new ImageView(MediaLoader.LOADING);

    static {
        loadingView.maxWidth(128);
        loadingView.maxHeight(128);
    }

    @Getter
    private final ImageView   imageView;
    private final MediaLoader mediaLoader;
    public final  StackPane   root;

    public MediaFileListCell(MediaLoader mediaLoader) {
        this.mediaLoader = mediaLoader;
        getStyleClass().add("image-grid-cell");
        imageView = new ImageView(MediaLoader.LOADING);
        imageView.fitHeightProperty().bind(this.heightProperty().subtract(3));
        imageView.fitWidthProperty().bind(this.widthProperty().subtract(3));
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
//        root = Borders.wrap(this.imageView).lineBorder().innerPadding(5, 5, 5,5).color(Color.WHITE).build().build();
        root = new StackPane(imageView);
//        imageView.setVisible(false);

    }

    @Override
    protected void updateItem(MediaFile item, boolean empty) {
        log.debug("updateItem: cell:{}, item: '{}' , empty: '{}'", this.hashCode(), item == null ? "null" : item.getFileName(), empty);
        super.updateItem(item, empty);

        if (empty || item == null) {
            this.setGraphic(null);
        } else {
//            if (item.isLoaded()) {
            setImage(mediaLoader.getCachedValue(item)
                                .map(Thumbnail::getImage)
                                .orElse(MediaLoader.LOADING));
//            } else {
//                setImage(MediaLoader.LOADING);
//            }
            setGraphic(root);
        }
    }

    private ImageView setImage(Image image) {
        double      newMeasure = Math.min(image.getWidth(), image.getHeight());
        double      x          = (image.getWidth() - newMeasure) / 2;
        double      y          = (image.getHeight() - newMeasure) / 2;
        Rectangle2D rect       = new Rectangle2D(x, y, newMeasure, newMeasure);
        imageView.setViewport(rect);
        imageView.setImage(image);

        return imageView;
    }

}
