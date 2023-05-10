package org.icroco.picture.ui.collections;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.util.Pair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.EntryStream;
import org.icroco.javafx.FxInitOnce;
import org.icroco.javafx.FxViewBinding;
import org.icroco.picture.ui.event.*;
import org.icroco.picture.ui.event.CatalogEvent.EventType;
import org.icroco.picture.ui.model.Catalog;
import org.icroco.picture.ui.model.CatalogueEntry;
import org.icroco.picture.ui.model.EThumbnailType;
import org.icroco.picture.ui.model.MediaFile;
import org.icroco.picture.ui.persistence.PersistenceService;
import org.icroco.picture.ui.pref.UserPreferenceService;
import org.icroco.picture.ui.task.AbstractTask;
import org.icroco.picture.ui.task.TaskService;
import org.icroco.picture.ui.util.*;
import org.icroco.picture.ui.util.hash.IHashGenerator;
import org.icroco.picture.ui.util.metadata.IMetadataExtractor;
import org.icroco.picture.ui.util.metadata.MetadataHeader;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.boot.context.event.ApplicationStartedEvent;
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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Component
@FxViewBinding(id = "catalog", fxmlLocation = "collection.fxml")
@RequiredArgsConstructor
public class CollectionController extends FxInitOnce {
    private final TaskService           taskService;
    private final PersistenceService    service;
    private final UserPreferenceService pref;
    private final IMetadataExtractor    metadataExtractor;
    private final IHashGenerator        hashGenerator;
    private final DirectoryWatcher      directoryWatcher;
    private final BooleanProperty       disablePathActions = new SimpleBooleanProperty(false);
    @FXML
    private       Accordion             mediaCollections;
    @FXML
    private       VBox                  layout;
    @FXML
    private       Label                 header;
    @FXML
    private       Label                 addCollection;
    @FXML
    private       HBox                  collectionHeader;

    protected void initializedOnce() {
        addCollection.prefHeightProperty().bind(header.heightProperty());
        addCollection.setVisible(false);
        addCollection.disableProperty().bind(disablePathActions);
        layout.setOnMouseEntered(event -> {
            addCollection.setVisible(true);
        });
        layout.setOnMouseExited(event -> {
//            Nodes.hideNodeAfterTime(addCollection, 2, true);
            addCollection.setVisible(false);
        });
        mediaCollections.expandedPaneProperty().addListener(this::titlePaneChanged);
    }

    private void titlePaneChanged(ObservableValue<? extends TitledPane> observableValue, TitledPane oldValue, TitledPane newValue) {
        if (newValue != null) {
            Catalog c = (Catalog) newValue.getUserData();
            pref.getUserPreference().getCollection().setLastViewed((c).id());
            taskService.fxNotifyLater(new WarmThumbnailCacheEvent(c, this));
//            taskService.notifyLater(new CatalogEvent(c, EventType.SELECTED, this));
        }
    }

    @EventListener(ApplicationStartedEvent.class)
    public void applicationStarted() {
        log.info("Application Started.");
        Platform.runLater(() -> initCollections(pref.getUserPreference().getCollection().getLastViewed()));
    }

    private void initCollections(int id) {
        List<Catalog> catalogs = service.findAllCatalog();
        catalogs.stream()
                .peek(this::watchDir)
                .map(this::createTreeView)
                .toList()
                .stream()
                .filter(p -> p.getKey().id() == id)
                .findFirst()
                .ifPresent(p -> {
                    mediaCollections.setExpandedPane(p.getValue().tp());
                    taskService.fxNotifyLater(new MediaCollectionsLoadedEvent(catalogs, this));
                });
    }

    private void watchDir(Catalog catalog) {
        directoryWatcher.registerAll(catalog.path());
    }

