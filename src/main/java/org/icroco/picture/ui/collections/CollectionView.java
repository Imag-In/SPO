package org.icroco.picture.ui.collections;

import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Tweaks;
import jakarta.annotation.PostConstruct;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.util.Pair;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.EntryStream;
import org.icroco.picture.ui.FxEventListener;
import org.icroco.picture.ui.FxView;
import org.icroco.picture.ui.event.*;
import org.icroco.picture.ui.event.CollectionEvent.EventType;
import org.icroco.picture.ui.model.MediaCollection;
import org.icroco.picture.ui.model.MediaCollectionEntry;
import org.icroco.picture.ui.model.MediaFile;
import org.icroco.picture.ui.persistence.CollectionRepository;
import org.icroco.picture.ui.persistence.PersistenceService;
import org.icroco.picture.ui.pref.UserPreferenceService;
import org.icroco.picture.ui.task.AbstractTask;
import org.icroco.picture.ui.task.TaskService;
import org.icroco.picture.ui.util.Constant;
import org.icroco.picture.ui.util.FileUtil;
import org.icroco.picture.ui.util.Styles;
import org.icroco.picture.ui.util.hash.IHashGenerator;
import org.icroco.picture.ui.util.metadata.IMetadataExtractor;
import org.icroco.picture.ui.util.widget.FxUtil;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
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
    private final CollectionRepository collectionRepository;
    private final TaskService           taskService;
    private final PersistenceService    persistenceService;
    private final UserPreferenceService pref;
    private final IMetadataExtractor    metadataExtractor;
    private final IHashGenerator        hashGenerator;
    private final BooleanProperty       disablePathActions = new SimpleBooleanProperty(false);
    private final SimpleIntegerProperty catalogSizeProp    = new SimpleIntegerProperty(0);

    private final VBox   root             = new VBox();
    private final Label  catalogSize      = new Label();
    //    private final Accordion mediaCollections = new Accordion();
    private final Button addCollection    = new Button();
    private final HBox   collectionHeader = new HBox();

    private final CollectionTreeItem rootTreeItem = new CollectionTreeItem(new CollectionNode(Path.of("Files"), -1));
    private final TreeView<CollectionNode> treeView     = new TreeView<>(rootTreeItem);

    public record CollectionNode(Path path, int id) {
        public CollectionNode(Path path) {
            this(path, -1);
        }

        boolean isRootCollection() {
            return id >= 0;
        }
    }

    @PostConstruct
    protected void initializedOnce() {
        root.getStyleClass().add("header");
        rootTreeItem.setExpanded(true);
        treeView.setMinHeight(250);
        treeView.setShowRoot(false);
        treeView.setEditable(false);
        treeView.setCellFactory(param -> new CollectionTreeCell(taskService));
        treeView.getSelectionModel().selectedItemProperty().addListener(this::treeItemSelectionChanged);
        atlantafx.base.theme.Styles.toggleStyleClass(treeView, Tweaks.EDGE_TO_EDGE);

        collectionHeader.setAlignment(Pos.BASELINE_LEFT);

        FxUtil.styleCircleButton(addCollection);
        addCollection.setGraphic(new FontIcon(MaterialDesignP.PLUS));
        addCollection.setVisible(true);
        addCollection.disableProperty().bind(disablePathActions);
        addCollection.setOnMouseClicked(this::newCollection);
        root.setOnMouseEntered(event -> addCollection.setVisible(true));
        root.setOnMouseExited(event -> addCollection.setVisible(rootTreeItem.getChildren().isEmpty()));

        catalogSize.getStyleClass().add(Styles.TEXT_SMALL);
        catalogSizeProp.addListener((observable, oldValue, newValue) -> {
            if (newValue.intValue() <= 0) {
                catalogSize.setText("");
            } else {
                catalogSize.setText(" (" + newValue.toString() + ")");
            }
        });
//        catalogSize.prefHeightProperty().bind(header.heightProperty());
        Label header = new Label("Collections");
        header.getStyleClass().add(Styles.TITLE_3);
        collectionHeader.getChildren().addAll(header, catalogSize, new Spacer(), addCollection);

        var box = new HBox(treeView);
        VBox.setVgrow(box, Priority.ALWAYS);

        root.getChildren().addAll(collectionHeader, box);
    }


    private TreeItem<CollectionNode> findMainCollectionNode(TreeItem<CollectionNode> child) {
        if (child.getParent() == null || child.equals(rootTreeItem)) {
            return child;
        }
        if (child.getParent().equals(rootTreeItem)) {
            return child;
        }

        return findMainCollectionNode(child.getParent());
    }

    private static StringConverter<CollectionNode> getStringConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(CollectionNode object) {
                return object.path.getFileName().toString();
            }

            @Override
            public CollectionNode fromString(String string) {
                return new CollectionNode(Path.of(string), -1);
            }
        };
    }

    private void treeItemSelectionChanged(ObservableValue<? extends TreeItem<CollectionNode>> v,
                                          TreeItem<CollectionNode> oldValue,
                                          TreeItem<CollectionNode> newValue) {
        if (newValue != null) {
            log.trace("Tree view selected: {} ", newValue.getValue());
            pref.getUserPreference().getCollection().setLastViewed(newValue.getValue().id);

            taskService.sendEvent(new CollectionSubPathSelectedEvent(findMainCollectionNode(newValue).getValue().id(),
                                                                     newValue.getValue().path,
                                                                     CollectionView.this));
        }
    }

    private Pair<MediaCollection, TreeItem<CollectionNode>> createTreeView(final MediaCollection mediaCollection) {
        var pathTreeItem = new TreeItem<>(new CollectionNode(mediaCollection.path(), mediaCollection.id()));
        log.info("Add collection: {}", mediaCollection.path());
        catalogSizeProp.set(catalogSizeProp.get() + mediaCollection.medias().size());
        rootTreeItem.getChildren().add(pathTreeItem);
        mediaCollection.subPaths().forEach(c -> {
            var p = mediaCollection.path().relativize(c.name());
            addSubDir(pathTreeItem, p);
            log.debug("Path: {}", p);
        });

        final TitledPane title = new TitledPane(mediaCollection.path().getFileName().toString(), treeView);
        title.setUserData(mediaCollection.id());
        title.setTooltip(new Tooltip(mediaCollection.path() + " (id: " + mediaCollection.id() + ")"));

//        FontIcon graphic = new FontIcon(FontAwesomeRegular.TRASH_ALT);
//        graphic.setIconSize(12);
//        graphic.setId("deleteCollection");
//        Label label = new Label("", graphic);
//        Nodes.addRightGraphic(title, label);
//        label.setOnMouseClicked(this::onDeleteCollection);

//        mediaCollections.getPanes().add(title);
//        mediaCollections.getPanes().sort(Comparator.comparing(TitledPane::getText));


        return new Pair<>(mediaCollection, pathTreeItem);
    }

    private void updateTreeView(final TreeItem<CollectionNode> treeItem, final MediaCollection mediaCollection) {
        mediaCollection.subPaths().forEach(c -> {
            var p = mediaCollection.path().relativize(c.name());
            addSubDir(treeItem, p);
            log.debug("Path: {}", p);
        });
    }

    private void addSubDir(TreeItem<CollectionNode> current, Path path) {
        current.getChildren().sort(Comparator.comparing(ti -> ti.getValue().path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER));
        for (int i = 0; i < path.getNameCount(); i++) {
            var child = new TreeItem<>(new CollectionNode(path.subpath(0, i + 1)));
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

    @FxEventListener
    public void onDeleteCollection(DeleteCollectionEvent event) {
//        if (mouseEvent.getClickCount() == 1) {
//            Node source = (Node) mouseEvent.getSource();
//            Nodes.getFirstParent(source, TitledPane.class)
//                 .map(t -> new TitlePaneEntry(t, (int) t.getUserData()))
        persistenceService.findMediaCollection(event.getMcId())
                          .ifPresent(mc -> {
                              final Alert dlg = new Alert(Alert.AlertType.CONFIRMATION, "");
                              dlg.initOwner(root.getScene().getWindow());
                              dlg.setTitle("You do want delete MediaCollection ?");
                              dlg.getDialogPane()
                                 .setContentText("""
                                                         Do you want to delete MediaCollection:
                                                           Path: %s
                                                           Id: %d
                                                           #Items: %s
                                                         """.formatted(mc.path(), mc.id(), mc.medias().size()));

                              dlg.show();
                              dlg.resultProperty()
                                 .addListener(o -> {
                                     if (dlg.getResult() == ButtonType.OK) {
                                         catalogSizeProp.set(catalogSizeProp.get() - mc.medias().size());
                                         deleteCollection(mc);
                                     }
                                 });
                          });
//    }
    }

    private void deleteCollection(MediaCollection entry) {
        taskService.supply(() -> {
            rootTreeItem.getChildren().removeIf(pathTreeItem -> pathTreeItem.getValue().path.equals(entry.path()));
            persistenceService.deleteMediaCollection(entry.id());
            taskService.sendEvent(new CollectionEvent(entry, EventType.DELETED, this));
            // TODO: Clean Thumbnail Cache and DB.
        });
    }

    //    @FXML
    private void newCollection(MouseEvent event) {
        final DirectoryChooser directoryChooser  = new DirectoryChooser();
        final File             selectedDirectory = directoryChooser.showDialog(root.getScene().getWindow());

        if (selectedDirectory != null) {
            var rootPath = selectedDirectory.toPath().normalize();
            rootTreeItem.getChildren()
                        .stream()
                        .filter(treeItem -> rootPath.startsWith(persistenceService.getMediaCollection(treeItem.getValue().id()).path()))
                        .findFirst()
                        .ifPresent(p -> {
                            // TODO: Rather than getting an error, jump to that dir into the proper collection.
                            throw new CollectionException("Path: '%s' is already included into collection item: '%s'".formatted(rootPath, p));
                        });
            rootTreeItem.getChildren()
                        .stream()
                        .filter(treeItem -> rootPath.startsWith(persistenceService.getMediaCollection(treeItem.getValue().id()).path()))
                        .findFirst()
                        .map(treeItem -> persistenceService.getMediaCollection(treeItem.getValue().id()))
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
                   .geoLocation(header.geoLocation())
                   .originalDate(header.orginalDate());
        });

        return builder.build();
    }


    //        @Async(ImageInConfiguration.FX_EXECUTOR)
    @FxEventListener
    public void initCollections(CollectionsLoadedEvent event) {
        var id = pref.getUserPreference().getCollection().getLastViewed();
        log.info("Collection Loaded: {}, prefId: {}", event, id);
        event.getMediaCollections()
             .stream()
             .map(this::createTreeView)
             .toList()
             .stream()
             .filter(p -> p.getKey().id() == id)
             .findFirst()
             .ifPresent(p -> {
                 log.info("Expand: {}", p.getKey().path());
                 p.getValue().setExpanded(true);
                 var catalogById = persistenceService.getMediaCollection(id);
                 taskService.sendEvent(new CollectionEvent(catalogById, EventType.SELECTED, this));
                 treeView.getSelectionModel().select(p.getValue());
             });
    }

    @FxEventListener
    public void catalogEvent(final CollectionEvent event) {
        log.info("Catalog Event: {}", event);
        if (Objects.requireNonNull(event.getType()) == EventType.READY) {
            rootTreeItem.getChildren()
                        .stream()
                        .filter(treeItem -> treeItem.getValue().id() == event.getMediaCollection().id())
                        .findFirst()
                        .ifPresent(treeItem -> {
                            treeItem.setExpanded(true);
                            treeView.getSelectionModel().select(treeItem);
                        });
        }
//        else if (EventType.SELECTED == event.getType()) {
//            treeView.getSelectionModel().clearSelection();
//        }
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

        rootTreeItem.getChildren()
                    .stream()
                    .filter(treeItem -> treeItem.getValue().id() == event.getMediaCollectionId())
                    .findFirst()
                    .ifPresent(treeItem -> updateTreeView(treeItem, mcSaved));
    }

    record PaneTreeView(TitledPane tp, TreeView<Path> treeView) {
    }
}
