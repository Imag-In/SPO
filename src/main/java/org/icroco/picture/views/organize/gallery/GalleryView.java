package org.icroco.picture.views.organize.gallery;

import atlantafx.base.controls.Breadcrumbs;
import atlantafx.base.controls.ProgressSliderSkin;
import atlantafx.base.controls.Spacer;
import atlantafx.base.util.Animations;
import jakarta.annotation.PostConstruct;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.icroco.picture.event.*;
import org.icroco.picture.model.EKeepOrThrow;
import org.icroco.picture.model.MediaCollection;
import org.icroco.picture.model.MediaFile;
import org.icroco.picture.model.Thumbnail;
import org.icroco.picture.persistence.PersistenceService;
import org.icroco.picture.util.SceneReadyEvent;
import org.icroco.picture.views.FxEventListener;
import org.icroco.picture.views.ViewConfiguration;
import org.icroco.picture.views.organize.PathSelection;
import org.icroco.picture.views.pref.UserPreferenceService;
import org.icroco.picture.views.task.TaskService;
import org.icroco.picture.views.util.*;
import org.icroco.picture.views.util.widget.Zoom;
import org.icroco.picture.views.util.widget.ZoomDragPane;
import org.jooq.lambda.Unchecked;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.javafx.StackedFontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2OutlinedAL;
import org.kordamp.ikonli.material2.Material2OutlinedMZ;
import org.slf4j.Logger;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static javafx.application.Platform.runLater;
import static org.fxmisc.wellbehaved.event.EventPattern.keyPressed;
import static org.fxmisc.wellbehaved.event.InputMap.consume;
import static org.fxmisc.wellbehaved.event.InputMap.sequence;
import static org.fxmisc.wellbehaved.event.Nodes.addInputMap;

@Component
@RequiredArgsConstructor
public class GalleryView implements FxView<StackPane> {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(GalleryView.class);

    private final MediaLoader           mediaLoader;
    private final UserPreferenceService pref;
    private final TaskService           taskService;
    //    @Qualifier(OrganizeConfiguration.GALLERY_ZOOM)
    private       ZoomDragPane          photo;
    private final PersistenceService    persistenceService;

    private final StackPane                             root           = new StackPane();
    private final BorderPane                            gallery        = new BorderPane();
    private final BorderPane                            carousel       = new BorderPane();
    private final Slider                                zoomThumbnails = createTickSlider();
    private final Breadcrumbs<Path>                     breadCrumbBar  = new Breadcrumbs<>();
    private       CustomGridView<MediaFile>             gridView;
    //    private final StackPane                             photoContainer = new StackPane();
    private final BooleanProperty                       expandCell     = new SimpleBooleanProperty(true);
    private final ObservableList<MediaFile>             images         = FXCollections.observableArrayList(MediaFile.extractor());
    private final FilteredList<MediaFile>               filteredImages = new FilteredList<>(images);
    private final SortedList<MediaFile>                 sortedImages   = new SortedList<>(filteredImages);
    private       double                                gridCellWidth;
    //    private       double                                gridCellHeight;
    private       int                                   zoomLevel      = 0;
    private final SimpleObjectProperty<MediaCollection> currentCatalog = new SimpleObjectProperty<>(null);
    private       EGalleryClickState                    dblCickState   = EGalleryClickState.GALLERY;
    private       HBox                                  toolBar;
    FontIcon        thumbsUpDownIcon = new FontIcon(Material2OutlinedMZ.THUMBS_UP_DOWN);
    FontIcon        blockIcon        = new FontIcon(Material2OutlinedAL.BLOCK);
    StackedFontIcon keepOrThrowIcon  = new StackedFontIcon();
    Label           keepOrThrowLabel = new Label();