    private Pair<Catalog, PaneTreeView> createTreeView(final Catalog catalog) {
        var rootItem = new TreeItem<>(catalog.path());
        var treeView = new TreeView<>(rootItem);
        treeView.setShowRoot(false);
        treeView.setEditable(false);
        rootItem.setExpanded(true);
        treeView.setCellFactory(param -> new TextFieldTreeCell<>(new PathConverter()));
        log.info("Add collection: {}", catalog.path());

        catalog.subPaths().forEach(c -> {
            var p = catalog.path().relativize(c.name());
            addSubDir(rootItem, p);
            log.debug("Path: {}", p);
        });

        final TitledPane title = new TitledPane(catalog.path().getFileName().toString(), treeView);
        title.setUserData(catalog);
        title.setTooltip(new Tooltip(catalog.path() + " (id: " + catalog.id() + ")"));

        FontIcon delete = new FontIcon();
        delete.setId("deleteCollection");
        Label label = new Label("", delete);
        Nodes.addRightGraphic(title, label);
        label.setOnMouseClicked(this::onDeleteCollection);

        mediaCollections.getPanes().add(title);
        mediaCollections.getPanes().sort(Comparator.comparing(TitledPane::getText));

        treeView.getSelectionModel().selectedItemProperty().addListener((v, oldValue, newValue) -> {
            if (newValue != null) {
                log.debug("Tree view selected: {} ", newValue.getValue());
                taskService.fxNotifyLater(new CatalogEntrySelectedEvent(catalog.path(), newValue.getValue(), CollectionController.this));
            }
        });

        return new Pair<>(catalog, new PaneTreeView(title, treeView));
    }

    private void addSubDir(TreeItem<Path> current, Path path) {
        for (int i = 0; i < path.getNameCount(); i++) {
            var child = new TreeItem<>(path.subpath(0, i + 1));
            if (current.getChildren().stream().noneMatch(pathTreeItem -> pathTreeItem.getValue().equals(child.getValue()))) {
                current.getChildren().add(child);
            }
            current = child;
        }
    }

    record TitlePaneEntry(TitledPane titledPane, Catalog catalog) {}

