package org.icroco.picture.views.organize.gallery;

import atlantafx.base.util.Animations;
import javafx.beans.property.BooleanProperty;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.GridCell;
import org.icroco.picture.model.MediaFile;
import org.icroco.picture.model.Thumbnail;
import org.icroco.picture.views.util.CustomGridView;
import org.icroco.picture.views.util.ImageUtils;
import org.icroco.picture.views.util.MediaLoader;

import static org.icroco.picture.views.util.LangUtils.EMPTY_STRING;

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
    private final CustomGridView<MediaFile> grid;
    private       String                    lastHash = EMPTY_STRING;

    public MediaFileGridCell(boolean preserveImageProperties,
                             MediaLoader mediaLoader,
                             BooleanProperty isExpandCell,
                             CustomGridView<MediaFile> grid) {
        this.preserveImageProperties = preserveImageProperties;
        this.mediaLoader = mediaLoader;
        this.isExpandCell = isExpandCell;
        getStyleClass().add("image-grid-cell");
        loadingView = new ImageView(ImageUtils.LOADING);
        loadingView.maxHeight(128 - 5);
        loadingView.maxWidth(128 - 5);
        imageView = new ImageView();
        imageView.fitHeightProperty().bind(this.heightProperty().subtract(5));
        imageView.fitWidthProperty().bind(this.widthProperty().subtract(5));
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
//        root = Borders.wrap(this.imageView).lineBorder().innerPadding(5, 5, 5,5).color(Color.WHITE).build().build();
        root = new StackPane(imageView);
        this.grid = grid;
    }

    @Override
    protected void updateItem(MediaFile item, boolean empty) {
//        setSelected(false);
//        log.debug("updateItem: cell:{}, item: '{}' , empty: '{}'", this.hashCode(), item == null ? "null" : item.getId()+":"+item.getFileName(), empty);
        super.updateItem(item, empty);

        if (empty || item == null) {
            this.setGraphic(null);
            lastHash = EMPTY_STRING;
            log.debug("reset cell({})", this.hashCode());
        } else {
            updateSelected(grid.getSelectionModel().contains(this));
            item.getLastUpdated().addListener((observable, oldValue1, newValue) -> {
                log.info("Update: {}:{}", item.getFullPath(), newValue);
            });
            if (item.isLoadedInCache()) {
                if ((lastHash != null && !lastHash.equals(item.getHash()) || MediaFileGridCellFactory.isCellVisible(grid, this))) {
                    lastHash = item.getHash();
                    log.atDebug().log(() -> "Grid Cell updated(%s): %s: %s - %s - %s ('%s'/'%s') - %s".formatted(this.hashCode(),
                                                                                                                 item.getFullPath(),
                                                                                                                 item.getKeywords(),
                                                                                                                 item.isLoadedInCache(),
                                                                                                                 lastHash,
                                                                                                                 item.getHash(),
                                                                                                                 (!lastHash.equals(item.getHash())),
                                                                                                                 MediaFileGridCellFactory.isCellVisible(
                                                                                                                         grid,
                                                                                                                         this)));
                    setImage(mediaLoader.getCachedValue(item)
                                        .map(Thumbnail::getImage)
                                        .orElse(ImageUtils.getNoThumbnailImage()));
                }
            } else {
                setImage(ImageUtils.getNoThumbnailImage());
            }
            setGraphic(root);
//            if (MediaFileGridCellFactory.isCellVisible(grid, this)) {
//                setImage(mediaLoader.getCachedValue(item)
//                                    .map(Thumbnail::getImage)
//                                    .orElseGet(() -> {
//                                        mediaLoader.loadAndCachedValue(item);
//                                        return MediaLoader.LOADING;
//                                    }));
//            }
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
        if (image != imageView.getImage()) {
            imageView.setOpacity(0);
            imageView.setImage(image);
            Animations.fadeIn(imageView, Duration.millis(300)).playFromStart();
        }


        return imageView;
    }

}