    @PostConstruct
    protected void postConstruct() {
        root.getStyleClass().add(ViewConfiguration.V_GALLERY);
        root.setId(ViewConfiguration.V_GALLERY);
        gridView = new CustomGridView<>(taskService, FXCollections.emptyObservableList());
        gridView.addScrollAndKeyhandler();
        root.setMinSize(350, 250);
        root.setEventDispatcher(new DoubleClickEventDispatcher(root.getEventDispatcher()));

        log.debug("GalleryView: gridCellWidth: {}, gridCellHeight: {}, hCellSpacing: {}, vCellSpacing: {}",
                  gridView.getCellWidth(),
                  gridView.getCellHeight(),
                  gridView.getHorizontalCellSpacing(),
                  gridView.getVerticalCellSpacing());
        sortedImages.setComparator(Comparator.comparing(MediaFile::getOriginalDate));
        gridView.setItems(sortedImages);
//        gridCellWidth = Optional.ofNullable(pref.getUserPreference().getGrid().getCellWidth()).orElse((int)gridView.getCellWidth());
//        gridCellHeight = Optional.ofNullable(pref.getUserPreference().getGrid().getCellHeight()).orElse((int)gridView.getCellHeight());
//        gridCellWidth = 128; //gridView.getCellWidth() * 2;
//        gridCellHeight = 128; //gridView.getCellHeight() * 2;
//        gridView.cellWidthProperty().addListener((observable, oldValue, newValue) -> log.info("Grid Cell Width: {}", newValue));

        gridView.setCellWidth(128);
        gridView.setCellHeight(128);
        gridView.setCache(true);
        gridView.setCacheHint(CacheHint.SPEED);
        ofNullable(pref.getUserPreference().getGrid().getGridZoomFactor()).ifPresent(this::applyGridCellWidthFactor);
        gridView.setHorizontalCellSpacing(0D);
        gridView.setVerticalCellSpacing(0D);
        keepOrThrowLabel.setOpacity(0.4);
        gridView.setCellFactory(new MediaFileGridCellFactory(mediaLoader,
                                                             taskService,
                                                             expandCell,
                                                             this::cellDoubleClick,
                                                             Bindings.greaterThan(keepOrThrowLabel.opacityProperty(), 0.4)));
        gridView.setOnZoom(this::zoomOnGrid);
        currentCatalog.addListener(this::collectionChanged);
        carousel.addEventHandler(CustomMouseEvent.MOUSE_DOUBLE_CLICKED, this::onImageClick);
        carousel.setId("Carousel");
//        photoContainer.getChildren().add(photo);

//        carousel.maxHeightProperty().bind(root.heightProperty());
//        carousel.maxWidthProperty().bind(root.widthProperty());
        photo = new ZoomDragPane(mediaLoader);
        carousel.setCenter(photo);
        photo.maxHeightProperty().bind(carousel.heightProperty());
        photo.maxWidthProperty().bind(carousel.widthProperty());
//        carousel.setBottom(carouselIcons);

        addInputMap(root, sequence(consume(keyPressed(KeyCode.ESCAPE), this::escapePressed)));
//        addInputMap(root, sequence(consume(keyPressed(KeyCode.LEFT), this::leftPressed)));
//        addInputMap(root, sequence(consume(keyPressed(KeyCode.RIGHT), this::rightPressed)));
//        addInputMap(root, sequence(consume(keyPressed(KeyCode.UP), this::skipPressed)));
//        addInputMap(root, sequence(consume(keyPressed(KeyCode.DOWN), this::skipPressed)));
        addInputMap(root, sequence(consume(keyPressed(KeyCode.K), this::kPressed)));
//        addInputMap(root, sequence(consume(keyPressed(KeyCode.P), keyEvent -> log.info("P pressed"))));
//        addInputMap(root, sequence(consume(keyPressed(KeyCode.DIGIT4), keyEvent -> log.info("Shift pressed"))));
//
//        root.setOnKeyPressed(ev2 -> {if (ev2.getCode() == KeyCode.DIGIT2) {
//            log.info("2222");
//        }});
//        photo.fitHeightProperty().bind(photoContainer.heightProperty().subtract(10));
//        photo.fitWidthProperty().bind(photoContainer.widthProperty().subtract(10));

        breadCrumbBar.setCrumbFactory(item -> new Hyperlink(item.getValue().getFileName().toString()));
        breadCrumbBar.setAutoNavigationEnabled(false);
        breadCrumbBar.setOnCrumbAction(bae -> {
            log.info("You just clicked on '" + bae.getSelectedCrumb() + "'!");
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE_FILE_DIR)) {
                Unchecked.runnable(() -> Desktop.getDesktop().browseFileDirectory(bae.getSelectedCrumb().getValue().toFile())).run();
            }
        });
        breadCrumbBar.setDividerFactory(GalleryView::bcbDividerFactory);

