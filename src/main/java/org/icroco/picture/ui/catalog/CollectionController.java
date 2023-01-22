package org.icroco.picture.ui.catalog;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
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
import org.icroco.javafx.FxInitOnce;
import org.icroco.javafx.FxViewBinding;
import org.icroco.picture.ui.event.CatalogEntrySelectedEvent;
import org.icroco.picture.ui.event.CatalogEvent;
import org.icroco.picture.ui.event.CatalogEvent.EventType;
import org.icroco.picture.ui.event.GenerateThumbnailEvent;
import org.icroco.picture.ui.event.WarmThumbnailCacheEvent;
import org.icroco.picture.ui.model.Catalog;
import org.icroco.picture.ui.model.CatalogueEntry;
import org.icroco.picture.ui.model.MediaFile;
import org.icroco.picture.ui.persistence.PersistenceService;
import org.icroco.picture.ui.pref.UserPreferenceService;
import org.icroco.picture.ui.task.AbstractTask;
import org.icroco.picture.ui.task.TaskService;
import org.icroco.picture.ui.util.Constant;
import org.icroco.picture.ui.util.FileUtil;
import org.icroco.picture.ui.util.Nodes;
import org.icroco.picture.ui.util.PathConverter;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.stream.Collectors;

@Slf4j
@Component
@FxViewBinding(id = "catalog", fxmlLocation = "collection.fxml")
@RequiredArgsConstructor
public class CollectionController extends FxInitOnce {
    private final PersistenceService    persistenceService;
    private final TaskService           taskService;
    private final PersistenceService    service;
    private final UserPreferenceService pref;

