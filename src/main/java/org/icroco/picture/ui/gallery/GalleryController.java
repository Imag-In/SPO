package org.icroco.picture.ui.gallery;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
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
import org.icroco.picture.ui.persistence.PersistenceService;
import org.icroco.picture.ui.pref.UserPreferenceService;
import org.icroco.picture.ui.task.TaskService;
import org.icroco.picture.ui.util.CustomGridView;
import org.icroco.picture.ui.util.MediaLoader;
import org.icroco.picture.ui.util.Nodes;
import org.icroco.picture.ui.util.ZoomDragPane;
import org.slf4j.Logger;
import org.springframework.context.event.EventListener;
import org.springframework.lang.Nullable;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@FxViewBinding(id = "gallery", fxmlLocation = "gallery.fxml")
@RequiredArgsConstructor
public class GalleryController extends FxInitOnce {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(GalleryController.class);

    private final MediaLoader           mediaLoader;
    private final UserPreferenceService pref;
    private final PersistenceService    persistenceService;
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

    private final BooleanProperty           expandCell     = new SimpleBooleanProperty(false);
    private final ObservableList<MediaFile> images         = FXCollections.observableArrayList(MediaFile.extractor());
    private final FilteredList<MediaFile>   filteredImages = new FilteredList<>(images);
    private final SortedList<MediaFile>     sortedImages   = new SortedList<>(filteredImages);

    private double gridCellWidth;
    private double gridCellHeight;

    private Optional<Catalog> currentCatalog = Optional.empty();

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
        gridView.setCellWidth(gridCellWidth);
        gridView.setCellHeight(gridCellHeight);
        gridView.setCellFactory(new MediaFileGridCellFactory(mediaLoader, taskService, expandCell));
        gridView.setOnZoom(this::zoomOnGrid);
        gridView.setOnZoomStarted(this::zoomStart);
        gridView.setOnZoomFinished(this::zoomFinish);

        carouselIcons.setCellFactory(new MediaFileListCellFactory(mediaLoader, taskService));
        carouselIcons.setItems(sortedImages);
        carouselIcons.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        carouselIcons.getSelectionModel().selectedItemProperty().addListener(this::carouselItemSelected);
        carouselIcons.setFixedCellSize(128);
        carouselIcons.prefWidthProperty().bind(gridView.cellWidthProperty());
        carouselIcons.prefHeightProperty().bind(gridView.cellHeightProperty().add(20));

        photo = new ZoomDragPane(photoContainer);
        photoContainer.getChildren().add(photo);
//        photo.fitHeightProperty().bind(photoContainer.heightProperty().subtract(10));
//        photo.fitWidthProperty().bind(photoContainer.widthProperty().subtract(10));

        zoomThumbnails.valueProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
            gridView.setCellWidth(gridCellWidth + 3 * newValue.doubleValue());
            gridView.setCellHeight(gridCellHeight + 3 * newValue.doubleValue());
            carouselIcons.setFixedCellSize(gridView.getCellWidth());
        });
        nbImages.textProperty().bind(Bindings.size(images).map(String::valueOf));
        breadCrumbBar.setCrumbFactory(item -> new BreadCrumbBar.BreadCrumbButton(item.getValue() != null ? item.getValue().getFileName().toString() : ""));
        breadCrumbBar.setAutoNavigationEnabled(false);
//        breadCrumbBar.setOnCrumbAction(bae -> log.info("You just clicked on '" + bae.getSelectedCrumb() + "'!"));

//        photo.setOnMouseClicked(event -> {
//            if (event.getClickCount() == 2) {
//                taskService.notifyLater(CarouselEvent.builder().source(this).eventType(CarouselEvent.EventType.HIDE).build());
//            }
//        });

        expand.setUserData(expandCell);
        gridView.addScrollAndKeyhandler();
        expandCell.addListener((observable, oldValue, newValue) -> gridView.refreshItems());
        Platform.runLater(() -> gridView.requestFocus());
    }


    private void zoomFinish(ZoomEvent event) {
//        log.info("Zoom Finished: {}", event);
    }

    private void zoomStart(ZoomEvent event) {
//        log.info("Zoom Start: {}", event);
    }

    private void zoomOnGrid(ZoomEvent event) {
        final var ratio = event.getTotalZoomFactor() / event.getZoomFactor();
        final var zoomValue = ratio >= 1
                              ? Math.min(100.0, Math.round(Math.max(zoomThumbnails.getValue(), 10) * ratio))
                              : Math.max(0D, Math.floor(zoomThumbnails.getValue() * event.getTotalZoomFactor()));
        log.info("type: {}, factor: {}, totalFactor: {}, ratio: {}, zoomValue: {}",
                 event.getEventType(), event.getZoomFactor(), event.getTotalZoomFactor(), ratio, (int) zoomValue);
        Platform.runLater(() -> zoomThumbnails.setValue((int) zoomValue));

        event.consume();
    }

    @EventListener(CatalogEvent.class)
    public void updateImages(CatalogEvent event) {
        log.info("CatalogEvent: {}, Add images to grid view: {}", event.getType(), event.getCatalog().medias().size());
        images.clear();
        filteredImages.setPredicate(null);

        switch (event.getType()) {
            case DELETED -> {
                breadCrumbBar.setSelectedCrumb(null);
                currentCatalog = Optional.empty();
            }
            case SELECTED, CREATED, UPDATED -> {
                currentCatalog = Optional.of(event.getCatalog());
                resetBcbModel(event.getCatalog().path(), null);
                images.addAll(event.getCatalog().medias());
            }
        }
    }

    @EventListener(CatalogEntrySelectedEvent.class)
    public void updateImages(CatalogEntrySelectedEvent event) {
        log.debug("Entry selected: root: {}, entry: {}", event.getRoot(), event.getEntry());
        resetBcbModel(event.getRoot(), event.getEntry());
        final var path = event.getRoot().resolve(event.getEntry());
        filteredImages.setPredicate(mediaFile -> mediaFile.fullPath().startsWith(path));
    }

    @EventListener(PhotoSelectedEvent.class)
    public void updatePhotoSelected(PhotoSelectedEvent event) {
        final var source = Optional.ofNullable(event.getSource()).orElse(MediaFileListCellFactory.class).getClass();
        final var mf     = event.getFile();
        log.debug("Photo selected: root: {}, from: {}", mf.getFileName(), source.getSimpleName());
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
        switch (event.getEventType()) {
            case SHOW -> {
                gallery.setVisible(false);
                carousel.setVisible(true);
                photo.setImage(mediaLoader.loadImage(event.getMediaFile().getFullPath()));
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
        photo.setImage(mediaLoader.loadImage(newValue.getFullPath()));
    }
}