//        expandCell.addListener((observable, oldValue, newValue) -> gridView.refreshItems());

        toolBar = createBottomBar();
        gallery.getStyleClass().setAll("gallery-center");
        gallery.setCenter(gridView);
        gallery.setBottom(toolBar);
        carousel.setVisible(false);
        root.getChildren().addAll(gallery, carousel);
    }

    private static Node bcbDividerFactory(Breadcrumbs.BreadCrumbItem<Path> item) {
        if (item == null) {
            return new Label("", new FontIcon(Material2AL.HOME));
        }
        return !item.isLast()
               ? new Label("", new FontIcon(Material2AL.CHEVRON_RIGHT))
               : null;
    }

    private void cellDoubleClick(MouseEvent event, MediaFileGridCell cell) {
//        var mf = ((MediaFileGridCell) event.getSource()).getItem();
        var optMf = ofNullable(cell.getItem());
        log.atDebug()
           .log(() -> "#click: %s, mf: %s".formatted(event.getClickCount(),
                                                     optMf.map(MediaFile::getFullPath).orElse(null)));
        optMf.ifPresent(mf -> {
            if (event.getClickCount() == 1) {
                gridView.getSelectionModel().set(cell);
                cell.requestLayout();
            } else if (event.getClickCount() == 2) {
                gridView.getSelectionModel().set(cell);
                displayNext(mf);
            }
        });

        event.consume();
    }

    HBox createBottomBar() {
        HBox bar = new HBox();
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().setAll("tool-bar");

        Label expand = new Label();
        expand.setCursor(Cursor.HAND);
        expand.setPrefHeight(10D);
        FontIcon icon = new FontIcon();
//        icon.setIconSize(32);
        icon.setId("grid-cell-collapse");
        expand.setGraphic(icon);
        expand.setOnMouseClicked(this::expandGridCell);

//        expand.setPrefHeight(10D);

        thumbsUpDownIcon.getStyleClass().add("inner-icon");
        blockIcon.getStyleClass().add("outer-icon");
        keepOrThrowIcon.getChildren().addAll(thumbsUpDownIcon, blockIcon);
//        new FontIcon(Material2OutlinedMZ.THUMBS_UP_DOWN);
//        icon.setIconSize(32);
//        icon.setId("fitGridCell");
        keepOrThrowLabel.setGraphic(thumbsUpDownIcon);
        keepOrThrowLabel.setOnMouseClicked(this::keepOrThrowClick);
        keepOrThrowLabel.setCursor(Cursor.HAND);
//        keepOrThrowLabel.setDisable(true);
        keepOrThrowLabel.setOpacity(.4);
        keepOrThrowLabel.setTooltip(new Tooltip("Press 'k' to enable / disable 'Keep or Throw"));
//        zoomThumbnails.getStyleClass().add(Styles.SMALL);
        zoomThumbnails.setSkin(new ProgressSliderSkin(zoomThumbnails));
        ofNullable(pref.getUserPreference().getGrid().getGridZoomFactor()).ifPresent(zoomThumbnails::setValue);
        zoomThumbnails.setBlockIncrement(1);
        zoomThumbnails.valueProperty()
                      .addListener((ObservableValue<? extends Number> _, Number _, Number newValue) -> {
                          zoomLevel = newValue.intValue();
                          log.debug("Zoom Level: {}", zoomLevel);
                          applyGridCellWidthFactor(zoomLevel);
                          pref.getUserPreference().getGrid().setGridZoomFactor(zoomLevel);
//                          gridView.setCellWidth(gridCellWidth + 10 * zoomLevel);
//                          gridView.setCellHeight(gridCellHeight + 10 * zoomLevel);
//            carouselIcons.setFixedCellSize(gridView.getCellWidth());
                      });
        HBox.setHgrow(breadCrumbBar, Priority.ALWAYS);
        Label nbImages = new Label();
        nbImages.textProperty().bind(Bindings.size(sortedImages).map(number -> number + " files"));

        Label nbKeep = new Label();
        nbKeep.setGraphic(new FontIcon(Material2OutlinedMZ.THUMB_UP));
        nbKeep.textProperty()
              .bind(Bindings.size(sortedImages.filtered(mf -> mf.getKeepOrThrow() == EKeepOrThrow.KEEP)).map(Object::toString));
        Label nbThrow = new Label();
        nbThrow.setGraphic(new FontIcon(Material2OutlinedMZ.THUMB_DOWN));
        nbThrow.textProperty()
               .bind(Bindings.size(sortedImages.filtered(mf -> mf.getKeepOrThrow() == EKeepOrThrow.THROW)).map(Object::toString));
        nbKeep.visibleProperty().bind(keepOrThrowLabel.opacityProperty().map(number -> number.doubleValue() > 0.4));
        nbThrow.visibleProperty().bind(keepOrThrowLabel.opacityProperty().map(number -> number.doubleValue() > 0.4));

        bar.getChildren().addAll(expand, keepOrThrowLabel, zoomThumbnails, breadCrumbBar, new Spacer(), nbKeep, nbThrow, new Separator(
                Orientation.VERTICAL), nbImages);

        return bar;
    }

    private void applyGridCellWidthFactor(int value) {
        var width = 128 + 10D * Math.max(0, value);
        gridView.setCellWidth(width);
        gridView.setCellHeight(width);
    }

    private Slider createTickSlider() {
        var slider = new Slider(0, 10, 0);
//        slider.setShowTickLabels(true);
//        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(1);
        slider.setBlockIncrement(1);
        slider.setMinorTickCount(5);
        slider.setSnapToTicks(true);
        return slider;
    }

    void collectionChanged(ObservableValue<? extends MediaCollection> observable,
                           MediaCollection oldValue,
                           MediaCollection newValue) {
        if (newValue != null) {
            images.clear();
            gridView.getSelectionModel().clear();
            resetBcbModel(null);
            filteredImages.setPredicate(null);
            images.addAll(newValue.medias());
        } else {
            breadCrumbBar.setSelectedCrumb(null);
            images.clear();
            filteredImages.setPredicate(null);
        }
    }

    private void onImageClick(MouseEvent event) {
        if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
            displayNext(null);
            event.consume();
        }
    }

    private void displayNext(MediaFile mf) {
        EGalleryClickState prev = dblCickState;
        dblCickState = dblCickState.next();
        log.atDebug()
           .log(() -> "Next click: %s, file: %s".formatted(dblCickState, ofNullable(mf).map(MediaFile::getFullPath).orElse(null)));
        switch (dblCickState) {
            case GALLERY -> displayGallery(mf);
            case IMAGE, IMAGE_BACK -> {
                photo.requestFocus();
                if (prev == EGalleryClickState.GALLERY) {
                    gallery.setVisible(false);
                    photo.setImage(null, null, false);
                    carousel.setVisible(true);
                    mediaLoader.getOrLoadImage(mf);
                } else {
                    photo.noZoom();
                    gallery.setVisible(false);
                    carousel.setVisible(true);
                }
            }
            case ZOOM -> {
                photo.requestFocus();
                photo.zoom();
            }
        }
    }

    private void displayGallery(MediaFile mf) {
        dblCickState = EGalleryClickState.GALLERY;
        gallery.requestFocus();
        Animations.fadeOut(carousel, Duration.millis(1000)).playFromStart();
        gallery.setVisible(true);
        carousel.setVisible(false);
        if (mf != null) {
            gridView.ensureVisible(mf);
//            gridView.getSelectionModel().set();
        }
    }

    private void zoomOnGrid(ZoomEvent event) {
        var zoom = Zoom.of(event);
        zoomLevel += zoom.getZoomLevelDelta() * 10;
        zoomLevel = Math.min(zoomLevel, 100);
        zoomLevel = Math.max(zoomLevel, 0);
        log.info("zoom:{}, zoomLevel: {}", zoom, zoomLevel);
//        zoomThumbnails.setValue((int) zoomLevel);

//        final var ratio = event.getTotalZoomFactor() / event.getZoomFactor();
//        final var zoomValue = ratio >= 1
//                              ? Math.min(100.0, Math.round(Math.max(zoomThumbnails.getValue(), 10) * ratio))
//                              : Math.max(0D, Math.floor(zoomThumbnails.getValue() * event.getTotalZoomFactor()));
//        log.info("type: {}, factor: {}, totalFactor: {}, ratio: {}, zoomValue: {}",
//                 event.getEventType(), event.getZoomFactor(), event.getTotalZoomFactor(), ratio, (int) zoomValue);
        runLater(() -> zoomThumbnails.setValue(zoomLevel));

        event.consume();
    }

    @FxEventListener
    public void updateImages(CollectionEvent event) {
        log.info("CollectionEvent type {}, CollectionId: {},", event.getType(), event.getMcId());

        switch (event.getType()) {
            case DELETED -> {
                currentCatalog.set(null);
                // TODO: clear thumbnail cache ?
            }
            case SELECTED -> {
                currentCatalog.set(persistenceService.getMediaCollection(event.getMcId()));
                // TODO: Warm thumbnail cache ?
            }
            case READY -> {
                var mc = persistenceService.getMediaCollection(event.getMcId())
                                           .medias()
                                           .stream()
                                           .collect(Collectors.toMap(MediaFile::getId, Function.identity()));

                gridView.getSelectionModel()
                        .getSelection()
                        .stream()
                        .map(cell -> {
                            var found = mc.get(cell.getItem().getId());
                            if (found == null) {
                                return null;
                            } else {
                                cell.setItem(found);
                                return cell;
                            }
                        })
                        .filter(Objects::nonNull)
                        .forEach(gridView.getSelectionModel()::set);
            }
        }
    }

    @FxEventListener
    public void updateImages(CollectionUpdatedEvent event) {
        if (event.isEmpty()) {
            return;
        }
        log.info("Recieved CollectionUpdatedEvent on collection: '{}', newItems: '{}', deletedItems: '{}', modifiedItems: '{}'",
                 event.getMcId(),
                 event.getNewItems().size(),
                 event.getDeletedItems().size(),
                 event.getModifiedItems());
        images.addAll(event.getNewItems());
        images.removeAll(event.getDeletedItems());
        // TODO: find a smoother way to update.
        images.removeAll(event.getModifiedItems());
        images.addAll(event.getModifiedItems());
    }


    public void collectionPathChange(ObservableValue<? extends PathSelection> observable, PathSelection oldValue, PathSelection newValue) {
        dblCickState = EGalleryClickState.GALLERY;
        if (newValue.equalsNoSeed(oldValue)) {
            log.debug("MediaCollection subpath re-selected: root: {}, entry: {}", newValue.mediaCollectionId(), newValue.subPath());
            displayGallery(null);
//            gallery.setOpacity(0);
//             Animations.fadeIn(gallery, Duration.millis(1000)).playFromStart();
        } else {
            log.debug("MediaCollection subpath selected: root: {}, entry: {}", newValue.mediaCollectionId(), newValue.subPath());
            currentCatalog.setValue(persistenceService.getMediaCollection(newValue.mediaCollectionId()));
            resetBcbModel(newValue.subPath());
            final var path = getCurrentCatalog().path().resolve(newValue.subPath());
            filteredImages.setPredicate(mediaFile -> mediaFile.fullPath().startsWith(path));
//        mediaLoader.warmThumbnailCache(getCurrentCatalog(), filteredImages);
//            gridView.getFirstVisible().ifPresent(mediaFileCell -> gridView.getSelectionModel().set(mediaFileCell));
        }
    }

    private MediaCollection getCurrentCatalog() {
        return ofNullable(currentCatalog.get())
                .orElseThrow(() -> new IllegalStateException("Technical error, current catalog is not set"));
    }

    @FxEventListener
    public void updatePhotoSelected(PhotoSelectedEvent event) {
        log.info("Receive PhotoSelectedEvent: {}", event);
        if (event.getType() == PhotoSelectedEvent.ESelectionType.SELECTED) {
            final var source = ofNullable(event.getSource()).orElse(MediaFileListCellFactory.class).getClass();
            final var mf     = event.getMf();
            log.atDebug().log(() -> {
                Optional<Thumbnail> cache = persistenceService.getThumbnailFromCache(mf);
                Optional<Thumbnail> db    = persistenceService.findByPathOrId(mf);
                return "Photo selected: root: '%s', '%s', '%s', from: '%s'. Thumbhnail DB id: '%s', type: '%s'. Tumbhnail Cache, id: '%s', type: '%s'"
                        .formatted(mf.getId(),
                                   mf.getFileName(),
                                   mf.getThumbnailType(),
                                   source.getSimpleName(),
                                   db.map(Thumbnail::getMfId).orElse(-1L),
                                   db.map(Thumbnail::getOrigin).orElse(null),
                                   cache.map(Thumbnail::getMfId).orElse(-1L),
                                   cache.map(Thumbnail::getOrigin).orElse(null)
                        );
            });
            resetBcbModel(mf.getFullPath());
        }
    }

    @FxEventListener
    public void sceanREady(SceneReadyEvent event) {
        root.requestFocus();
    }

    @FxEventListener
    public void imageLoading(ImageLoadingdEvent event) {
        photo.getMaskerPane().start(event.getProgress());
    }

    @FxEventListener
    public void imageLoaded(ImageLoadedEvent event) {
        photo.setImage(event.getMediaFile(), event.getImage(), event.isFromCache());
        if (!event.isFromCache()) {
            photo.getMaskerPane().stop();
        }
        gridView.getLeftAndRight(event.getMediaFile()).forEach(mediaLoader::warmCache);
    }

    private void resetBcbModel(@Nullable Path entry) {
        log.debug("Reset CB: {}", entry);
        var        rootPath = getCurrentCatalog().path();
        List<Path> paths    = new ArrayList<>(10);
        while (entry != null && !rootPath.equals(entry)) {
            paths.add(entry);
            entry = entry.getParent();
        }
        paths = paths.reversed();
        paths.addFirst(rootPath);
        Breadcrumbs.BreadCrumbItem<Path> model = Breadcrumbs.buildTreeModel(paths.toArray(new Path[0]));
        breadCrumbBar.setSelectedCrumb(model);
    }

    private void escapePressed(KeyEvent keyEvent) {
        if (dblCickState == EGalleryClickState.ZOOM) {
            displayNext(photo.getMediaFile());
        } else if (dblCickState.isImage()) {
            displayGallery(photo.getMediaFile());
        }
        keyEvent.consume();
    }

    private void leftPressed(KeyEvent keyEvent) {
        if (dblCickState.isImage() && photo.getMediaFile() != null) {
            gridView.getLeft(photo.getMediaFile()).ifPresent(mf -> {
                // TODO: Test by adding to selection ?
                taskService.sendEvent(PhotoSelectedEvent.builder()
                                                        .mf(mf)
                                                        .type(PhotoSelectedEvent.ESelectionType.SELECTED)
                                                        .source(this)
                                                        .build());
                mediaLoader.getOrLoadImage(mf);
            });
        }
        keyEvent.consume();
    }

    private void rightPressed(KeyEvent keyEvent) {
        if (dblCickState.isImage() && photo.getMediaFile() != null) {
            gridView.getRight(photo.getMediaFile())
                    .ifPresent(mf -> {
                        // TODO: Test by adding to selection ?
                        taskService.sendEvent(PhotoSelectedEvent.builder()
                                                                .mf(mf)
                                                                .type(PhotoSelectedEvent.ESelectionType.SELECTED)
                                                                .source(this)
                                                                .build());
                        mediaLoader.getOrLoadImage(mf);
                    });
        }
        keyEvent.consume();
    }

    private void skipPressed(KeyEvent keyEvent) {
        keyEvent.consume();
    }

    private void kPressed(KeyEvent keyEvent) {
        log.info("'K' pressed: {}", keepOrThrowLabel.getOpacity());
        if (keepOrThrowLabel.getOpacity() == 0.4) {
            keepOrThrowLabel.setOpacity(1);
        } else {
            keepOrThrowLabel.setOpacity(0.4);
        }
//        keepOrThrowLabel.setDisable(!keepOrThrowLabel.isDisable());
    }

    public void expandGridCell(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 1) {
            Label l = (Label) mouseEvent.getSource();
            if (expandCell.getValue()) {
                l.getGraphic().setId("grid-cell-expand");
            } else {
                l.getGraphic().setId("grid-cell-collapse");
            }
            expandCell.set(!expandCell.getValue());
        }
    }

    public void keepOrThrowClick(MouseEvent mouseEvent) {
        kPressed(null);
    }

    @Override
    public StackPane getRootContent() {
        return root;
    }
}
