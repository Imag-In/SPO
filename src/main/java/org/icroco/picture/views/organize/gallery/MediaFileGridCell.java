package org.icroco.picture.views.organize.gallery;

import atlantafx.base.util.Animations;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.GridCell;
import org.icroco.picture.event.UpdateMedialFileEvent;
import org.icroco.picture.model.EKeepOrThrow;
import org.icroco.picture.model.MediaCollection;
import org.icroco.picture.model.MediaFile;
import org.icroco.picture.model.Thumbnail;
import org.icroco.picture.thumbnail.ThumbnailService;
import org.icroco.picture.views.task.TaskService;
import org.icroco.picture.views.util.CustomGridView;
import org.icroco.picture.views.util.ImageUtils;
import org.icroco.picture.views.util.MediaLoader;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2OutlinedMZ;
import org.kordamp.ikonli.materialdesign2.MaterialDesignH;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;

import java.nio.file.Files;

import static org.icroco.picture.util.LangUtils.EMPTY_STRING;

@Slf4j
public class
MediaFileGridCell extends GridCell<MediaFile> {
    private final TaskService      taskService;
    @Getter
    private final ImageView        imageView;
    private final boolean          preserveImageProperties;
    private final MediaLoader      mediaLoader;
    private final ThumbnailService thumbnailService;

    public final  StackPane                               root;
    public final  BooleanProperty                         isExpandCell;
    private final ReadOnlyObjectProperty<MediaCollection> currentMediaCollection;
    private final CustomGridView<MediaFile>               grid;
    private       String                                  lastHash      = EMPTY_STRING;
    private final FontIcon                                keepIcon      = new FontIcon(Material2OutlinedMZ.THUMB_UP);
    private final FontIcon                                throwIcon     = new FontIcon(Material2OutlinedMZ.THUMB_DOWN);
    private final FontIcon                                undecidedIcon = new FontIcon(MaterialDesignH.HEAD_QUESTION_OUTLINE);
    private final Label                                   keepOrThrow   = new Label();
    private final Label                                   pathNotFound  = new Label();
    private final SimpleDoubleProperty                    gap           = new SimpleDoubleProperty(5);

    public MediaFileGridCell(TaskService taskService,
                             boolean preserveImageProperties,
                             MediaLoader mediaLoader,
                             BooleanProperty isExpandCell,
                             ReadOnlyObjectProperty<MediaCollection> currentMediaCollection,
                             CustomGridView<MediaFile> grid,
                             BooleanProperty isEditable,
                             ThumbnailService thumbnailService) {
        this.taskService = taskService;
        this.preserveImageProperties = preserveImageProperties;
        this.mediaLoader = mediaLoader;
        this.isExpandCell = isExpandCell;
        this.currentMediaCollection = currentMediaCollection;
        this.thumbnailService = thumbnailService;
        isExpandCell.addListener((_, _, _) -> requestLayout());
        getStyleClass().add("image-grid-cell");
//        loadingView.maxHeight(128 - 5);
//        loadingView.maxWidth(128 - 5);
        imageView = new ImageView();
        imageView.fitHeightProperty().bind(this.heightProperty().subtract(gap));
        imageView.fitWidthProperty().bind(this.widthProperty().subtract(gap));
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        keepOrThrow.setId("keepOrThrow");
        keepOrThrow.setGraphic(undecidedIcon);
//        root = Borders.wrap(this.imageView).lineBorder().innerPadding(5, 5, 5,5).color(Color.WHITE).build().build();
//        root = new StackPane(imageView);
        StackPane.setAlignment(this.keepOrThrow, Pos.BOTTOM_RIGHT);
        keepOrThrow.setTranslateX(this.keepOrThrow.getTranslateX() - gap.getValue() * 2);
        keepOrThrow.setTranslateY(this.keepOrThrow.getTranslateY() - gap.getValue() * 2);
        keepOrThrow.setOnMouseClicked(this::keepOrThrowClick);
        keepOrThrow.setCursor(Cursor.HAND);
        keepOrThrow.visibleProperty().bind(isEditable);

        StackPane.setAlignment(pathNotFound, Pos.TOP_LEFT);
        pathNotFound.setGraphic(FontIcon.of(MaterialDesignL.LINK_OFF));
        pathNotFound.setVisible(false);
        pathNotFound.setTranslateX(this.pathNotFound.getTranslateX() + gap.getValue() * 2);
        pathNotFound.setTranslateY(this.pathNotFound.getTranslateY() + gap.getValue() * 2);

        root = new StackPane(imageView, pathNotFound, keepOrThrow);
        root.getStyleClass().add("stack-pane");
        this.grid = grid;
    }

    private void keepOrThrowClick(MouseEvent event) {
        if (event.getClickCount() == 1) {
            getItem().setNextKeepOrThrow();
            log.info("keepOrThrow: {}", getItem().getKeepOrThrow());
            taskService.sendEvent(UpdateMedialFileEvent.builder().mediaFile(getItem()).source(this).build());
            event.consume();
        }
    }

    @Override
    protected void updateItem(MediaFile item, boolean empty) {
//        setSelected(false);
//        log.debug("updateItem: cell:{}, item: '{}' , empty: '{}'", this.hashCode(), item == null ? "null" : item.getId()+":"+item.getFileName(), empty);
        super.updateItem(item, empty);

        if (empty || item == null) {
            this.setGraphic(null);
            lastHash = EMPTY_STRING;
            pathNotFound.setVisible(false);
            pathNotFound.setTooltip(null);
            keepOrThrow.graphicProperty().unbind();
        } else {
            boolean contains = grid.getSelectionModel().getSelectedItems().contains(item);
            pathNotFound.setTooltip(new Tooltip("File path not not found (or not mounted): %s".formatted(item.getFullPath())));
            pathNotFound.setVisible(Files.notExists(item.fullPath()));
            if (contains) {
                gap.set(10);
            } else {
                gap.set(5);
            }
            keepOrThrow.graphicProperty().bind(item.getKeepOrThrowProperty().map(this::displayKeepOrThrowIcon));
            updateSelected(contains);
            if (item.isLoadedInCache()) {
                if ((lastHash != null && !lastHash.equals(item.getHash())
                     || MediaFileGridCellFactory.isCellVisible(grid, this))) {
                    lastHash = item.getHash();
                    log.atDebug().log(() -> "Grid Cell updated(%s): %s: %s - %s - %s ('%s'/'%s') - %s".formatted(this.hashCode(),
                                                                                                                 item.getFullPath(),
                                                                                                                 item.getKeywords(),
                                                                                                                 false,/*item.isLoadedInCache(),*/
                                                                                                                 lastHash,
                                                                                                                 item.getHash(),
                                                                                                                 (!lastHash.equals(item.getHash())),
                                                                                                                 MediaFileGridCellFactory.isCellVisible(
                                                                                                                         grid, this)));
                    setImage(thumbnailService.get(item)
                                        .map(Thumbnail::getImage)
                                        .orElse(ImageUtils.getNoThumbnailImage()));
                }
            } else {
                setImage(ImageUtils.getNoThumbnailImage());
            }
            setGraphic(root);
        }
    }

    Node displayKeepOrThrowIcon(EKeepOrThrow flag) {
        return switch (flag) {
            case UNKNOW -> undecidedIcon;
            case KEEP -> keepIcon;
            case THROW -> throwIcon;
        };
    }

    private ImageView setImage(Image image) {
        if (isExpandCell.getValue()) {
            double      newMeasure = Math.min(image.getWidth(), image.getHeight());
            double      x          = (image.getWidth() - newMeasure) / 2;
            double      y          = (image.getHeight() - newMeasure) / 2;
            Rectangle2D rect       = new Rectangle2D(x, y, newMeasure, newMeasure);
//            log.info("image W: {}, H: {}, cell W: {}, H: {}", image.getWidth(), image.getHeight(), getWidth(), getHeight());
            imageView.setViewport(rect);
        } else {
            imageView.setViewport(null);
        }
        if (image != imageView.getImage()) {
            imageView.setOpacity(0);
            imageView.setImage(image);
            Animations.fadeIn(imageView, Duration.millis(300)).playFromStart();
        }

//        imageView.setFitWidth(Math.min(image.getWidth(), getWidth())-gap.getValue());
//        imageView.setFitHeight(Math.min(image.getHeight(), getHeight())-gap.getValue());

        return imageView;
    }
}
