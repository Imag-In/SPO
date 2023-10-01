package org.icroco.picture.views.organize.gallery;

import atlantafx.base.controls.Breadcrumbs;
import atlantafx.base.controls.ProgressSliderSkin;
import atlantafx.base.controls.Spacer;
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
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TreeItem;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import lombok.RequiredArgsConstructor;
import org.icroco.javafx.StageReadyEvent;
import org.icroco.picture.event.*;
import org.icroco.picture.model.MediaCollection;
import org.icroco.picture.model.MediaFile;
import org.icroco.picture.model.Thumbnail;
import org.icroco.picture.persistence.PersistenceService;
import org.icroco.picture.views.FxEventListener;
import org.icroco.picture.views.organize.OrganizeConfiguration;
import org.icroco.picture.views.pref.UserPreferenceService;
import org.icroco.picture.views.task.TaskService;
import org.icroco.picture.views.util.Collections;
import org.icroco.picture.views.util.*;
import org.icroco.picture.views.util.widget.Zoom;
import org.icroco.picture.views.util.widget.ZoomDragPane;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static javafx.application.Platform.runLater;
import static org.fxmisc.wellbehaved.event.EventPattern.keyPressed;
import static org.fxmisc.wellbehaved.event.InputMap.consume;
import static org.fxmisc.wellbehaved.event.InputMap.sequence;

@Component
@RequiredArgsConstructor
public class GalleryView implements FxView<StackPane> {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(GalleryView.class);

    private final MediaLoader           mediaLoader;
    private final UserPreferenceService pref;
    private final TaskService           taskService;
    @Qualifier(OrganizeConfiguration.GALLERY_ZOOM)
    private final ZoomDragPane          photo;
    private final PersistenceService    persistenceService;

    private final StackPane         root           = new StackPane();
    private final BorderPane        gallery        = new BorderPane();
    private final BorderPane        carousel       = new BorderPane();
    private final Slider            zoomThumbnails = createTickSlider();
    //    private final BreadCrumbBar<Path>       breadCrumbBar  = new BreadCrumbBar<>();
    private final Breadcrumbs<Path> breadCrumbBar  = new Breadcrumbs<>();

    private       CustomGridView<MediaFile>             gridView;
    //    private       ZoomDragPane                          photo;
    //    @FXML
//    private ImageView              photo;
    private final StackPane                             photoContainer = new StackPane();
    //    private final ListView<MediaFile>                   carouselIcons  = new ListView<>();
    private final BooleanProperty                       expandCell     = new SimpleBooleanProperty(true);
    private final ObservableList<MediaFile>             images         = FXCollections.observableArrayList(MediaFile.extractor());
    private final FilteredList<MediaFile>               filteredImages = new FilteredList<>(images);
    private final SortedList<MediaFile>                 sortedImages   = new SortedList<>(filteredImages);
    private       double                                gridCellWidth;
    private       double                                gridCellHeight;
    private       double                                zoomLevel      = 0;
    private final SimpleObjectProperty<MediaCollection> currentCatalog = new SimpleObjectProperty<>(null);