    private void onDeleteCollection(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 1) {
            Node source = (Node) mouseEvent.getSource();
            Nodes.getFirstParent(source, TitledPane.class)
                 .map(t -> new TitlePaneEntry(t, (Catalog) t.getUserData()))
                 .ifPresent(tpe -> {
                     final Alert dlg = new Alert(Alert.AlertType.CONFIRMATION, "");
                     dlg.setTitle("You do want delete Catalog ?");
                     dlg.getDialogPane().setContentText("Do you want to delete Catalog: " + tpe.catalog.path() + " id: " + tpe.catalog.id());

                     dlg.show();
                     dlg.resultProperty()
                        .addListener(o -> {
                            if (dlg.getResult() == ButtonType.OK) {
                                deleteCollection(tpe);
                            }
                        });
                 });
        }
    }

    private void deleteCollection(TitlePaneEntry entry) {
        service.deleteCatalog(entry.catalog);
        mediaCollections.getPanes().remove(entry.titledPane);
        taskService.fxNotifyLater(new CatalogEvent(entry.catalog, EventType.DELETED, this));
        // TODO: Clean Thumbnail Cache and DB.
    }

    @FXML
    private void newCollection(MouseEvent event) {
        final DirectoryChooser directoryChooser  = new DirectoryChooser();
        final File             selectedDirectory = directoryChooser.showDialog(header.getScene().getWindow());

        if (selectedDirectory != null) {
            var rootPath = selectedDirectory.toPath().normalize();
            mediaCollections.getPanes()
                            .stream()
                            .map(t -> new TitlePaneEntry(t, (Catalog) t.getUserData()))
                            .filter(tpe -> rootPath.startsWith(tpe.catalog.path()))
                            .findFirst()
                            .ifPresent(p -> {
                                // TODO: Rather than getting an error, jump to that dir into the proper collection.
                                throw new CollectionException("Path: '%s' is already included into collection item: '%s'".formatted(rootPath, p));
                            });
            mediaCollections.getPanes()
                            .stream()
                            .map(t -> new TitlePaneEntry(t, (Catalog) t.getUserData()))
                            .filter(tpe -> tpe.catalog.path().startsWith(rootPath))
                            .findFirst()
                            .ifPresent(this::deleteCollection); // TODO: Ask user confirmation.

            disablePathActions.set(true);
            taskService.supply(scanDirectory(rootPath))
                       .thenAccept(catalog -> {
                           final var allFiles   = new ArrayBlockingQueue<MediaFile>(catalog.medias().size());
                           final var mediaFiles = List.copyOf(catalog.medias());
                           final var result     = Collections.splitByCoreWithIdx(mediaFiles);
                           final var futures = result.values()
                                                     .map(batchMf -> taskService.supply(hashFiles(batchMf.getValue(),
                                                                                                  batchMf.getKey(),
                                                                                                  result.splitCount()))
                                                                                .thenApply(allFiles::addAll))
                                                     .toArray(new CompletableFuture[0]);

                           CompletableFuture.allOf(futures)
                                            .thenAccept(unused -> {
                                                final var newCatalog = service.saveCatalog(catalog);
                                                Platform.runLater(() -> {
                                                    createTreeView(newCatalog);
                                                    taskService.fxNotifyLater(new ExtractThumbnailEvent(newCatalog, this));
                                                });
                                            });
                       });
        }
    }

    public Task<Catalog> scanDirectory(Path rootPath) {
        return new AbstractTask<>() {
            @Override
            protected Catalog call() throws Exception {
                updateTitle("Scanning directory: " + rootPath.getFileName());
                updateMessage("%s: scanning".formatted(rootPath));
                Set<CatalogueEntry> children;
                var                 now = LocalDate.now();
                try (var s = Files.walk(rootPath)) {
                    children = s.filter(Files::isDirectory)
                                .map(Path::normalize)
                                .filter(p -> !p.equals(rootPath))
                                .filter(FileUtil::isLastDir)
                                .map(p -> CatalogueEntry.builder().name(p).build())
                                .collect(Collectors.toSet());
                }
                try (var images = Files.walk(rootPath)) {
                    final var filteredImages = images.filter(p -> !Files.isDirectory(p))   // not a directory
                                                     .map(Path::normalize)
                                                     .filter(Constant::isSupportedExtension)
                                                     .toList();
                    final var size = filteredImages.size();
                    updateProgress(0, size);
                    return Catalog.builder().path(rootPath)
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

    public Task<List<MediaFile>> hashFiles(final List<MediaFile> mediaFiles, final int batchId, final int nbBatches) {
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
                log.info("Files hasking time: {}", System.currentTimeMillis() - start);
//            // We do not expand now, we're waiting thumbnails creation.
//            createTreeView(catalog);
//            disablePathActions.set(false);
//            taskService.notifyLater(new ExtractThumbnailEvent(catalog, this));
            }
        };
    }

    private MediaFile create(LocalDate now, Path p) {
        final var h = metadataExtractor.header(p);

        return MediaFile.builder()
                .fullPath(p)
                .fileName(p.getFileName().toString())
                .thumbnailType(new SimpleObjectProperty<>(EThumbnailType.ABSENT))
                .hashDate(now)
                .originalDate(h.map(MetadataHeader::orginalDate).orElse(LocalDateTime.now()))
                .build();
    }

    @EventListener(CatalogEvent.class)
    public void catalogEvent(final CatalogEvent event) {
        if (Objects.requireNonNull(event.getType()) == EventType.READY) {
            Platform.runLater(() -> {
                mediaCollections.getPanes()
                                .stream()
                                .filter(tp -> ((Catalog) tp.getUserData()).id() == event.getCatalog().id())
                                .findFirst()
                                .ifPresent(tp -> mediaCollections.setExpandedPane(tp));
            });
        }
    }

    record PaneTreeView(TitledPane tp, TreeView<Path> treeView) {
    }

}