    @FXML
    private       Accordion       catalogs;
    @FXML
    private       VBox            layout;
    @FXML
    private       Label           header;
    @FXML
    private       Label           addCollection;
    @FXML
    private       HBox            collectionHeader;
    private final BooleanProperty disablePathActions = new SimpleBooleanProperty(false);

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
        catalogs.expandedPaneProperty().addListener(this::titlePaneChanged);
    }

    @EventListener(ApplicationStartedEvent.class)
    public void applicationStarted() {
        log.info("Application Started.");
        Platform.runLater(() -> createCatalogs(pref.getUserPreference().getCollection().getLastViewed()));
    }

    private void titlePaneChanged(ObservableValue<? extends TitledPane> observableValue, TitledPane oldValue, TitledPane newValue) {
        if (newValue != null) {
            Catalog c = (Catalog) newValue.getUserData();
            pref.getUserPreference().getCollection().setLastViewed((c).id());
            taskService.notifyLater(new WarmThumbnailCacheEvent(c, this));
            taskService.notifyLater(new CatalogEvent(c, EventType.SELECTED, this));
        }
    }

    @FXML
    private void newCollection(MouseEvent event) {
        final DirectoryChooser directoryChooser  = new DirectoryChooser();
        final File             selectedDirectory = directoryChooser.showDialog(header.getScene().getWindow());

        if (selectedDirectory != null) {
            var rootPath = selectedDirectory.toPath().normalize();
            catalogs.getPanes()
                    .stream()
                    .map(TitledPane::getContent)
                    .map(TreeView.class::cast)
                    .map(TreeView::getRoot)
                    .map(TreeItem::getValue)
                    .map(Path.class::cast)
                    .map(Path::normalize)
                    .filter(p -> rootPath.toString().startsWith(p.toString()))
                    .findFirst()
                    .ifPresent(p -> {
                        throw new CollectionException("Path: '%s' is already included into collection item: '%s'".formatted(rootPath, p));
                    });

            disablePathActions.set(true);
            taskService.supply(scanDirectory(rootPath));
        }
    }

    public Task<Catalog> scanDirectory(Path rootPath) {
        return new AbstractTask<>() {
            @Override
            protected Catalog call() throws Exception {
                updateMessage("Scanning '" + rootPath + "'");
                try (var s = Files.walk(rootPath)) {
                    var children = s.filter(Files::isDirectory)
                                    .filter(p -> !p.normalize().equals(rootPath))
                                    .filter(FileUtil::isLastDir)
                                    .map(p -> CatalogueEntry.builder().name(p).build())
                                    .collect(Collectors.toSet());

                    try (var images = Files.walk(rootPath)) {
                        var filteredImages = images.filter(p -> !Files.isDirectory(p))   // not a directory
                                                   .filter(Constant::isSupportedExtension)
                                                   .toList();
                        var now = LocalDate.now();
                        return service.saveCatalog(Catalog.builder().path(rootPath)
                                                                    .subPaths(children)
                                                                    .medias(filteredImages.stream()
                                                                                          .map(i -> persistenceService.findByPath(i)
                                                                                                                      .orElseGet(() -> create(i, now)))
                                                                                          .collect(Collectors.toSet()))
                                                                    .build());
                    }
                }
            }

            @Override
            protected void succeeded() {
                var catalog = getValue();
                log.info("Entries: {}", catalog.medias().size());
                var paneTreeView = createTreeView(catalog);
//                catalogs.setExpandedPane(paneTreeView.getValue().tp);
                disablePathActions.set(false);
                taskService.notifyLater(new GenerateThumbnailEvent(catalog, this));
            }

            @Override
            protected void failed() {
                disablePathActions.set(false);
                log.error("While scanning dir: '{}'", rootPath, getException());
                super.failed();
            }
        };
    }


    private MediaFile create(Path p, LocalDate now) {
        return MediaFile.builder()
                .fullPath(p)
                .fileName(p.getFileName().toString())
                .originalDate(now)
                .build();
    }

    record PaneTreeView(TitledPane tp, TreeView<Path> treeView) {}

    private Pair<Catalog, PaneTreeView> createTreeView(final Catalog catalog) {
        var rootItem = new TreeItem<>(catalog.path());
        var treeView = new TreeView<>(rootItem);
        treeView.setShowRoot(false);
        treeView.setEditable(false);
        rootItem.setExpanded(true);
        treeView.setCellFactory(param -> new TextFieldTreeCell<>(new PathConverter()));
        log.info("create Collection: {}", catalog.path());

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

        catalogs.getPanes().add(title);
        catalogs.getPanes().sort(Comparator.comparing(TitledPane::getText));

        treeView.getSelectionModel().selectedItemProperty().addListener((v, oldValue, newValue) -> {
            if (newValue != null) {
                log.debug("Tree view selected: {} ", newValue.getValue());
                taskService.notifyLater(new CatalogEntrySelectedEvent(catalog.path(), newValue.getValue(), CollectionController.this));
            }
        });

        return new Pair<>(catalog, new PaneTreeView(title, treeView));
    }

    private void onDeleteCollection(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 1) {
            Node source = (Node) mouseEvent.getSource();
            Nodes.getFirstParent(source, TitledPane.class)
                 .map(Node::getUserData)
                 .map(Catalog.class::cast)
                 .ifPresent(c -> {
                     final Alert dlg = new Alert(Alert.AlertType.CONFIRMATION, "");
                     dlg.setTitle("You do want delete Catalog ?");
                     dlg.getDialogPane()
                        .setContentText("Do you want to delete Catalog: " + c.path() + " id: " + c.id());

                     dlg.show();
                     dlg.resultProperty()
                        .addListener(o -> {
                            if (dlg.getResult() == ButtonType.OK) {
                                service.deleteCatalog(c);
                                catalogs.getPanes().remove(Nodes.getFirstParent(source, TitledPane.class).orElseThrow());
                                taskService.notifyLater(new CatalogEvent(c, EventType.DELETED, this));
                                // TODO: Clean Thumbnail Cache and DB.
                            }
                        });
                 });
        }
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

    @EventListener(CatalogEvent.class)
    public void catalogEvent(CatalogEvent event) {
        switch (event.getType()) {
            case SELECTED -> catalogs.getPanes()
                                     .stream()
                                     .filter(tp -> ((Catalog) tp.getUserData()).id() == event.getCatalog().id())
                                     .findFirst()
                                     .ifPresent(tp -> catalogs.setExpandedPane(tp));
        }
    }

    private void createCatalogs(int id) {
        service.findAllCatalog()
               .stream()
               .map(this::createTreeView)
               .toList()
               .stream()
               .filter(p -> p.getKey().id() == id)
               .findFirst()
               .ifPresent(p -> {
                   catalogs.setExpandedPane(p.getValue().tp());
               });
    }

}