    @PostConstruct
    protected void postConstruct() {
        root.getStyleClass().add("v-gallery");
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
        gridCellWidth = 128; //gridView.getCellWidth() * 2;
        gridCellHeight = 128; //gridView.getCellHeight() * 2;
        gridView.setCache(true);
        gridView.setCacheHint(CacheHint.SPEED);
        gridView.setCellWidth(gridCellWidth);
        gridView.setCellHeight(gridCellHeight);
        gridView.setHorizontalCellSpacing(0D);
        gridView.setVerticalCellSpacing(0D);
        gridView.setCellFactory(new MediaFileGridCellFactory(mediaLoader, taskService, expandCell));
        gridView.setOnZoom(this::zoomOnGrid);
        currentCatalog.addListener(this::collectionChanged);

//        carouselIcons.setCellFactory(new MediaFileListCellFactory(mediaLoader, taskService));
//        carouselIcons.setItems(sortedImages);
//        carouselIcons.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
//        carouselIcons.getSelectionModel().selectedItemProperty().addListener(this::carouselItemSelected);
//        carouselIcons.setFixedCellSize(128);
//        carouselIcons.prefWidthProperty().bind(gridView.cellWidthProperty());
//        carouselIcons.prefHeightProperty().bind(gridView.cellHeightProperty().add(20));

//        photo = new ZoomDragPane(photoContainer);
        photo.setOnMouseClicked(this::onImageClick);
//        photo.setFocusTraversable(true);
        photoContainer.getChildren().add(photo);
        photo.getView().setFocusTraversable(true);

        carousel.setCenter(photoContainer);
//        carousel.setBottom(carouselIcons);

        org.fxmisc.wellbehaved.event.Nodes.addInputMap(photo.getView(),
                                                       sequence(consume(keyPressed(KeyCode.ESCAPE), this::escapePressed)
                                                       ));
//        photoContainer.setOnKeyPressed(new EventHandler<KeyEvent>() {
//            public void handle(final KeyEvent keyEvent) {
//                if (keyEvent.getCode() == KeyCode.ESCAPE) {
//                    taskService.fxNotifyLater(CarouselEvent.builder().source(this).mediaFile(null).eventType(CarouselEvent.EventType.HIDE).build());
//                }
//            }
//        });

//        photo.fitHeightProperty().bind(photoContainer.heightProperty().subtract(10));
//        photo.fitWidthProperty().bind(photoContainer.widthProperty().subtract(10));


//        breadCrumbBar.setCrumbFactory(item -> {
//            var btn = new Button(item.getValue().getFileName().toString());
//            btn.getStyleClass().add(Styles.FLAT);
//            btn.setFocusTraversable(false);
//            return btn;
//
//        });
        breadCrumbBar.setCrumbFactory(item -> new Hyperlink(item.getValue().getFileName().toString()));

//        breadCrumbBar.setCrumbFactory(item -> new BreadCrumbBar.BreadCrumbButton(item.getValue() != null ? item.getValue().getFileName().toString() : ""));
        breadCrumbBar.setAutoNavigationEnabled(false);
        breadCrumbBar.setOnCrumbAction(bae -> log.info("You just clicked on '" + bae.getSelectedCrumb() + "'!"));

        breadCrumbBar.setDividerFactory(item -> {
            if (item == null) {
                return new Label("", new FontIcon(Material2AL.HOME));
            }
            return !item.isLast()
                   ? new Label("", new FontIcon(Material2AL.CHEVRON_RIGHT))
                   : null;
        });

//        gridView.addScrollAndKeyhandler();
        expandCell.addListener((observable, oldValue, newValue) -> gridView.refreshItems());

        gallery.setCenter(gridView);
        gallery.setBottom(createBottomBar());
        carousel.setVisible(false);
        root.getChildren().addAll(gallery, carousel);

//        runLater(() -> gridView.requestFocus());
    }


