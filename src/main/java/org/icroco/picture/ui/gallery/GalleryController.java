package org.icroco.picture.ui.gallery;

import javafx.application.Platform;
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
import javafx.fxml.FXML;
import javafx.scene.CacheHint;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import lombok.RequiredArgsConstructor;
import org.controlsfx.control.BreadCrumbBar;
import org.icroco.javafx.FxInitOnce;
import org.icroco.javafx.FxViewBinding;
import org.icroco.picture.ui.event.CarouselEvent;
import org.icroco.picture.ui.event.CatalogEntrySelectedEvent;
import org.icroco.picture.ui.event.CatalogEvent;
import org.icroco.picture.ui.event.PhotoSelectedEvent;
import org.icroco.picture.ui.model.Catalog;
import org.icroco.picture.ui.model.MediaFile;
import org.icroco.picture.ui.pref.UserPreferenceService;
import org.icroco.picture.ui.task.TaskService;
import org.icroco.picture.ui.util.CustomGridView;
import org.icroco.picture.ui.util.MediaLoader;
import org.icroco.picture.ui.util.Nodes;
import org.icroco.picture.ui.util.widget.Zoom;
import org.icroco.picture.ui.util.widget.ZoomDragPane;
import org.slf4j.Logger;
import org.springframework.context.event.EventListener;
import org.springframework.lang.Nullable;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.fxmisc.wellbehaved.event.EventPattern.keyPressed;
import static org.fxmisc.wellbehaved.event.InputMap.consume;
import static org.fxmisc.wellbehaved.event.InputMap.sequence;

@FxViewBinding(id = "gallery", fxmlLocation = "gallery.fxml")
@RequiredArgsConstructor
public class GalleryController extends FxInitOnce {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(GalleryController.class);

    private final MediaLoader           mediaLoader;
    private final UserPreferenceService pref;
    private final TaskService           taskService;

    @FXML
    private Label                     nbImages;
    @FXML
    private Label                     expand;
    @FXML
    private Slider                    zoomThumbnails;
    @FXML
    private StackPane                 layout;
    @FXML
    private BorderPane                gallery;
    @FXML
    private BorderPane                carousel;
    @FXML
    private BreadCrumbBar<Path>       breadCrumbBar;
    @FXML
    private CustomGridView<MediaFile> gridView;
    private ZoomDragPane              photo;
    //    @FXML
//    private ImageView              photo;
    @FXML
    private StackPane                 photoContainer;
    @FXML
    private ListView<MediaFile>       carouselIcons;

    private final BooleanProperty               expandCell     = new SimpleBooleanProperty(false);
    private final ObservableList<MediaFile>     images         = FXCollections.observableArrayList(MediaFile.extractor());
    private final FilteredList<MediaFile>       filteredImages = new FilteredList<>(images);
    private final SortedList<MediaFile>         sortedImages   = new SortedList<>(filteredImages);
    private       double                        gridCellWidth;
    private       double                        gridCellHeight;
    private       double                        zoomLevel      = 0;
    private final SimpleObjectProperty<Catalog> currentCatalog = new SimpleObjectProperty<>(null);

    @Override
    protected void initializedOnce() {

        log.info("GalleryView: gridCellWidth: {}, gridCellHeight: {}, hCellSpacing: {}, vCellSpacing: {}",
                 gridView.getCellWidth(),
                 gridView.getCellHeight(),
                 gridView.getHorizontalCellSpacing(),
                 gridView.getVerticalCellSpacing());
        sortedImages.setComparator(Comparator.comparing(MediaFile::getOriginalDate));
        gridView.setItems(sortedImages);
//        gridCellWidth = Optional.ofNullable(pref.getUserPreference().getGrid().getCellWidth()).orElse((int)gridView.getCellWidth());
//        gridCellHeight = Optional.ofNullable(pref.getUserPreference().getGrid().getCellHeight()).orElse((int)gridView.getCellHeight());
        gridCellWidth = gridView.getCellWidth() * 2;
        gridCellHeight = gridView.getCellHeight() * 2;
        gridView.setCache(true);
        gridView.setCacheHint(CacheHint.SPEED);
        gridView.setCellWidth(gridCellWidth);
        gridView.setCellHeight(gridCellHeight);
        gridView.setCellFactory(new MediaFileGridCellFactory(mediaLoader, taskService, expandCell));
        gridView.setOnZoom(this::zoomOnGrid);

        carouselIcons.setCellFactory(new MediaFileListCellFactory(mediaLoader, taskService));
        carouselIcons.setItems(images);
        carouselIcons.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        carouselIcons.getSelectionModel().selectedItemProperty().addListener(this::carouselItemSelected);
        carouselIcons.setFixedCellSize(128);
        carouselIcons.prefWidthProperty().bind(gridView.cellWidthProperty());
        carouselIcons.prefHeightProperty().bind(gridView.cellHeightProperty().add(20));

        photo = new ZoomDragPane(photoContainer);
        photo.setOnMouseClicked(this::onPhotoClick);
        photo.setFocusTraversable(true);
        photoContainer.getChildren().add(photo);
        org.fxmisc.wellbehaved.event.Nodes.addInputMap(photo,
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

        zoomThumbnails.setSnapToTicks(true);
        zoomThumbnails.valueProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
            zoomLevel = newValue.doubleValue() / 10;
            gridView.setCellWidth(gridCellWidth + 10 * zoomLevel);
            gridView.setCellHeight(gridCellHeight + 10 * zoomLevel);
            carouselIcons.setFixedCellSize(gridView.getCellWidth());
        });
        nbImages.textProperty().bind(Bindings.size(images).map(String::valueOf));
        breadCrumbBar.setCrumbFactory(item -> new BreadCrumbBar.BreadCrumbButton(item.getValue() != null ? item.getValue().getFileName().toString() : ""));
        breadCrumbBar.setAutoNavigationEnabled(false);
//        breadCrumbBar.setOnCrumbAction(bae -> log.info("You just clicked on '" + bae.getSelectedCrumb() + "'!"));


