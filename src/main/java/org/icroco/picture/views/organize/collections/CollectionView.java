package org.icroco.picture.views.organize.collections;

import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Tweaks;
import jakarta.annotation.PostConstruct;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.util.Pair;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.event.CollectionEvent;
import org.icroco.picture.event.CollectionEvent.EventType;
import org.icroco.picture.event.CollectionUpdatedEvent;
import org.icroco.picture.event.CollectionsLoadedEvent;
import org.icroco.picture.event.DeleteCollectionEvent;
import org.icroco.picture.model.MediaCollection;
import org.icroco.picture.model.MediaCollectionEntry;
import org.icroco.picture.persistence.PersistenceService;
import org.icroco.picture.util.FileUtil;
import org.icroco.picture.views.FxEventListener;
import org.icroco.picture.views.organize.PathSelection;
import org.icroco.picture.views.pref.UserPreference;
import org.icroco.picture.views.pref.UserPreferenceService;
import org.icroco.picture.views.task.TaskService;
import org.icroco.picture.views.util.FxView;
import org.icroco.picture.views.util.Nodes;
import org.icroco.picture.views.util.Styles;
import org.icroco.picture.views.util.widget.FxUtil;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollectionView implements FxView<VBox> {
    private final TaskService           taskService;
    private final PersistenceService    persistenceService;
    private final UserPreferenceService pref;
    private final CollectionManager     collectionManager;

    private final VBox                     root             = new VBox();
    private final Label                    catalogSize      = new Label();
    private final Button                   addCollection    = new Button();
    private final HBox                     collectionHeader = new HBox();
    private final CollectionTreeItem       rootTreeItem     = new CollectionTreeItem(new CollectionNode(Path.of("Files"), -1, false));
    private final TreeView<CollectionNode> treeView         = new TreeView<>(rootTreeItem);

    @Getter
    private final SimpleObjectProperty<PathSelection> pathSelectionProperty = new SimpleObjectProperty<>();
    private final BooleanProperty                     disablePathActions    = new SimpleBooleanProperty(false);
    private final SimpleIntegerProperty               catalogSizeProp       = new SimpleIntegerProperty(0);

    public record CollectionNode(Path path, int id, boolean isColTopLevel) {
        public CollectionNode(Path path, int id) {
            this(path, id, false);
        }

        boolean isRootCollection() {
            return id >= 0;
        }
    }

    @PostConstruct
    protected void initializedOnce() {
        root.getStyleClass().add("v-collections");
        root.getStyleClass().add("header");
        rootTreeItem.setExpanded(true);
        treeView.setMinHeight(250);
        treeView.setShowRoot(false);
        treeView.setEditable(false);
        treeView.getSelectionModel().selectedItemProperty().addListener(this::treeItemSelectionChanged);
        treeView.setCellFactory(param -> {
            var cell = new CollectionTreeCell(taskService);
            cell.getRoot().addEventHandler(MouseEvent.ANY, event -> {
                if (event.getClickCount() == 1
                    && event.getButton().equals(MouseButton.PRIMARY)
                    && event.getEventType().getName().equals("MOUSE_CLICKED")) {
                    var path = pathSelectionProperty.get();
                    if (path != null) {
                        pathSelectionProperty.set(new PathSelection(path.mediaCollectionId(), path.subPath()));
                    }
                }
            });
            return cell;
        });
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
                return new CollectionNode(Path.of(string), -1, false);
            }
        };
    }

    private void treeItemSelectionChanged(ObservableValue<? extends TreeItem<CollectionNode>> v,
                                          TreeItem<CollectionNode> oldValue,
                                          TreeItem<CollectionNode> newValue) {
        if (newValue != null) {
            log.trace("Tree view selected: {} ", newValue.getValue());
            pathSelectionProperty.set(new PathSelection(findMainCollectionNode(newValue).getValue().id(),
                                                        newValue.getValue().path, 0D));
            pref.getUserPreference().setLastViewed(newValue.getValue().id, newValue.getValue().path);
        }
    }

    private Pair<MediaCollection, TreeItem<CollectionNode>> createTreeView(final MediaCollection mediaCollection) {
        var pathTreeItem = new TreeItem<>(new CollectionNode(mediaCollection.path(), mediaCollection.id(), true));
        log.info("Add collection: {}", mediaCollection.path());
        catalogSizeProp.set(catalogSizeProp.get() + mediaCollection.medias().size());
        rootTreeItem.getChildren().add(pathTreeItem);
        mediaCollection.subPaths().forEach(c -> {
            var p = mediaCollection.path().relativize(c.name());
            addSubDir(pathTreeItem, p, mediaCollection.id());
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
            addSubDir(treeItem, p, mediaCollection.id());
            log.debug("Path: {}", p);
        });
    }

    private void addSubDir(TreeItem<CollectionNode> current, Path path, int id) {
        for (int i = 0; i < path.getNameCount(); i++) {
            var child = new TreeItem<>(new CollectionNode(path.subpath(0, i + 1), id));
            if (current.getChildren().stream().noneMatch(pathTreeItem -> pathTreeItem.getValue().equals(child.getValue()))) {
                current.getChildren().add(child);
            }
            current.getChildren()
                   .sort(Comparator.comparing(ti -> ti.getValue().path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER));
            current = child;
        }
    }

    @Override
    public VBox getRootContent() {
        return root;
    }

    @FxEventListener
    public void onDeleteCollection(DeleteCollectionEvent event) {
//        if (mouseEvent.getClickCount() == 1) {
//            Node source = (Node) mouseEvent.getSource();
//            Nodes.getFirstParent(source, TitledPane.class)
//                 .map(t -> new TitlePaneEntry(t, (int) t.getUserData()))
        // TODO: Create Task (can takes times);
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
                                         collectionManager.deleteCollection(mc);
                                         catalogSizeProp.set(catalogSizeProp.get() - mc.medias().size());
                                     }
                                 });
                          });
