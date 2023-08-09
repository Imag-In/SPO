package org.icroco.picture.ui.gallery;

import javafx.beans.property.BooleanProperty;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.GridCell;
import org.icroco.picture.ui.model.MediaFile;
import org.icroco.picture.ui.model.Thumbnail;
import org.icroco.picture.ui.util.CustomGridView;
import org.icroco.picture.ui.util.GridCellSelectionModel;
import org.icroco.picture.ui.util.MediaLoader;

@Slf4j
public class MediaFileGridCell extends GridCell<MediaFile> {
    private final ImageView                 loadingView;
    @Getter
    private final ImageView                 imageView;
    private final boolean                   preserveImageProperties;
    private final MediaLoader               mediaLoader;
    public final  StackPane                 root;
    public final  BooleanProperty           isExpandCell;
    private       MediaFile                 oldValue = null;
    private       GridCellSelectionModel    selectionModel;
    private final CustomGridView<MediaFile> grid;

    public MediaFileGridCell(boolean preserveImageProperties, MediaLoader mediaLoader, BooleanProperty isExpandCell, GridCellSelectionModel selectionModel,
                             CustomGridView<MediaFile> grid) {
        this.preserveImageProperties = preserveImageProperties;
        this.mediaLoader = mediaLoader;
        this.isExpandCell = isExpandCell;
        this.selectionModel = selectionModel;
        getStyleClass().add("image-grid-cell");
        loadingView = new ImageView(MediaLoader.LOADING);
        loadingView.maxHeight(128);
        loadingView.maxWidth(128);
        imageView = new ImageView();
        imageView.fitHeightProperty().bind(this.heightProperty().subtract(3));
        imageView.fitWidthProperty().bind(this.widthProperty().subtract(3));
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
//        root = Borders.wrap(this.imageView).lineBorder().innerPadding(5, 5, 5,5).color(Color.WHITE).build().build();
        root = new StackPane(imageView);
        this.grid = grid;
    }

    @Override
    protected void updateItem(MediaFile item, boolean empty) {
//        log.debug("updateItem: cell:{}, item: '{}' , empty: '{}'", this.hashCode(), item == null ? "null" : item.getId()+":"+item.getFileName(), empty);
        super.updateItem(item, empty);

        if (empty || item == null) {
            this.setGraphic(null);
        } else {
            if (item.isLoadedInCache()) {
                setImage(mediaLoader.getCachedValue(item)
                                    .map(Thumbnail::getImage)
                                    .orElse(MediaLoader.LOADING));
            } else {
                setImage(MediaLoader.LOADING);
//                mediaLoader.loadAndCachedValue(item);
            }
//            if (MediaFileGridCellFactory.isCellVisible(grid, this)) {
//                setImage(mediaLoader.getCachedValue(item)
//                                    .map(Thumbnail::getImage)
//                                    .orElseGet(() -> {
//                                        mediaLoader.loadAndCachedValue(item);
//                                        return MediaLoader.LOADING;
//                                    }));
//            }

            updateSelected(selectionModel.contains(item));
            setGraphic(root);
        }
    }


    private ImageView setImage(Image image) {
        if (isExpandCell.getValue()) {
            double      newMeasure = Math.min(image.getWidth(), image.getHeight());
            double      x          = (image.getWidth() - newMeasure) / 2;
            double      y          = (image.getHeight() - newMeasure) / 2;
            Rectangle2D rect       = new Rectangle2D(x, y, newMeasure, newMeasure);
            imageView.setViewport(rect);
        } else {
            imageView.setViewport(null);
        }
        imageView.setImage(image);

        return imageView;
    }

}
