package org.icroco.picture.views.organize.gallery;

import atlantafx.base.controls.Breadcrumbs;
import atlantafx.base.controls.ModalPane;
import atlantafx.base.controls.ProgressSliderSkin;
import atlantafx.base.controls.Spacer;
import atlantafx.base.util.Animations;
import eu.iamgio.animated.binding.value.AnimatedValueLabel;
import eu.iamgio.animated.common.Curve;
import jakarta.annotation.PostConstruct;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.icroco.picture.event.*;
import org.icroco.picture.model.EKeepOrThrow;
import org.icroco.picture.model.MediaCollection;
import org.icroco.picture.model.MediaFile;
import org.icroco.picture.model.Thumbnail;
import org.icroco.picture.persistence.PersistenceService;
import org.icroco.picture.util.Constant;
import org.icroco.picture.util.I18N;
import org.icroco.picture.util.SceneReadyEvent;
import org.icroco.picture.util.SystemUtil;
import org.icroco.picture.views.FxEventListener;
import org.icroco.picture.views.ViewConfiguration;
import org.icroco.picture.views.organize.OrganizeConfiguration;
import org.icroco.picture.views.organize.PathSelection;
import org.icroco.picture.views.organize.gallery.predicate.PathPredicate;
import org.icroco.picture.views.pref.UserPreferenceService;
import org.icroco.picture.views.task.TaskService;
import org.icroco.picture.views.util.*;
import org.icroco.picture.views.util.widget.Zoom;
import org.icroco.picture.views.util.widget.ZoomDragPane;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2OutlinedAL;
import org.kordamp.ikonli.material2.Material2OutlinedMZ;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.icroco.picture.util.SystemUtil.mouseContiguousSelection;
import static org.icroco.picture.util.SystemUtil.mouseNonContiguousSelection;

@Component
@RequiredArgsConstructor
public class GalleryView implements FxView<StackPane> {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(GalleryView.class);

    private final I18N        i18N;
    private final MediaLoader mediaLoader;
    private final UserPreferenceService pref;
    private final TaskService           taskService;
    private       ZoomDragPane          photo;
    private final PersistenceService    persistenceService;
    private final GalleryFilterView     filterView;
    @Qualifier(OrganizeConfiguration.ORGANIZE_EDIT_MODE)
    private final SimpleBooleanProperty editMode;

    private final StackPane                             root           = new StackPane();
    private final BorderPane                            gallery        = new BorderPane();
    //    private final BorderPane                            carousel       = new BorderPane();
    private final Slider                                zoomThumbnails = createTickSlider();
    private final Breadcrumbs<Path>                     breadCrumbBar  = new Breadcrumbs<>();
    private final BooleanProperty                       expandCell     = new SimpleBooleanProperty(true);
    private final ObservableList<MediaFile>             images         = FXCollections.observableArrayList(MediaFile.extractor());
    private final DynamicFilteredList<MediaFile>        filteredImages = new DynamicFilteredList<>(images);
    private final SortedList<MediaFile>                 sortedImages   = new SortedList<>(filteredImages);
    private final SimpleObjectProperty<MediaCollection> currentCatalog = new SimpleObjectProperty<>(null);
    private final SimpleBooleanProperty                 mouseMoved     = new SimpleBooleanProperty(true);
    private       CustomGridView<MediaFile>             gridView;

    private       EGalleryClickState dblCickState = EGalleryClickState.GALLERY;
    private final FontIcon  editOn    = new FontIcon(CarbonIcons.EDIT);
    private final FontIcon  editOff   = new FontIcon(CarbonIcons.EDIT_OFF);
    private final FontIcon  blockIcon = new FontIcon(Material2OutlinedAL.BLOCK);
    private final Label     editCell  = new Label();
    private final ModalPane modalPane = new ModalPane();
    private       HBox               toolBar;


    //    private final Label dateOverlay = new Label("Display Date");
//    AnimatedLabel dateOverlay = new AnimatedLabel("", AnimationPair.fade());
    private final AnimatedValueLabel<String> dateOverlay = new AnimatedValueLabel<>("")
            .custom(settings -> settings.withCurve(Curve.EASE_IN_OUT_EXPO).withDuration(Duration.millis(500)));

