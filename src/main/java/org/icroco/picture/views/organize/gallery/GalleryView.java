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
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TreeItem;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.icroco.javafx.StageReadyEvent;
import org.icroco.picture.event.*;
import org.icroco.picture.model.MediaCollection;
import org.icroco.picture.model.MediaFile;
import org.icroco.picture.model.Thumbnail;
import org.icroco.picture.persistence.PersistenceService;
import org.icroco.picture.views.FxEventListener;
import org.icroco.picture.views.organize.PathSelection;
import org.icroco.picture.views.pref.UserPreferenceService;
import org.icroco.picture.views.task.TaskService;
import org.icroco.picture.views.util.*;
import org.icroco.picture.views.util.widget.Zoom;
import org.icroco.picture.views.util.widget.ZoomDragPane;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.slf4j.Logger;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

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


    @PostConstruct
    protected void postConstruct() {
        root.getStyleClass().add("v-gallery");
        root.setId("gallery");
        gridView = new CustomGridView<>(taskService, FXCollections.emptyObservableList());
        gridView.addScrollAndKeyhandler();
        root.setMinSize(350, 250);
        log.info("GalleryView: gridCellWidth: {}, gridCellHeight: {}, hCellSpacing: {}, vCellSpacing: {}",
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
        gridView.cellWidthProperty().addListener((observable, oldValue, newValue) -> log.info("Grid Cell Width: {}", newValue));

        gridView.setCellWidth(128);
        gridView.setCellHeight(128);
        gridView.setCache(true);
        gridView.setCacheHint(CacheHint.SPEED);
        Optional.ofNullable(pref.getUserPreference().getGrid().getGridZoomFactor()).ifPresent(this::applyGridCellWidthFactor);
        gridView.setHorizontalCellSpacing(0D);
        gridView.setVerticalCellSpacing(0D);
        gridView.setCellFactory(new MediaFileGridCellFactory(mediaLoader, taskService, expandCell, this::cellDoubleClick));
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

        addInputMap(carousel, sequence(consume(keyPressed(KeyCode.ESCAPE), this::escapePressed)));
        addInputMap(carousel, sequence(consume(keyPressed(KeyCode.LEFT), this::leftPressed)));
        addInputMap(carousel, sequence(consume(keyPressed(KeyCode.RIGHT), this::rightPressed)));
        addInputMap(carousel, sequence(consume(keyPressed(KeyCode.UP), this::skipPressed)));
        addInputMap(carousel, sequence(consume(keyPressed(KeyCode.DOWN), this::skipPressed)));

//        photo.fitHeightProperty().bind(photoContainer.heightProperty().subtract(10));
//        photo.fitWidthProperty().bind(photoContainer.widthProperty().subtract(10));

        breadCrumbBar.setCrumbFactory(item -> new Hyperlink(item.getValue().getFileName().toString()));
        breadCrumbBar.setAutoNavigationEnabled(false);
        breadCrumbBar.setOnCrumbAction(bae -> log.info("You just clicked on '" + bae.getSelectedCrumb() + "'!"));
        breadCrumbBar.setDividerFactory(GalleryView::bcbDividerFactory);

        expandCell.addListener((observable, oldValue, newValue) -> gridView.refreshItems());

        gallery.setCenter(gridView);
        gallery.setBottom(createBottomBar());
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
        var mf = ((MediaFileGridCell) event.getSource()).getItem();
        if (mf != null && event.getClickCount() == 1) {
            gridView.getSelectionModel().set(cell);
            cell.requestLayout();
        } else if (event.getClickCount() == 2) {
            gridView.getSelectionModel().set(cell);
            displayNext(mf);
        }
        event.consume();
    }

    HBox createBottomBar() {
        HBox bar = new HBox();
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().setAll("tool-bar");
        bar.setId("gallery-toolbar");

        Label expand = new Label();
        expand.setPrefHeight(10D);
        FontIcon icon = new FontIcon();
//        icon.setIconSize(32);
        icon.setId("fitGridCell");
        expand.setGraphic(icon);
        expand.setOnMouseClicked(this::expandGridCell);


//        zoomThumbnails.getStyleClass().add(Styles.SMALL);
        zoomThumbnails.setSkin(new ProgressSliderSkin(zoomThumbnails));
        Optional.ofNullable(pref.getUserPreference().getGrid().getGridZoomFactor()).ifPresent(zoomThumbnails::setValue);
        zoomThumbnails.setBlockIncrement(1);
        zoomThumbnails.valueProperty()
                      .addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
                          zoomLevel = newValue.intValue();
                          log.info("Zoom Level: {}", zoomLevel);
                          applyGridCellWidthFactor(zoomLevel);
                          pref.getUserPreference().getGrid().setGridZoomFactor(zoomLevel);
//                          gridView.setCellWidth(gridCellWidth + 10 * zoomLevel);
//                          gridView.setCellHeight(gridCellHeight + 10 * zoomLevel);
//            carouselIcons.setFixedCellSize(gridView.getCellWidth());
                      });
        HBox.setHgrow(breadCrumbBar, Priority.ALWAYS);
        Label nbImages = new Label();
        nbImages.textProperty().bind(Bindings.size(images).map(number -> number + " files"));

        bar.getChildren().addAll(expand, zoomThumbnails, breadCrumbBar, new Spacer(), nbImages);

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
    public void stageReady(StageReadyEvent event) {
        var width = Math.max(gridView.getCellWidth(), (root.getWidth() - 100) / (gridView.getItemsInRow() - 1));
        width = Math.max(128, root.getWidth() / 8);

        log.info("Parent width: {}, nbColumn: {}, currentWidth: {}, newWidth: {}",
                 gridView.getWidth(),
                 gridView.getItemsInRow(),
                 gridView.getCellWidth(),
                 width);
//        gridView.cellWidthProperty().bind(Bindings.max(128, Bindings.divide(root.widthProperty(), 8)));

//        root.widthProperty().addListener((observable, oldValue, newValue) -> {
//            gridCellWidth = Math.max(128, newValue.doubleValue() / 8);
////            log.info("Parent width: {}, cell width: {}", newValue, gridCellWidth);
////            gridView.setCellHeight(gridCellWidth);
////            gridView.setCellWidth(gridCellWidth);
//        });

//        gridCellHeight = gridCellWidth = width;
//        gridView.setCellWidth(width);
//        gridView.setCellHeight(width);
        gridView.requestFocus();
    }

    @FxEventListener
    public void updateImages(CollectionEvent event) {
        // FIXME: Confilt with dir scanning.
        MediaCollection mediaCollection = event.getMediaCollection();
        log.info("CollectionEvent type {}, Collection: {}, mediaFiles size: {}",
                 event.getType(),
                 mediaCollection.id(),
                 mediaCollection.medias().size());

        switch (event.getType()) {
            case DELETED -> {
                currentCatalog.set(null);
                // TODO: clear thumbnail cache ?
            }
            case SELECTED -> {
                currentCatalog.set(mediaCollection);
                // TODO: Warm thumbnail cache ?
            }
            case READY -> {
                // Ingore right now.
            }
        }
    }

    @FxEventListener
    public void updateImages(CollectionUpdatedEvent event) {
        log.info("Recieved update on collection: '{}', newItems: '{}', deletedItems: '{}'",
                 event.getMediaCollectionId(),
                 event.getNewItems().size(),
                 event.getDeletedItems().size());
        images.addAll(event.getNewItems());
        images.removeAll(event.getDeletedItems());
    }


    public void collectionPathChange(ObservableValue<? extends PathSelection> observable, PathSelection oldValue, PathSelection newValue) {
        dblCickState = EGalleryClickState.GALLERY;
        if (newValue.equalsNoSeed(oldValue)) {
            log.info("MediaCollection subpath re-selected: root: {}, entry: {}", newValue.mediaCollectionId(), newValue.subPath());
            displayGallery(null);
//            gallery.setOpacity(0);
//             Animations.fadeIn(gallery, Duration.millis(1000)).playFromStart();
        } else {
            log.info("MediaCollection subpath selected: root: {}, entry: {}", newValue.mediaCollectionId(), newValue.subPath());
            currentCatalog.setValue(persistenceService.getMediaCollection(newValue.mediaCollectionId()));
            resetBcbModel(newValue.subPath());
            final var path = getCurrentCatalog().path().resolve(newValue.subPath());
            filteredImages.setPredicate(mediaFile -> mediaFile.fullPath().startsWith(path));
//        mediaLoader.warmThumbnailCache(getCurrentCatalog(), filteredImages);
//            gridView.getFirstVisible().ifPresent(mediaFileCell -> gridView.getSelectionModel().set(mediaFileCell));
        }
    }

    private MediaCollection getCurrentCatalog() {
        return Optional.ofNullable(currentCatalog.get())
                       .orElseThrow(() -> new IllegalStateException("Technical error, current catalog is not set"));
    }

    @FxEventListener
    public void updatePhotoSelected(PhotoSelectedEvent event) {
        if (event.getType() == PhotoSelectedEvent.ESelectionType.SELECTED) {
//            log.info("GridView item in row: {}", gridView.getItemsInRow());
            final var source = Optional.ofNullable(event.getSource()).orElse(MediaFileListCellFactory.class).getClass();
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
            TreeItem<Path> root    = Nodes.getRoot(breadCrumbBar.getSelectedCrumb());
            Path           subPath = root.getValue().relativize(mf.getFullPath());
            resetBcbModel(subPath);
        } else {
//            TreeItem<Path> root = Nodes.getRoot(breadCrumbBar.getSelectedCrumb());
//            resetBcbModel(null);
        }
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

    private void resetBcbModel(@Nullable final Path entry) {
        log.debug("Reset CB: {}", entry);
        Path[] paths;
        if (getCurrentCatalog().path().equals(entry)) {
            paths = new Path[] { getCurrentCatalog().path() };
        } else {
            paths = Stream.concat(Stream.of(getCurrentCatalog().path()),
                                  entry == null
                                  ? Stream.empty()
                                  : Collections.toStream(entry.iterator()))
                          .toArray(Path[]::new);
        }
        Breadcrumbs.BreadCrumbItem<Path> model = Breadcrumbs.buildTreeModel(paths);
        breadCrumbBar.setSelectedCrumb(model);
    }

    private void escapePressed(KeyEvent keyEvent) {
        log.info("Carousel Escape");
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


    public void expandGridCell(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 1) {
            Label l = (Label) mouseEvent.getSource();
            if (expandCell.getValue()) {
                l.getGraphic().setId("fitGridCell");
            } else {
                l.getGraphic().setId("fas-expand-alt");
            }
            expandCell.set(!expandCell.getValue());
        }
    }

//    @FxEventListener
//    public void showCarousel(CarouselEvent event) {
//        log.debug("Receive Carousel Event: {},", event.getEventType());
//        // TODO: Add visual animations;
//        switch (event.getEventType()) {
//            case SHOW -> {
//                gallery.setVisible(false);
//                photo.setImage(null, null);
//                carousel.setVisible(true);
//                mediaLoader.getOrLoadImage(event.getMediaFile());
//            }
//            case HIDE -> {
//                gallery.setVisible(true);
//                carousel.setVisible(false);
//                photo.setImage(null, null);
//                // TODO: Jump to previous item.
////                gridView.getSelectionModel().clear();
////                gridView.ensureVisible(event.getMediaFile());
//            }
//        }
//    }

    //    @FxEventListener
    public void refreshGrid(GalleryRefreshEvent event) {
        if (event.getMediaCollectionId() == Optional.ofNullable(currentCatalog.get())
                                                    .map(MediaCollection::id)
                                                    .orElse(event.getMediaCollectionId())) {
            log.info("Refresh collection id: '{}'", event.getMediaCollectionId());
            MediaCollection mc = persistenceService.getMediaCollection(event.getMediaCollectionId());
            images.clear();
            images.addAll(mc.medias());
            currentCatalog.set(mc);
//                gridView.refreshItems();
        }
    }

    @Override
    public StackPane getRootContent() {
        return root;
    }
}