        expand.setUserData(expandCell);
        gridView.addScrollAndKeyhandler();
        expandCell.addListener((observable, oldValue, newValue) -> gridView.refreshItems());
        Platform.runLater(() -> gridView.requestFocus());
    }

    private void onPhotoClick(MouseEvent event) {
        if (event.getClickCount() == 2) {
            if (carousel.getChildren().contains(carouselIcons)) {
                carousel.getChildren().remove(carouselIcons);
                photo.zoom(event);

            } else {
                carousel.setBottom(carouselIcons);
                photo.noZoom();
            }
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
        Platform.runLater(() -> zoomThumbnails.setValue(zoomLevel));

        event.consume();
    }

    @EventListener(CatalogEvent.class)
    public void updateImages(CatalogEvent event) {
        log.info("CatalogEvent: {}, Add images to grid view: {}", event.getType(), event.getCatalog().medias().size());
        images.clear();
        filteredImages.setPredicate(null);
        currentCatalog.set(event.getCatalog());

        switch (event.getType()) {
            case DELETED -> {
                breadCrumbBar.setSelectedCrumb(null);
                currentCatalog.set(null);
            }
            case SELECTED, CREATED, UPDATED -> {
                resetBcbModel(event.getCatalog().path(), null);
                images.addAll(event.getCatalog().medias());
            }
        }
    }

    @EventListener(CatalogEntrySelectedEvent.class)
    public void updateImages(CatalogEntrySelectedEvent event) {
        log.info("Entry selected: root: {}, entry: {}", event.getRoot(), event.getEntry());
        resetBcbModel(event.getRoot(), event.getEntry());
        final var path = event.getRoot().resolve(event.getEntry());
        filteredImages.setPredicate(mediaFile -> mediaFile.fullPath().startsWith(path));
        mediaLoader.warmThumbnailCache(currentCatalog.get(), filteredImages);
        taskService.fxNotifyLater(CarouselEvent.builder().source(this).mediaFile(null).eventType(CarouselEvent.EventType.HIDE).build());

    }

    @EventListener(PhotoSelectedEvent.class)
    public void updatePhotoSelected(PhotoSelectedEvent event) {
        final var source = Optional.ofNullable(event.getSource()).orElse(MediaFileListCellFactory.class).getClass();
        final var mf     = event.getFile();
        log.info("Photo selected: root: {}, from: {}", mf.getFileName(), source.getSimpleName());
        // TODO: it works with only one item selected.

        TreeItem<Path> root    = Nodes.getRoot(breadCrumbBar.getSelectedCrumb());
        Path           subPath = root.getValue().relativize(mf.getFullPath());
        resetBcbModel(root.getValue(), subPath);
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
                }
                catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private void resetBcbModel(final Path root, @Nullable final Path entry) {
        var paths = Stream.concat(Stream.of(root),
                                  entry == null
                                  ? Stream.empty()
                                  : StreamSupport.stream(Spliterators.spliteratorUnknownSize(entry.iterator(),
                                                                                             Spliterator.ORDERED)
                                          , false))
                          .toArray(Path[]::new);
        TreeItem<Path> model = BreadCrumbBar.buildTreeModel(paths);
        breadCrumbBar.setSelectedCrumb(model);
    }

    private void escapePressed(KeyEvent event) {
        taskService.fxNotifyLater(CarouselEvent.builder().source(this).mediaFile(null).eventType(CarouselEvent.EventType.HIDE).build());
    }

    public void expandGridCell(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 1) {
            var b = (BooleanProperty) expand.getUserData();
            if (b.getValue()) {
                expand.getGraphic().setId("fitGridCell");
            } else {
                expand.getGraphic().setId("fas-expand-alt");
            }
            b.set(!b.getValue());
//            log.info("expand");
//            FontIcon fi = new FontIcon("fas-expand-alt");
//            fi.setId("fas-expand-alt");
//            expand.setGraphic(fi);
        }
    }

    @EventListener(CarouselEvent.class)
    public void showCarousel(CarouselEvent event) {
        log.debug("Receive Carousel Event: {},", event.getEventType());
        // TODO: Add visual animations;
        switch (event.getEventType()) {
            case SHOW -> {
                gallery.setVisible(false);
                carousel.setVisible(true);
                photo.setImage(mediaLoader.loadImage(event.getMediaFile()));
                MultipleSelectionModel<MediaFile> selectionModel = carouselIcons.getSelectionModel();
                selectionModel.select(event.getMediaFile());
//                var idx = selectionModel.getSelectedIndex();
//                selectionModel.select(Math.max(0, idx-5));
                carouselIcons.scrollTo(event.getMediaFile());
            }
            case HIDE -> {
                gallery.setVisible(true);
                carousel.setVisible(false);
                gridView.getSelectionModel().clear();
                gridView.ensureVisible(event.getMediaFile());
//                event.getMediaFile().setSelected(true);
            }
        }
    }

    private void carouselItemSelected(ObservableValue<? extends MediaFile> observableValue, MediaFile oldValue, MediaFile newValue) {
        photo.setImage(mediaLoader.loadImage(newValue));
    }
}
