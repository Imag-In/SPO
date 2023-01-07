package org.icroco.picture.ui.gallery;

import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.GridCell;
import org.controlsfx.tools.Borders;
import org.icroco.picture.ui.model.MediaFile;
import org.icroco.picture.ui.util.MediaLoader;

@Slf4j
public class MediaFileGridCell extends GridCell<MediaFile> {
    private final ImageView   imageView;
    private final boolean     preserveImageProperties;
    public final  MediaLoader mediaLoader;

    public final StackPane root;

    public MediaFileGridCell(boolean preserveImageProperties, MediaLoader mediaLoader) {
        this.preserveImageProperties = preserveImageProperties;
        this.mediaLoader = mediaLoader;
        this.getStyleClass().add("image-grid-cell");
        this.imageView = new ImageView();
        this.imageView.fitHeightProperty().bind(this.heightProperty().subtract(10));
        this.imageView.fitWidthProperty().bind(this.widthProperty().subtract(10));
//        root = Borders.wrap(this.imageView).lineBorder().innerPadding(5, 5, 5,5).color(Color.WHITE).build().build();
        root = new StackPane(this.imageView);
    }

    @Override
    protected void updateItem(MediaFile item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            this.setGraphic((Node) null);
        } else {
            if (this.preserveImageProperties) {
                this.imageView.setPreserveRatio(true);
                imageView.setSmooth(true);
//                this.imageView.setPreserveRatio(item.isPreserveRatio());
//                this.imageView.setSmooth(item.isSmooth());
            }

            this.imageView.setImage(mediaLoader.loadThumbnail(item));
            this.setGraphic(this.root);
        }
    }
}
