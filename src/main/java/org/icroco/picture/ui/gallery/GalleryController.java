package org.icroco.picture.ui.gallery;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.BorderPane;
import lombok.RequiredArgsConstructor;
import org.controlsfx.control.BreadCrumbBar;
import org.controlsfx.control.GridView;
import org.icroco.javafx.FxInitOnce;
import org.icroco.javafx.FxViewBinding;
import org.icroco.picture.ui.event.CatalogEntrySelectedEvent;
import org.icroco.picture.ui.event.CatalogEvent;
import org.icroco.picture.ui.model.MediaFile;
import org.icroco.picture.ui.persistence.PersistenceService;
import org.icroco.picture.ui.pref.UserPreferenceService;
import org.icroco.picture.ui.util.MediaLoader;
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

    @FXML
    public  Label               nbImages;
    @FXML
    public  Slider              zoomThumbnails;
    @FXML
    public  BorderPane          layout;
    @FXML
    public  BreadCrumbBar<Path> breadCrumbBar;
    @FXML
    private GridView<MediaFile> gridView;

    private final ObservableList<MediaFile> images         = FXCollections.observableArrayList(MediaFile.extractor());
    private final FilteredList<MediaFile>   filteredImages = new FilteredList<>(images);
    private final SortedList<MediaFile>     sortedImages   = new SortedList<>(filteredImages);

    private double gridCellWidth;
    private double gridCellHeight;

    @Override
    protected void initializedOnce() {
        log.info("GalleryView: gridCellWidth: {}, gridCellHeight: {}, hCellSpacing: {}, vCellSpacing: {}",
                 gridView.getCellWidth(),
                 gridView.getCellHeight(),
                 gridView.getHorizontalCellSpacing(),
                 gridView.getVerticalCellSpacing());
        gridView.setItems(sortedImages);
//        gridCellWidth = Optional.ofNullable(pref.getUserPreference().getGrid().getCellWidth()).orElse((int)gridView.getCellWidth());
//        gridCellHeight = Optional.ofNullable(pref.getUserPreference().getGrid().getCellHeight()).orElse((int)gridView.getCellHeight());
        gridCellWidth = gridView.getCellWidth() * 2;
        gridCellHeight = gridView.getCellHeight() * 2;
        gridView.setCellWidth(gridCellWidth);
        gridView.setCellHeight(gridCellHeight);
//        gridView.setCellFactory(gv -> new ImageGridCell());
        gridView.setCellFactory(new MediaFileGridCellFactory(mediaLoader));
        zoomThumbnails.valueProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
            gridView.setCellWidth(gridCellWidth + 3 * newValue.doubleValue());
            gridView.setCellHeight(gridCellHeight + 3 * newValue.doubleValue());
        });
        nbImages.textProperty().bind(Bindings.size(images).map(String::valueOf));
        breadCrumbBar.setCrumbFactory(item -> new BreadCrumbBar.BreadCrumbButton(item.getValue() != null ? item.getValue().getFileName().toString() : ""));
        breadCrumbBar.setAutoNavigationEnabled(false);
        breadCrumbBar.setOnCrumbAction(bae -> log.info("You just clicked on '" + bae.getSelectedCrumb() + "'!"));

        Platform.runLater(() -> {
            gridView.requestFocus();
        });
    }

    @EventListener(CatalogEvent.class)
    public void updateImages(CatalogEvent event) {
        log.info("Event: {}, Add images to grid view: {}", event.getType(), event.getCatalog().medias().size());
        images.clear();
        filteredImages.setPredicate(null);

        switch (event.getType()) {
            case DELETED -> {
                breadCrumbBar.setSelectedCrumb(null);
            }
            case SELECTED, CREATED, UPDATED -> {
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

}
