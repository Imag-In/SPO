package org.icroco.picture.ui.collections;

import atlantafx.base.controls.Spacer;
import jakarta.annotation.PostConstruct;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.util.Pair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.EntryStream;
import org.icroco.picture.ui.FxEventListener;
import org.icroco.picture.ui.FxView;
import org.icroco.picture.ui.event.*;
import org.icroco.picture.ui.event.CollectionEvent.EventType;
import org.icroco.picture.ui.model.GeoLocation;
import org.icroco.picture.ui.model.MediaCollection;
import org.icroco.picture.ui.model.MediaCollectionEntry;
import org.icroco.picture.ui.model.MediaFile;
import org.icroco.picture.ui.persistence.PersistenceService;
import org.icroco.picture.ui.pref.UserPreferenceService;
import org.icroco.picture.ui.task.AbstractTask;
import org.icroco.picture.ui.task.TaskService;
import org.icroco.picture.ui.util.*;
import org.icroco.picture.ui.util.hash.IHashGenerator;
import org.icroco.picture.ui.util.metadata.IMetadataExtractor;
import org.icroco.picture.ui.util.widget.FxUtil;
import org.kordamp.ikonli.fontawesome5.FontAwesomeRegular;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static javafx.application.Platform.runLater;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollectionView implements FxView<VBox> {
    private final TaskService           taskService;
    private final PersistenceService    persistenceService;
    private final UserPreferenceService pref;
    private final IMetadataExtractor    metadataExtractor;
    private final IHashGenerator        hashGenerator;
    private final BooleanProperty       disablePathActions = new SimpleBooleanProperty(false);
    private final SimpleIntegerProperty catalogSizeProp    = new SimpleIntegerProperty(0);

    private final VBox  root        = new VBox();
    private final Label catalogSize = new Label();
    private final Accordion mediaCollections = new Accordion();
    private final Button    addCollection    = new Button();
    private final HBox      collectionHeader = new HBox();


    @PostConstruct
    protected void initializedOnce() {
        root.getStyleClass().add("header");
        VBox.setVgrow(mediaCollections, Priority.ALWAYS);

        collectionHeader.setAlignment(Pos.BASELINE_LEFT);

        FxUtil.styleCircleButton(addCollection);
        addCollection.setGraphic(new FontIcon(FontAwesomeSolid.PLUS));
        addCollection.setVisible(true);
        addCollection.disableProperty().bind(disablePathActions);
        addCollection.setOnMouseClicked(this::newCollection);
        root.setOnMouseEntered(event -> addCollection.setVisible(true));
        root.setOnMouseExited(event -> {
            if (!mediaCollections.getPanes().isEmpty()) {
                addCollection.setVisible(false);
            }
        });
        mediaCollections.expandedPaneProperty().addListener(this::titlePaneChanged);

        catalogSize.getStyleClass().add(Styles.TEXT_SMALL);
        catalogSizeProp.addListener((observable, oldValue, newValue) -> {
            if (newValue.intValue() <= 0) {
                catalogSize.setText("");
            } else {
                catalogSize.setText(newValue.toString());
            }
        });
//        catalogSize.prefHeightProperty().bind(header.heightProperty());
        Label header = new Label("Collections");
        header.getStyleClass().add(Styles.TITLE_3);
        collectionHeader.getChildren().addAll(header, catalogSize, new Spacer(), addCollection);

        root.getChildren().addAll(collectionHeader, mediaCollections);
    }

    private void titlePaneChanged(ObservableValue<? extends TitledPane> observableValue, TitledPane oldValue, TitledPane newValue) {
        if (newValue != null) {
            int id = (int) newValue.getUserData();
            pref.getUserPreference().getCollection().setLastViewed(id);
            var catalogById = persistenceService.getMediaCollection(id);
            taskService.sendEvent(new CollectionEvent(catalogById, EventType.SELECTED, this));
//            taskService.sendFxEvent(new WarmThumbnailCacheEvent(catalogById, this));
        }
    }

    //        @Async(ImageInConfiguration.FX_EXECUTOR)
    @EventListener(CollectionsLoadedEvent.class)
    public void initCollections(CollectionsLoadedEvent event) {
        runLater(() -> {
            var id = pref.getUserPreference().getCollection().getLastViewed();
            event.getMediaCollections()
                 .stream()
                 .map(this::createTreeView)
                 .toList()
                 .stream()
                 .filter(p -> p.getKey().id() == id)
                 .findFirst()
                 .ifPresent(p -> {
                     mediaCollections.setExpandedPane(p.getValue().tp());
                 });
        });
    }

    private Pair<MediaCollection, PaneTreeView> createTreeView(final MediaCollection mediaCollection) {
        var rootItem = new TreeItem<>(mediaCollection.path());
        var treeView = new TreeView<>(rootItem);
        treeView.setShowRoot(false);
        treeView.setEditable(false);
        rootItem.setExpanded(true);
        treeView.setCellFactory(param -> new TextFieldTreeCell<>(new PathConverter()));
        log.info("Add collection: {}", mediaCollection.path());
        catalogSizeProp.set(catalogSizeProp.get() + mediaCollection.medias().size());

        mediaCollection.subPaths().forEach(c -> {
            var p = mediaCollection.path().relativize(c.name());
            addSubDir(rootItem, p);
            log.debug("Path: {}", p);
        });

        final TitledPane title = new TitledPane(mediaCollection.path().getFileName().toString(), treeView);
        title.setUserData(mediaCollection.id());
        title.setTooltip(new Tooltip(mediaCollection.path() + " (id: " + mediaCollection.id() + ")"));


        FontIcon graphic = new FontIcon(FontAwesomeRegular.TRASH_ALT);
        graphic.setIconSize(12);
        graphic.setId("deleteCollection");
        Label label = new Label("", graphic);
        Nodes.addRightGraphic(title, label);
        label.setOnMouseClicked(this::onDeleteCollection);

        mediaCollections.getPanes().add(title);
        mediaCollections.getPanes().sort(Comparator.comparing(TitledPane::getText));

        treeView.getSelectionModel().selectedItemProperty().addListener((v, oldValue, newValue) -> {
            if (newValue != null) {
                log.debug("Tree view selected: {} ", newValue.getValue());
                taskService.sendEvent(new CollectionSubPathSelectedEvent(mediaCollection.path(), newValue.getValue(), CollectionView.this));
            }
        });

        return new Pair<>(mediaCollection, new PaneTreeView(title, treeView));
    }

    @SuppressWarnings("unchecked")
    private void updateTreeView(final TitledPane titledPane, final MediaCollection mediaCollection) {
        var treeView = (TreeView<Path>) titledPane.getContent();
        var rootItem = treeView.getRoot();

        mediaCollection.subPaths().forEach(c -> {
            var p = mediaCollection.path().relativize(c.name());
            addSubDir(rootItem, p);
            log.debug("Path: {}", p);
        });
    }

    private void addSubDir(TreeItem<Path> current, Path path) {
        current.getChildren().sort(Comparator.comparing(ti -> ti.getValue().getFileName().toString(), String.CASE_INSENSITIVE_ORDER));
        for (int i = 0; i < path.getNameCount(); i++) {
            var child = new TreeItem<>(path.subpath(0, i + 1));
            if (current.getChildren().stream().noneMatch(pathTreeItem -> pathTreeItem.getValue().equals(child.getValue()))) {
                current.getChildren().add(child);
            }
            current = child;
        }
    }

    @Override
    public VBox getRootContent() {
        return root;
    }

    record TitlePaneEntry(TitledPane titledPane, int mediaCollectionId) {}

    private void onDeleteCollection(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 1) {
            Node source = (Node) mouseEvent.getSource();
            Nodes.getFirstParent(source, TitledPane.class)
                 .map(t -> new TitlePaneEntry(t, (int) t.getUserData()))
                 .ifPresent(tpe -> {
                     final Alert dlg             = new Alert(Alert.AlertType.CONFIRMATION, "");
                     final var   mediaCollection = persistenceService.getMediaCollection(tpe.mediaCollectionId);
                     dlg.setTitle("You do want delete MediaCollection ?");
                     dlg.getDialogPane()
                        .setContentText("Do you want to delete MediaCollection: " + mediaCollection.path() + " id: " + mediaCollection.id());

                     dlg.show();
                     dlg.resultProperty()
                        .addListener(o -> {
                            if (dlg.getResult() == ButtonType.OK) {
                                catalogSizeProp.set(catalogSizeProp.get() - mediaCollection.medias().size());
                                deleteCollection(tpe);
                            }
                        });
                 });
        }
    }

    private void deleteCollection(TitlePaneEntry entry) {
        var mc = persistenceService.getMediaCollection(entry.mediaCollectionId);
        persistenceService.deleteMediaCollection(entry.mediaCollectionId);
        mediaCollections.getPanes().remove(entry.titledPane);
        taskService.sendEvent(new CollectionEvent(mc, EventType.DELETED, this));
        // TODO: Clean Thumbnail Cache and DB.
    }

    //    @FXML
    private void newCollection(MouseEvent event) {
        final DirectoryChooser directoryChooser  = new DirectoryChooser();
        final File             selectedDirectory = directoryChooser.showDialog(root.getScene().getWindow());

        if (selectedDirectory != null) {
            var rootPath = selectedDirectory.toPath().normalize();
            mediaCollections.getPanes()
                            .stream()
                            .map(t -> new TitlePaneEntry(t, (int) t.getUserData()))
                            .filter(tpe -> rootPath.startsWith(persistenceService.getMediaCollection(tpe.mediaCollectionId).path()))
                            .findFirst()
                            .ifPresent(p -> {
                                // TODO: Rather than getting an error, jump to that dir into the proper collection.
                                throw new CollectionException("Path: '%s' is already included into collection item: '%s'".formatted(rootPath, p));
                            });
            mediaCollections.getPanes()
                            .stream()
                            .map(t -> new TitlePaneEntry(t, (int) t.getUserData()))
                            .filter(tpe -> rootPath.startsWith(persistenceService.getMediaCollection(tpe.mediaCollectionId).path()))
                            .findFirst()
                            .ifPresent(this::deleteCollection); // TODO: Ask user confirmation.

            disablePathActions.set(true);

            record CatalogAndTask(MediaCollection mediaCollection, CompletableFuture<?>[] futures) {}

            taskService.supply(scanDirectory(rootPath))
                       // TODO: generate hash only at the end of collection creation.
//                       .thenApplyAsync(catalog -> {
//                           final var allFiles   = new ArrayBlockingQueue<MediaFile>(catalog.medias().size());
//                           final var mediaFiles = List.copyOf(catalog.medias());
//                           final var result     = Collections.splitByCoreWithIdx(mediaFiles);
//                           final var futures = result.values()
//                                                     .map(batchMf -> taskService.supply(hashFiles(batchMf.getValue(),
//                                                                                                  batchMf.getKey(),
//                                                                                                  result.splitCount()))
//                                                                                .thenApply(allFiles::addAll))
//                                                     .toArray(new CompletableFuture[0]);
//                           return new CatalogAndTask(catalog, futures);
//                       })
//                       .thenApplyAsync(catalogAndTask -> {
//                           CompletableFuture.allOf(catalogAndTask.futures);
//                           return catalogAndTask.mediaCollection;
//                       })
                       .thenApplyAsync(persistenceService::saveCollection)
                       .thenAccept(mediaCollection -> runLater(() -> {
                           createTreeView(mediaCollection);
                           taskService.sendEvent(new ExtractThumbnailEvent(mediaCollection, this));
                       }));
        }
    }

    private Task<MediaCollection> scanDirectory(Path rootPath) {
        return new AbstractTask<>() {
            @Override
            protected MediaCollection call() throws Exception {
                updateTitle("Scanning directory: " + rootPath.getFileName());
                updateMessage("%s: scanning".formatted(rootPath));
                Set<MediaCollectionEntry> children;
                var                       now = LocalDate.now();
                try (var s = Files.walk(rootPath)) {
                    children = s.filter(Files::isDirectory)
                                .map(Path::normalize)
                                .filter(p -> !p.equals(rootPath))
                                .filter(FileUtil::isLastDir)
                                .map(p -> MediaCollectionEntry.builder().name(p).build())
                                .collect(Collectors.toSet());
                }
                try (var images = Files.walk(rootPath)) {
                    final var filteredImages = images.filter(p -> !Files.isDirectory(p))   // not a directory
                                                     .map(Path::normalize)
                                                     .filter(Constant::isSupportedExtension)
                                                     .toList();
                    final var size = filteredImages.size();
                    updateProgress(0, size);
                    return MediaCollection.builder().path(rootPath)
                                                    .subPaths(children)
                                                    .medias(EntryStream.of(filteredImages)
                                                                       .peek(i -> updateProgress(i.getKey(), size))
                                                                       .map(i -> create(now, i.getValue()))
                                                                       .collect(Collectors.toSet()))
                                                    .build();
                }
            }

            @Override
            protected void succeeded() {
                var catalog = getValue();
                log.info("Collections entries: {}, time: {}", catalog.medias().size(), System.currentTimeMillis() - start);
                disablePathActions.set(false);
            }

            @Override
            protected void failed() {
                disablePathActions.set(false);
                log.error("While scanning dir: '{}'", rootPath, getException());
                super.failed();
            }
        };
    }

    // TODO: move outside of this class, like collectionmanager.
    private Task<List<MediaFile>> hashFiles(final List<MediaFile> mediaFiles, final int batchId, final int nbBatches) {
        return new AbstractTask<>() {
            @Override
            protected List<MediaFile> call() throws Exception {
                var size = mediaFiles.size();
                updateTitle("Hashing %s files. %d/%d ".formatted(size, batchId, nbBatches));
                updateProgress(0, size);
//                updateMessage("%s: scanning".formatted(rootPath));
                for (int i = 0; i < size; i++) {
                    var mf = mediaFiles.get(i);
                    updateProgress(i, size);
                    updateMessage("Hashing: " + mf.getFullPath().getFileName());
                    mf.setHash(hashGenerator.compute(mf.getFullPath()).orElse(null));
                }
                return mediaFiles;
            }

            @Override
            protected void succeeded() {
                log.info("'{}' files hashing time: {}", mediaFiles.size(), System.currentTimeMillis() - start);
//            // We do not expand now, we're waiting thumbnails creation.
//            createTreeView(mediaCollection);
//            disablePathActions.set(false);
//            taskService.notifyLater(new ExtractThumbnailEvent(mediaCollection, this));
            }
        };
    }

    private MediaFile create(LocalDate now, Path p) {
        final var h = metadataExtractor.header(p);

        var builder = MediaFile.builder()
                .fullPath(p)
                .fileName(p.getFileName().toString())
                .thumbnailUpdateProperty(new SimpleObjectProperty<>(LocalDateTime.MIN));

        h.ifPresent(header -> {
            builder.dimension(header.size())
                   .geoLocation(new GeoLocation(header.geoLocation().getLatitude(), header.geoLocation().getLongitude()))
                   .originalDate(header.orginalDate());
        });

        return builder.build();
    }

    @FxEventListener
    public void catalogEvent(final CollectionEvent event) {
        if (Objects.requireNonNull(event.getType()) == EventType.READY) {
            runLater(() -> {
                mediaCollections.getPanes()
                                .stream()
                                .filter(tp -> ((int) tp.getUserData()) == event.getMediaCollection().id())
                                .findFirst()
                                .filter(tp -> mediaCollections.getExpandedPane() != tp)
                                .ifPresent(tp -> mediaCollections.setExpandedPane(tp));
            });
        } else if (EventType.SELECTED == event.getType()) {
            ((TreeView<?>) mediaCollections.getExpandedPane().getContent()).getSelectionModel().clearSelection();
        }
    }

    @FxEventListener
    public void updateImages(CollectionUpdatedEvent event) {
        log.info("Recieved update on collection: '{}', newItems: '{}', deletedItems: '{}'",
                 event.getMediaCollectionId(),
                 event.getNewItems().size(),
                 event.getDeletedItems().size());
        var mc = persistenceService.getMediaCollection(event.getMediaCollectionId());
        var directories = event.getNewItems()
                               .stream()
                               .map(mediaFile -> mediaFile.getFullPath().normalize().getParent())
                               .distinct()
                               .filter(FileUtil::isLastDir)
                               .filter(p -> !p.equals(mc.path()))
                               .map(p -> MediaCollectionEntry.builder().name(p).build())
                               .toList();
        mc.subPaths().addAll(directories);

        var mcSaved = persistenceService.saveCollection(mc);
        mediaCollections.getPanes()
                        .stream()
                        .filter(n -> ((int) n.getUserData() == event.getMediaCollectionId()))
                        .findFirst()
                        .ifPresent(titledPane -> updateTreeView(titledPane, mcSaved));
    }

    record PaneTreeView(TitledPane tp, TreeView<Path> treeView) {
    }
}