    private final Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(15), ae -> {
        var animation = Animations.fadeOut(dateOverlay, Duration.seconds(1));
        animation.setOnFinished(event -> mouseMoved.set(false));
        animation.play();
//            mouseMoved.set(false);
    }));
    private final MultiplePredicates<MediaFile> predicates = new MultiplePredicates<>();

    @PostConstruct
    protected void postConstruct() {
        root.getStyleClass().add(ViewConfiguration.V_GALLERY);
        root.setId(ViewConfiguration.V_GALLERY);
        root.setMinSize(350, 250);
        root.setEventDispatcher(new DoubleClickEventDispatcher());

        gridView = new CustomGridView<>(FXCollections.emptyObservableList(), this::gridScrollEvent);
        gridView.getSelectionModel().getSelectedIndices().addListener((ListChangeListener<? super Integer>) change -> {
            while (change.next()) {
                if (change.wasPermutated()) {
                    for (int i = change.getFrom(); i < change.getTo(); ++i) {
                        //permutate
                    }
                } else if (change.wasUpdated()) {
                    //update item
                } else {
                    if (!change.getRemoved().isEmpty()) {
                        log.debug("Last: Removed: {}", change.getRemoved().getLast());
                    }
                    if (!change.getAddedSubList().isEmpty()) {
                        log.debug("Last Added: {}", change.getAddedSubList().getLast());
                        ofNullable(change.getAddedSubList().getLast())
                                .ifPresent(idx -> {
                                    taskService.sendEvent(PhotoSelectedEvent.builder()
                                                                            .mf(gridView.getItems().get(idx))
                                                                            .type(PhotoSelectedEvent.ESelectionType.SELECTED)
                                                                            .source(this)
                                                                            .build());
                                });
                    }


                }
            }

//            if (newValue != null && newValue.intValue() >= 0) {
//                taskService.sendEvent(PhotoSelectedEvent.builder()
//                                                        .mf(gridView.getItems().get(newValue.intValue()))
//                                                        .type(PhotoSelectedEvent.ESelectionType.SELECTED)
//                                                        .source(this)
//                                                        .build());
//            }
        });
        gridView.addScrollAndKeyhandler();
        sortedImages.setComparator(Comparator.comparing(MediaFile::getOriginalDate));
        gridView.setItems(sortedImages);
        gridView.setCache(true);
        gridView.setCacheHint(CacheHint.SPEED);
//        ofNullable(pref.getUserPreference().getGrid().getGridZoomFactor()).ifPresent(this::applyGridCellWidthFactor);
        gridView.setHorizontalCellSpacing(0D);
        gridView.setVerticalCellSpacing(0D);
//        editCell.setOpacity(0.4);
        editMode.bind(Bindings.equal(editCell.graphicProperty(), editOn));
        gridView.setCellFactory(new MediaFileGridCellFactory(mediaLoader,
                                                             taskService,
                                                             expandCell,
                                                             currentCatalog,
                                                             this::cellDoubleClick,
                                                             editMode));
        gridView.setOnZoom(this::zoomOnGrid);
        currentCatalog.addListener(this::collectionChanged);
//        carousel.addEventHandler(CustomMouseEvent.MOUSE_DOUBLE_CLICKED, this::onImageClick);
//        carousel.setId("Carousel");

        photo = new ZoomDragPane();
//        carousel.setCenter(photo);
        photo.maxHeightProperty().bind(root.heightProperty());
        photo.maxWidthProperty().bind(root.widthProperty());

        breadCrumbBar.setCrumbFactory(item -> new Hyperlink(item.getValue().getFileName().toString()));
        breadCrumbBar.setAutoNavigationEnabled(false);
        breadCrumbBar.setOnCrumbAction(bae -> SystemUtil.browseFile(bae.getSelectedCrumb().getValue()));
        breadCrumbBar.setDividerFactory(GalleryView::bcbDividerFactory);

        dateOverlay.visibleProperty().bind(Bindings.and(gallery.visibleProperty(), Bindings.isNotEmpty(sortedImages)).and(mouseMoved));
        dateOverlay.setTranslateX(dateOverlay.getTranslateX() + 20);
        dateOverlay.setTranslateY(dateOverlay.getTranslateY() + 20);
        dateOverlay.setId("grid-current-date");
        dateOverlay.setFocusTraversable(false);
//        dateOverlay.setEventDispatcher((event, tail) -> gridView.getEventDispatcher().dispatchEvent(event, tail));
//        dateOverlay.setOnMouseClicked(event -> Event.fireEvent(gridView, event));

        gridView.addEventHandler(MouseEvent.MOUSE_MOVED, event -> resetDateAutoHideEffect());

        toolBar = createBottomBar();
        gallery.getStyleClass().setAll("gallery-center");
        gallery.setCenter(gridView);
        gallery.setBottom(toolBar);
        photo.setVisible(false);
        StackPane.setAlignment(dateOverlay, Pos.TOP_LEFT);
        root.getChildren().addAll(modalPane, gallery, photo, dateOverlay);


        ofNullable(pref.getUserPreference().getGrid().getCellPerRow()).ifPresent(zoomThumbnails::setValue);
        gridView.cellWidthProperty().bind(Bindings.divide(Bindings.subtract(gallery.widthProperty(), 18), zoomThumbnails.valueProperty()));
        gridView.cellHeightProperty().bind(gridView.cellWidthProperty());
    }

    private void resetDateAutoHideEffect() {
        mouseMoved.set(true);
        timeline.playFromStart();
    }

    private void gridScrollEvent(MediaFile mediaFile) {
        if (mediaFile != null) {
//            log.info("MF: {}", mediaFile);
            updateDateOverlay(Optional.of(mediaFile));
        }
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
        var optMf = ofNullable(cell.getItem());
        log.atDebug()
           .log(() -> "#click: %s, mf: %s".formatted(event.getClickCount(),
                                                     optMf.map(MediaFile::getFullPath).orElse(null)));
        optMf.ifPresent(mf -> {
            if (event.getClickCount() == 1) {
                if (mouseNonContiguousSelection().test(event)) {
                    gridView.getSelectionModel().select(cell.getItem());
                } else if (mouseContiguousSelection().test(event)) {
                    ofNullable(gridView.getSelectionModel().getSelectedIndices().getFirst())
                            .ifPresent(fromIdx -> {
                                var toIdx = gridView.getIndex(cell.getItem());
                                gridView.getSelectionModel().selectRange(fromIdx, toIdx + 1);
//                                log.info("fromIds: {}, toIds: {}", fromIdx, toIdx);
                            });
                } else {
                    gridView.getSelectionModel().clearSelection();
                    gridView.getSelectionModel().select(cell.getItem());
                }
                cell.requestLayout();
            } else if (event.getClickCount() == 2) {
                gridView.getSelectionModel().clearSelection();
                gridView.getSelectionModel().select(cell.getItem());
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

        editOn.getStyleClass().add("inner-icon");
        editOff.getStyleClass().add("inner-icon");
        blockIcon.getStyleClass().add("outer-icon");
//        keepOrThrowIcon.getChildren().addAll(editOff, blockIcon);
//        new FontIcon(Material2OutlinedMZ.THUMBS_UP_DOWN);
//        icon.setIconSize(32);
//        icon.setId("fitGridCell");
        editCell.setGraphic(editOff);
        editCell.setOnMouseClicked(this::editClick);
        editCell.setCursor(Cursor.HAND);
//        keepOrThrowLabel.setDisable(true);
//        editCell.setOpacity(.4);
        editCell.setTooltip(new Tooltip("Press 'e' to enter into edit mode (metadata)"));
//        zoomThumbnails.getStyleClass().add(Styles.SMALL);
        zoomThumbnails.setSkin(new ProgressSliderSkin(zoomThumbnails));
        ofNullable(pref.getUserPreference().getGrid().getCellPerRow()).ifPresent(zoomThumbnails::setValue);
        zoomThumbnails.setMax(pref.getUserPreference().getGrid().getMaxCellPerRow());
        zoomThumbnails.valueProperty()
                      .addListener((_, oldValue, newValue) -> {
                          log.debug("Zoom Level: old: {}, new: {}", oldValue, newValue.intValue());
                          pref.getUserPreference().getGrid().setCellPerRow(newValue.intValue());
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
        nbKeep.managedProperty().bind(editMode);
        nbThrow.managedProperty().bind(editMode);
        nbKeep.visibleProperty().bind(editMode);
        nbThrow.visibleProperty().bind(editMode);

        Label lbFilter = new Label();
        lbFilter.setCursor(Cursor.HAND);
        lbFilter.setGraphic(filterView.filterAdd);
        lbFilter.setOnMouseClicked(_ -> filterView.showFilter(modalPane, lbFilter, predicates)); // TODO: Replace with MpDialog.

        filteredImages.setPredicate(predicates);

        bar.getChildren().addAll(expand,
                                 editCell,
                                 zoomThumbnails,
                                 breadCrumbBar,
                                 new Spacer(),
                                 lbFilter,
                                 nbKeep,
                                 nbThrow,
                                 new Separator(Orientation.VERTICAL),
                                 nbImages);

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
        slider.setBlockIncrement(1);
        slider.setMajorTickUnit(1);
        slider.setMinorTickCount(0);
        slider.setSnapToTicks(true);
        slider.setShowTickLabels(false);
        slider.setMin(3);

        return slider;
    }

    void collectionChanged(ObservableValue<? extends MediaCollection> observable,
                           MediaCollection oldValue,
                           MediaCollection newValue) {
        if (newValue != null) {
            images.clear();
            gridView.getSelectionModel().clearSelection();
            resetBcbModel(null);
//            predicates.clear();
            images.addAll(newValue.medias());
            updateDateOverlay(sortedImages.stream().findFirst());
        } else {
            breadCrumbBar.setSelectedCrumb(null);
            images.clear();
//            predicates.clear();
            updateDateOverlay(empty());
        }
    }

    private void updateDateOverlay(Optional<MediaFile> optMf) {
        if (optMf.isPresent()) {
//            log.info("Overlay with: {}", optMf.get().fullPath());
            resetDateAutoHideEffect();
            dateOverlay.setValue(Constant.DATE_FORMATTER.format(optMf.get().getOriginalDate()));
        } else {
            dateOverlay.setValue("");
        }
    }

    private void onImageClick(MouseEvent event) {
        log.info("DblClick: {}", event);
        if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
            displayNext(null);
            event.consume();
        }
    }

    private void displayNext(MediaFile mf) {
        EGalleryClickState prev = dblCickState;
        if (mf != null && !Files.exists(mf.fullPath())) {
            taskService.sendEvent(NotificationEvent.builder()
                                                   .type(NotificationEvent.NotificationType.ERROR)
                                                   .message("File path not found (or not mounted): '%s'".formatted(mf.getFullPath()))
                                                   .source(this)
                                                   .build());
            return;
        }

        dblCickState = dblCickState.next();
        log.atDebug()
           .log(() -> "Next click: %s, file: %s".formatted(dblCickState, ofNullable(mf).map(MediaFile::getFullPath).orElse(null)));
        switch (dblCickState) {
            case GALLERY -> displayGallery(mf);
            case IMAGE, IMAGE_BACK -> {
                photo.requestFocus();
                if (prev == EGalleryClickState.GALLERY) {
                    gallery.setVisible(false);
                    photo.setImage(null, null);
                    photo.setVisible(true);
                    mediaLoader.getOrLoadImage(mf);
                } else {
                    photo.noZoom();
                    gallery.setVisible(false);
                    photo.setVisible(true);
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
        Timeline timeline1 = Animations.fadeOut(photo, Duration.millis(500));
        timeline1.setOnFinished(_ -> {
            gallery.setVisible(true);
            photo.setVisible(false);
            if (mf != null) {
                gallery.requestFocus();
                gridView.getSelectionModel().select(mf);
                gridView.selectionRequestFocus();
                gridView.ensureVisible(mf);
            }
        });
        timeline1.playFromStart();

//        updateDateOverlay(gridView.getFirstVisible().map(Cell::getItem));
    }

    private void zoomOnGrid(ZoomEvent event) {
        var zoom = Zoom.of(event);
        switch (zoom) {
            case ZOOM_IN -> zoomThumbnails.increment();
            case ZOOM_OUT -> zoomThumbnails.decrement();
        }
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

//                gridView.getSelectionModel()
//                        .getSelection()
//                        .stream()
//                        .map(cell -> {
//                            var found = mc.get(cell.getItem().getId());
//                            if (found == null) {
//                                return null;
//                            } else {
//                                cell.setItem(found);
//                                return cell;
//                            }
//                        })
//                        .filter(Objects::nonNull)
//                        .forEach(gridView.getSelectionModel()::set);
            }
        }
    }

    @FxEventListener
    public void updateImages(CollectionUpdatedEvent event) {
        log.info("Recieved CollectionUpdatedEvent on collection: '{}', newItems: '{}', deletedItems: '{}', modifiedItems: '{}'",
                 event.getMcId(),
                 event.getNewItems().size(),
                 event.getDeletedItems().size(),
                 event.getModifiedItems());
        if (event.isEmpty()) {
            return;
        }
        var selected = gridView.getSelectionModel().getSelectedItem();
        images.addAll(event.getNewItems());
        images.removeAll(event.getDeletedItems());
        images.removeIf(mf -> event.getSubDirsDeleted().contains(mf.fullPath().getParent()));
        // TODO: find a smoother way to update.
//        images.removeAll(event.getModifiedItems());
//        images.addAll(event.getModifiedItems());
        event.getModifiedItems().forEach(MediaFile::resetProperties);
        if (selected != null) {
            gridView.getSelectionModel().select(selected);
        }
    }


    public void collectionPathChange(ObservableValue<? extends PathSelection> observable, PathSelection oldValue, PathSelection newValue) {
        dblCickState = EGalleryClickState.GALLERY;
        if (newValue.equalsNoSeed(oldValue)) {
            log.debug("MediaCollection subpath re-selected: root: {}, entry: {}", newValue.mediaCollectionId(), newValue.subPath());
            displayGallery(null);
//            gallery.setOpacity(0);
//             Animations.fadeIn(gallery, Duration.millis(1000)).playFromStart();
        } else {
            log.info("MediaCollection subpath selected: root: {}, entry: {}", newValue.mediaCollectionId(), newValue.subPath());
//            filteredImages.setPredicate(predicates); // TODO: hook, better to invalidate with listener.
            currentCatalog.setValue(persistenceService.getMediaCollection(newValue.mediaCollectionId()));
            resetBcbModel(newValue.subPath());
            final var path = getCurrentCatalog().path().resolve(newValue.subPath());
            predicates.remove(PathPredicate.class);
            predicates.add(new PathPredicate(path));
//            log.info("First item: {}, latest: {}", sortedImages.getFirst(), sortedImages.getLast());
            mediaLoader.loadAndCachedValues(sortedImages);
            updateDateOverlay(sortedImages.stream().findFirst());
//            updateDateOverlay();
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
            final var source = ofNullable(event.getSource()).orElse(MediaFileGridCellFactory.class).getClass();
            final var mf     = event.getMf();
            log.atDebug().log(() -> {
                Optional<Thumbnail> cache = persistenceService.getThumbnailFromCache(mf);
                Optional<Thumbnail> db    = persistenceService.findByPathOrId(mf);
                return "Photo selected: root: '%s', '%s', '%s', from: '%s'. Thumbhnail DB mcId: '%s', type: '%s'. Tumbhnail Cache, mcId: '%s', type: '%s'"
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
    public void sceanReady(SceneReadyEvent event) {
        root.requestFocus();
    }

    @FxEventListener
    public void imageLoading(ImageLoadingdEvent event) {
        photo.getMaskerPane().start(event.getProgress());
    }

    @FxEventListener
    public void imageLoaded(ImageLoadedEvent event) {
        photo.setImage(event.getMediaFile(), event.getImage());
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

    public void enterPressed(KeyEvent keyEvent) {
        if (dblCickState == EGalleryClickState.GALLERY) {
            displayNext(gridView.getSelectionModel().getSelectedItem());
        } else if (dblCickState.isImage()) {
//            displayNext(photo.getMediaFile());
        }
    }

    public void escapePressed(KeyEvent keyEvent) {
        if (dblCickState == EGalleryClickState.ZOOM) {
            displayNext(photo.getMediaFile());
        } else if (dblCickState.isImage()) {
            editCell.setGraphic(editOff); // We ensure there is something to save.
            displayGallery(photo.getMediaFile());
        }
        keyEvent.consume();
    }

    public void leftPressed(KeyEvent keyEvent) {
        if (dblCickState.isImage()) {
            if (keyEvent.isMetaDown()) {
                escapePressed(keyEvent);
            } else {
                if (photo.getMediaFile() != null) {
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
            }
        } else if (dblCickState == EGalleryClickState.GALLERY) {
            gridView.oneRowLeft();
        }
        keyEvent.consume();
    }

    public void rightPressed(KeyEvent keyEvent) {
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
        } else if (dblCickState == EGalleryClickState.GALLERY) {
            gridView.oneRowRight();
        }
    }

    public void upPressed(KeyEvent keyEvent) {
        gridView.oneRowUp();
    }

    public void downPressed(KeyEvent keyEvent) {
        gridView.oneRowDown();
    }

    public void pageUpPressed(KeyEvent keyEvent) {
        gridView.pageUp();
    }

    public void pageDownPressed(KeyEvent keyEvent) {
        gridView.pageDown();
    }

    private void skipPressed(KeyEvent keyEvent) {
        keyEvent.consume();
    }

    public void editPressed(KeyEvent keyEvent) {
        if (editCell.getGraphic() == editOff) {
            editCell.setGraphic(editOn);
        } else {
            editCell.setGraphic(editOff);
        }
    }

    public void editClick(MouseEvent mouseEvent) {
        editPressed(null);
    }

    public void keepPressed(KeyEvent keyEvent) {
//        gridView.getSelectionModel().getSelection().forEach();
        if (SystemUtil.keyNonContiguousSelection().test(keyEvent)) {
            gridView.getSelectionModel().clearSelection();
            gridView.getSelectionModel().selectAll();
        } else {
            ofNullable(gridView.getSelectionModel().getSelectedItem())
                    .ifPresent(mediaFile -> mediaFile.setKeepOrThrow(EKeepOrThrow.KEEP));
        }
    }

    public void throwPressed(KeyEvent keyEvent) {
        ofNullable(gridView.getSelectionModel().getSelectedItem())
                .ifPresent(mediaFile -> mediaFile.setKeepOrThrow(EKeepOrThrow.THROW));
    }

    public void undecidePressed(KeyEvent keyEvent) {
        ofNullable(gridView.getSelectionModel().getSelectedItem())
                .ifPresent(mediaFile -> mediaFile.setKeepOrThrow(EKeepOrThrow.UNKNOW));
    }

    public void deletePressed(KeyEvent keyEvent) {
        if (dblCickState == EGalleryClickState.GALLERY) {
            var filesToBeDeleted = List.copyOf(gridView.getSelectionModel().getSelectedItems());
            if (!filesToBeDeleted.isEmpty()) {
                var dialog = MpDialog.builder()
                                     .width(400)
                                     .header(i18N.labelForKey("confirmation.deleteFiles.title"))
                                     .body(i18N.labelForKey("confirmation.deleteFiles.body", filesToBeDeleted.size()))
                                     .build();

                dialog.show(modalPane, exitMode -> {
                    gridView.requestFocus();
                    if (exitMode == MpDialog.EXIT_MODE.OK) {
                        images.removeAll(filesToBeDeleted); // Next disk pooling will do a clean delete (DB, Thumbnail ...)
                        SystemUtil.moveToTrash(filesToBeDeleted.stream().map(MediaFile::getFullPath).toList());
                    }
                });
            }
        }
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

    @Override
    public StackPane getRootContent() {
        return root;
    }

    public void diffRight(KeyEvent keyEvent) {
        taskService.sendEvent(ShowDiffEvent.builder().right(gridView.getSelectionModel().getSelectedItem()).source(this).build())
                   .thenAccept(_ -> setFocus());
    }

    public void diffLeft(KeyEvent keyEvent) {
        taskService.sendEvent(ShowDiffEvent.builder().left(gridView.getSelectionModel().getSelectedItem()).source(this).build())
                   .thenAccept(_ -> setFocus());
    }

    private void setFocus() {
        FxPlatformExecutor.fxRun(() -> {
            ((Stage) root.getScene().getWindow()).toFront();
            root.getScene().getWindow().requestFocus();
            switch (dblCickState) {
                case GALLERY -> gallery.requestFocus();
                case IMAGE, IMAGE_BACK, ZOOM -> photo.requestFocus();
            }
        });
    }
}