    HBox createBottomBar() {
        HBox bar = new HBox();
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().setAll("tool-bar");

        Label expand = new Label();
        expand.setPrefHeight(10D);
        FontIcon icon = new FontIcon();
//        icon.setIconSize(32);
        icon.setId("fitGridCell");
        expand.setGraphic(icon);
        expand.setOnMouseClicked(this::expandGridCell);

//        zoomThumbnails.getStyleClass().add(Styles.SMALL);
        zoomThumbnails.setSkin(new ProgressSliderSkin(zoomThumbnails));
        zoomThumbnails.valueProperty()
                      .addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
                          zoomLevel = newValue.doubleValue();
                          gridView.setCellWidth(gridCellWidth + 10 * zoomLevel);
                          gridView.setCellHeight(gridCellHeight + 10 * zoomLevel);
//            carouselIcons.setFixedCellSize(gridView.getCellWidth());
                      });
        HBox.setHgrow(breadCrumbBar, Priority.ALWAYS);
        Label nbImages = new Label();
        nbImages.textProperty().bind(Bindings.size(images).map(number -> number + " files"));

        bar.getChildren().addAll(expand, zoomThumbnails, breadCrumbBar, new Spacer(), nbImages);

        return bar;
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
        log.info("CLICK: {}", event);
        if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
            photo.requestFocus();
            if (photo.isZoomed()) {
                // TODO: Zoom from mouse coordinate, not center.
                photo.zoom(event);
            } else {
                photo.noZoom();
            }
            event.consume();
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
        var width = Math.max((int) gridView.getCellWidth(), (int) root.getWidth() / gridView.getItemsInRow());
        log.info("Parent width: {}, nbColumn: {}, currentWidth: {}, newWidth: {}",
                 gridView.getWidth(),
                 gridView.getItemsInRow(),
                 gridView.getCellWidth(),
                 width);
        gridCellHeight = width;
        gridCellWidth = width;
        gridView.setCellHeight(width);
        gridView.setCellWidth(width);
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

    @FxEventListener
    public void updateImages(CollectionSubPathSelectedEvent event) {
        log.info("MediaCollection subpath selected: root: {}, entry: {}", event.getCollectionId(), event.getEntry());
        currentCatalog.setValue(persistenceService.getMediaCollection(event.getCollectionId()));
        resetBcbModel(event.getEntry());
        final var path = getCurrentCatalog().path().resolve(event.getEntry());
        filteredImages.setPredicate(mediaFile -> mediaFile.fullPath().startsWith(path));
//        mediaLoader.warmThumbnailCache(getCurrentCatalog(), filteredImages);
        gridView.getSelectionModel().clear();
        escapePressed(null);
        pref.getUserPreference().setLastViewed(event.getCollectionId(), event.getEntry());
//        gridView.getFirstCellVisible().ifPresent(mediaFile -> gridView.getSelectionModel().add(mediaFile));
//        taskService.sendEvent(CarouselEvent.builder().source(this).mediaFile(null).eventType(CarouselEvent.EventType.HIDE).build());
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
        photo.setImage(event.getImage());
        photo.getMaskerPane().stop();
    }

    Task<List<MediaFile>> fillGallery(final List<MediaFile> files) {
        return new Task<>() {
//            StopWatch w = new StopWatch("Load Task");

            @Override
            protected List<MediaFile> call() throws Exception {
                var size          = files.size();
                var imagesResults = new ArrayList<>(files);
//                w.start("Build Thumbnail");
                updateMessage("Loading '" + size + " images");
                updateProgress(0, size);

                for (int i = 0, filesSize = files.size(); i < filesSize; i++) {
                    MediaFile mediaFile = files.get(i);
//                    mediaLoader.loadThumbnail(mediaFile);
//                    if (!mediaFile.thumbnail().isLoaded()) {
//                        if (mediaFile.thumbnail().getImage() == null) {
//                            mediaFile.thumbnail().setImage(mediaLoader.loadThumbnail(mediaFile.fullPath()));
//                        } else {
//                            mediaLoader.updateCache(mediaFile.fullPath(), mediaFile.thumbnail().getImage());
//                        }
//                    }
                    updateProgress(i, size);
                }


//                for (int i = 0; i < size; i++) {
//                    final var mf = files.get(i);
//                    imagesResults.add(mf);
//                    mf.thumbnail().setLoaded(true);
//                    mf.thumbnail().setImage(mediaLoader.loadThumbnail(mf.fullPath()));
//
////                    imagesResults.add(new MediaFile(mf.id(),
////                                                    mf.fullPath(),
////                                                    mf.fileName(),
////                                                    mf.originalDate(),
////                                                    mf.tags(),
////                                                    new Thumbnail(mediaLoader.loadThumbnail(mf.fullPath()), true)));
//                    updateProgress(i, size);
//                }
//                w.stop();
//                w.start("Save Task into DB");
//                persistenceService.saveMediaFiles(imagesResults);
//                w.stop();
//                log.info("PERF: {}", w.prettyPrint());

                return imagesResults;
            }

            @Override
            protected void succeeded() {
                images.addAll(getValue()
                                      .stream()
                                      .filter(Objects::nonNull)
                                      .toList());
            }

            @Override
            protected void failed() {
                try {
                    throw getException();
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private void resetBcbModel(@Nullable final Path entry) {
        log.debug("Reset CB: {}", entry);
        Path[] paths;
        if (getCurrentCatalog().path().equals(entry)) {
            paths = new Path[]{getCurrentCatalog().path()};
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

    private void escapePressed(KeyEvent event) {
        taskService.sendEvent(CarouselEvent.builder()
                                           .mediaFile(null)
                                           .eventType(CarouselEvent.EventType.HIDE)
                                           .source(this)
                                           .build());
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
//            log.info("expand");
//            FontIcon fi = new FontIcon("fas-expand-alt");
//            fi.setId("fas-expand-alt");
//            expand.setGraphic(fi);
        }
    }

    @FxEventListener
    public void showCarousel(CarouselEvent event) {
        log.debug("Receive Carousel Event: {},", event.getEventType());
        // TODO: Add visual animations;
        switch (event.getEventType()) {
            case SHOW -> {
                gallery.setVisible(false);
                carousel.setVisible(true);
//                photo.setImage(mediaLoader.loadImage(event.getMediaFile()));
                mediaLoader.getOrLoadImage(event.getMediaFile());
//                MultipleSelectionModel<MediaFile> selectionModel = carouselIcons.getSelectionModel();
//                selectionModel.select(event.getMediaFile());
//                var idx = selectionModel.getSelectedIndex();
//                selectionModel.select(Math.max(0, idx-5));
//                carouselIcons.scrollTo(event.getMediaFile());
            }
            case HIDE -> {
                gallery.setVisible(true);
                carousel.setVisible(false);
                // TODO: Jump to previous item.
                gridView.getSelectionModel().clear();
                gridView.ensureVisible(event.getMediaFile());
//                if (event.getMediaFile() != null) {
//                    gridView.getSelectionModel().add(event.getMediaFile());
//                }
//                event.getMediaFile().setSelected(true);
            }
        }
    }

    private void carouselItemSelected(ObservableValue<? extends MediaFile> observableValue,
                                      MediaFile oldValue,
                                      MediaFile newValue) {
        photo.setImage(mediaLoader.loadImage(newValue));
    }

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
