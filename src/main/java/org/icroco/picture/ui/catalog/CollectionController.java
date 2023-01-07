package org.icroco.picture.ui.catalog;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
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
import org.icroco.picture.ui.event.CatalogSelectedEvent;
import org.icroco.picture.ui.event.CatalogEntrySelectedEvent;
import org.icroco.picture.ui.event.TaskEvent;
import org.icroco.picture.ui.model.Catalog;
import org.icroco.picture.ui.model.CatalogueEntry;
import org.icroco.picture.ui.model.MediaFile;
import org.icroco.picture.ui.persistence.PersistenceService;
import org.icroco.picture.ui.pref.UserPreferenceService;
import org.icroco.picture.ui.util.Constant;
import org.icroco.picture.ui.util.FileUtil;
import org.icroco.picture.ui.util.PathConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.stream.Collectors;

@Slf4j
@Component
@FxViewBinding(id = "catalog", fxmlLocation = "collection.fxml")
@RequiredArgsConstructor
public class CollectionController extends FxInitOnce {
    @Qualifier(Constant.APPLICATION_EVENT_MULTICASTER)
    private final ApplicationEventMulticaster eventBus;
    private final PersistenceService          service;
    private final UserPreferenceService       pref;

    @FXML
    private       Accordion       paths;
    @FXML
    private       VBox            layout;
    @FXML
    private       Label           header;
    @FXML
    private       Label           addCollection;
    @FXML
    private       HBox            collectionHeader;
    private final BooleanProperty disablePathActions = new SimpleBooleanProperty(false);

//    @PostConstruct
//    public void postConstruct() {
//        log.info("POST CONSTRUCT");
//    }

    protected void initializedOnce() {
        log.info("Initializing");

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
        paths.expandedPaneProperty().addListener(this::titlePaneChanged);
    }

    @EventListener(ApplicationStartedEvent.class)
    public void applicationStarted() {
        log.info("Application Started.");
        Platform.runLater(() -> {
//            addCollection.setVisible(false);
            service.findAllCatalog()
                   .stream()
                   .map(this::createTreeView)
                   .toList()
                   .stream()
                   .filter(p -> p.getKey().id() == pref.getUserPreference().getCollection().getLastViewed())
                   .findFirst()
                   .ifPresent(p -> {
                       paths.setExpandedPane(p.getValue().tp());
                   });
        });
    }

    private void titlePaneChanged(ObservableValue<? extends TitledPane> observableValue, TitledPane oldValue, TitledPane newValue) {
        if (newValue != null) {
            pref.getUserPreference().getCollection().setLastViewed(((Catalog) newValue.getUserData()).id());
            eventBus.multicastEvent(new CatalogSelectedEvent((Catalog) newValue.getUserData(), this));
        }
    }

    @FXML
    private void newCollection(MouseEvent event) {
        final DirectoryChooser directoryChooser  = new DirectoryChooser();
        final File             selectedDirectory = directoryChooser.showDialog(header.getScene().getWindow());

        if (selectedDirectory != null) {
            var rootPath = selectedDirectory.toPath().normalize();
            paths.getPanes()
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
            eventBus.multicastEvent(new TaskEvent(scanDirectory(rootPath), this));
        }
    }

    public Task<Catalog> scanDirectory(Path rootPath) {
        return new Task<>() {
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
                                                                                          .map(i -> MediaFile.builder()
                                                                                                  .fullPath(i)
                                                                                                  .fileName(i.getFileName().toString())
                                                                                                  .originalDate(now)
                                                                                                  .build())
                                                                                          .collect(Collectors.toSet()))
                                                                    .build());
                    }
                }
            }

            @Override
            protected void succeeded() {
                var record       = getValue();
                var paneTreeView = createTreeView(record);
                paths.setExpandedPane(paneTreeView.getValue().tp);
                disablePathActions.set(false);
            }
        };
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

        final TitledPane tp = new TitledPane(catalog.path().getFileName().toString(), treeView);
        tp.setUserData(catalog);
        tp.setTooltip(new Tooltip(catalog.path() + " (id: " + catalog.id() + ")"));
        paths.getPanes().add(tp);

        treeView.getSelectionModel().selectedItemProperty().addListener((v, oldValue, newValue) -> {
            if (newValue != null) {
                log.debug("Tree view selected: {} ", newValue.getValue());
                eventBus.multicastEvent(new CatalogEntrySelectedEvent(catalog.path(), newValue.getValue(), CollectionController.this));
            }
        });

        return new Pair<>(catalog, new PaneTreeView(tp, treeView));
    }

    private void addSubDir(TreeItem<Path> current, Path path) {
        for (int i = 0; i < path.getNameCount(); i++) {
            var child = new TreeItem<>(path.subpath(0, i+1));
            if (current.getChildren().stream().noneMatch(pathTreeItem -> pathTreeItem.getValue().equals(child.getValue()))) {
                current.getChildren().add(child);
            }
            current = child;
        }
    }

}