//    }
    }

//    private void deleteCollection(MediaCollection entry) {
//        taskService.supply(() -> {
//            rootTreeItem.getChildren().removeIf(pathTreeItem -> pathTreeItem.getValue().path.equals(entry.path()));
//            persistenceService.deleteMediaCollection(entry.id());
//            taskService.sendEvent(CollectionEvent.builder().mediaCollection(entry).type(EventType.DELETED).source(this).build());
//            // TODO: Clean Thumbnail Cache and DB.
//        });
//    }

    //    @FXML
    private void newCollection(MouseEvent event) {
        final DirectoryChooser directoryChooser  = new DirectoryChooser();
        final File             selectedDirectory = directoryChooser.showDialog(root.getScene().getWindow());

        if (selectedDirectory != null) {
            var newColPath            = selectedDirectory.toPath().normalize();
            var existingSubCollection = isSubCollection(newColPath);
            if (existingSubCollection.isPresent()) {
                treeView.getSelectionModel().select(existingSubCollection.get());
                existingSubCollection.get().setExpanded(true);
                return;
            }

            detectParentCollection(newColPath);

            disablePathActions.set(true);
            // TODO: Ask question if newCollection is on a network volume or if @item is > 1000 ?
            var task = collectionManager.newCollection(newColPath);
            task.setOnSucceeded(evt -> {
                disablePathActions.set(false);
                createTreeView(task.getValue());
            });
            task.setOnFailed(evt -> disablePathActions.set(false));
        }
    }

    private void detectParentCollection(Path newColPath) {
        // Do something smarter (do not delete everytinh then recreate all files with thumbnails, ...)
        rootTreeItem.getChildren()
                    .stream()
                    .map(ti -> persistenceService.getMediaCollection(ti.getValue().id))
                    .filter(mc -> mc.path().startsWith(newColPath))
                    .forEach(collectionManager::deleteCollection);
//                    .ifPresent(collectionManager::deleteCollection); // TODO: Ask user confirmation.
    }

    private Optional<TreeItem<CollectionNode>> isSubCollection(Path rootPath) {
        return rootTreeItem.getChildren()
                           .stream()
                           .filter(treeItem -> rootPath.startsWith(persistenceService.getMediaCollection(treeItem.getValue()
                                                                                                                 .id())
                                                                                     .path()))
                           .findFirst()
                           .flatMap(treeItem -> Nodes.searchTreeItem(treeItem,
                                                                     (Predicate<CollectionNode>) cn -> cn.path.equals(
                                                                             treeItem.getValue()
                                                                                     .path()
                                                                                     .relativize(rootPath))));
    }


    //        @Async(ImageInConfiguration.FX_EXECUTOR)
    @FxEventListener
    public void initCollections(CollectionsLoadedEvent event) {
        UserPreference.Collection collectionPref = pref.getUserPreference().getCollection();
//        var nextSelection = PathSelection.builder()
//                                         .mediaCollectionId(collectionPref.getLastViewed())
//                                         .subPath(collectionPref.getLastPath())
//                                         .build();
        log.info("Collection Loaded: {}, saved: {}", event, collectionPref);
        event.getMediaCollections()
             .stream()
             .map(this::createTreeView)
             .toList()
             .stream()
             .filter(p -> p.getKey().id() == collectionPref.getLastViewed())
             .findFirst()
             .ifPresent(p -> {
                 log.info("Expand: {}", p.getKey().path());
                 p.getValue().setExpanded(true);
                 // TODO: Select subpath
                 var item = searchTreeItem(p.getValue(), collectionPref.getLastPath());
                 if (item != null) {
                     treeView.getSelectionModel().select(item);
                 } else {
                     treeView.getSelectionModel().select(p.getValue());
                 }
             });
    }

    @Nullable
    private TreeItem<CollectionNode> searchTreeItem(@NonNull TreeItem<CollectionNode> item, @Nullable Path path) {
        if (path == null) {
            return null;
        }

        if (item.getValue().path().equals(path)) {
            return item; // hit!
        }

        // continue on the children:
        TreeItem<CollectionNode> result = null;
        for (TreeItem<CollectionNode> child : item.getChildren()) {
            result = searchTreeItem(child, path);
            if (result != null) {
                return result; // hit!
            }
        }
        //no hit:
        return null;
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
        } else if (Objects.requireNonNull(event.getType()) == EventType.DELETED) {
            catalogSizeProp.set(catalogSizeProp.get() - event.getMediaCollection().medias().size());
            rootTreeItem.getChildren().removeIf(pathTreeItem -> pathTreeItem.getValue().path.equals(event.getMediaCollection().path()));
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
