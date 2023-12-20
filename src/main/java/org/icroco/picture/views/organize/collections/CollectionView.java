package org.icroco.picture.views.organize.collections;

import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Tweaks;
import jakarta.annotation.PostConstruct;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.event.*;
import org.icroco.picture.event.CollectionEvent.EventType;
import org.icroco.picture.event.NotificationEvent.NotificationType;
import org.icroco.picture.model.MediaCollection;
import org.icroco.picture.model.MediaCollectionEntry;
import org.icroco.picture.persistence.PersistenceService;
import org.icroco.picture.util.FileUtil;
import org.icroco.picture.views.CollectionManager;
import org.icroco.picture.views.FxEventListener;
import org.icroco.picture.views.ViewConfiguration;
import org.icroco.picture.views.organize.PathSelection;
import org.icroco.picture.views.pref.UserPreference;
import org.icroco.picture.views.pref.UserPreferenceService;
import org.icroco.picture.views.task.ModernTask;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;

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
    @Getter
    private final CollectionTreeItem
            rootTreeItem =
            new CollectionTreeItem(new CollectionNode(Path.of("Files"), -1, false, false));
    private final TreeView<CollectionNode> treeView         = new TreeView<>(rootTreeItem);

    @Getter
    private final SimpleObjectProperty<PathSelection> pathSelectionProperty = new SimpleObjectProperty<>();
    private final BooleanProperty                     disablePathActions    = new SimpleBooleanProperty(false);
    private final SimpleLongProperty                  catalogSizeProp       = new SimpleLongProperty(0);

    private final static Comparator<TreeItem<CollectionNode>> TREE_ITEM_COMPARATOR =
            Comparator.comparing(ti -> ti.getValue().path().getFileName().toString(),
                                 String.CASE_INSENSITIVE_ORDER);

    @PostConstruct
    protected void initializedOnce() {
        root.setId(ViewConfiguration.V_MEDIA_COLLECTION);
        root.getStyleClass().add(ViewConfiguration.V_MEDIA_COLLECTION);
        root.getStyleClass().add("header");
        rootTreeItem.setExpanded(true);
        rootTreeItem.setValue(new CollectionNode(Path.of("/"), -1, false, false));
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
        addCollection.setTooltip(new Tooltip("""
                                                     Add an existing folder into your collection.
                                                     Remoder folder will work but time analysis will depend of your network performance!
                                                     """));
        addCollection.disableProperty().bind(disablePathActions);
        addCollection.setOnMouseClicked(this::newCollection);

        catalogSize.getStyleClass().add(Styles.TEXT_SMALL);
        catalogSizeProp.addListener((observable, oldValue, newValue) -> {
            if (newValue.intValue() <= 0) {
                catalogSize.setText("");
            } else {
                catalogSize.setText(STR." (\{newValue.toString()})");
            }
        });
//        catalogSize.prefHeightProperty().bind(header.heightProperty());
        Label header = new Label("Collections");
        header.getStyleClass().add(Styles.TITLE_3);
        collectionHeader.getChildren().addAll(header, catalogSize, new Spacer(), addCollection);

        var box = new HBox(treeView);
        VBox.setVgrow(box, Priority.ALWAYS);

        root.setOnMouseEntered(event -> addCollection.setVisible(true));
        root.setOnMouseExited(event -> addCollection.setVisible(rootTreeItem.getChildren().isEmpty()));
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

    private void treeItemSelectionChanged(ObservableValue<? extends TreeItem<CollectionNode>> v,
                                          TreeItem<CollectionNode> oldValue,
                                          TreeItem<CollectionNode> newValue) {
        if (newValue != null) {
            log.trace("Tree view selected: {} ", newValue.getValue());
            pathSelectionProperty.set(new PathSelection(findMainCollectionNode(newValue).getValue().id(),
                                                        newValue.getValue().path(), 0D));
            pref.getUserPreference().setLastViewed(newValue.getValue().id(), newValue.getValue().path());
        }
    }

    private Pair<MediaCollection, TreeItem<CollectionNode>> createTreeView(final MediaCollection mediaCollection) {
        var pathTreeItem = new TreeItem<>(new CollectionNode(mediaCollection.path(), mediaCollection.id(), true, true));
        log.info("Add collection: '{}', into tree view.", mediaCollection.path());
        rootTreeItem.getChildren().add(pathTreeItem);
        rootTreeItem.getChildren().sort(TREE_ITEM_COMPARATOR);
        mediaCollection.subPaths().forEach(c -> {
            var p = c.name();//mediaCollection.path().relativize(c.name());
            addSubDir(pathTreeItem, p, mediaCollection.id());
            log.debug("Path: '{}'", p);
        });

        final TitledPane title = new TitledPane(mediaCollection.path().getFileName().toString(), treeView);
        title.setUserData(mediaCollection.id());
        title.setTooltip(new Tooltip(STR."\{mediaCollection.path()} (id: \{mediaCollection.id()})"));

        return new Pair<>(mediaCollection, pathTreeItem);
    }

    private void updateTreeView(final TreeItem<CollectionNode> treeItem, final MediaCollection mediaCollection) {
        mediaCollection.subPaths().forEach(e -> {
//            var p = mediaCollection.path().relativize(c.name());
            addSubDir(treeItem, e.name(), mediaCollection.id());
            log.debug("Path: {}", e.name());
        });
    }

    private void addSubDir(TreeItem<CollectionNode> current, Path path, int id) {
        path = current.getValue().path().relativize(path);
        for (int i = 0; i < path.getNameCount(); i++) {
            var       subPath = path.subpath(i, i + 1);
            final var c       = current;
            final var child   = c.getValue().path().resolve(subPath);
            current = current.getChildren()
                             .stream()
                             .filter(pathTreeItem -> pathTreeItem.getValue().path().equals(child))
                             .findFirst()
                             .orElseGet(() -> {
                                 var newItem = new TreeItem<>(new CollectionNode(child, id));
                                 c.getChildren().add(newItem);
                                 c.getChildren().sort(TREE_ITEM_COMPARATOR);
                                 return newItem;
                             });
        }
    }

    @Override
    public VBox getRootContent() {
        return root;
    }

    private void newCollection(MouseEvent event) {
        final DirectoryChooser directoryChooser  = new DirectoryChooser();
        final File             selectedDirectory = directoryChooser.showDialog(root.getScene().getWindow());

        if (selectedDirectory != null) {
            var newColPath = selectedDirectory.toPath().normalize();
            if (Files.notExists(newColPath)) {
                final Alert dlg = new Alert(Alert.AlertType.ERROR, "");
                dlg.initOwner(getRootContent().getScene().getWindow());
                dlg.setTitle("Directory doesn't exist");
                dlg.getDialogPane()
                   .setContentText("Looks like the directory is not found: %s ?".formatted(newColPath));

                Nodes.show(dlg, getRootContent().getScene());
                return;
            }
            var optPath = collectionManager.isSameOrSubCollection(newColPath);
            if (optPath.isPresent()) {
                Nodes.searchTreeItemByPredicate(rootTreeItem, colNode -> colNode.path().equals(optPath.get()))
                     .ifPresent(item -> {
                         treeView.getSelectionModel().select(item);
                         item.setExpanded(true);
                         taskService.sendEvent(NotificationEvent.builder()
                                                                .type(NotificationType.INFO)
                                                                .message("Directory already present into collections")
                                                                .source(this)
                                                                .build());
                     });
                return;
            }

            disablePathActions.set(true);
            taskService.supply(ModernTask.builder()
                                         .execute(_ -> detectParentCollection(newColPath))
                                         .onSuccess((myself, _) -> {
                                             // TODO: Ask question if newCollection is on a network volume or if @item is > 1000 ?
                                             var task = collectionManager.newCollection(newColPath, this::askConfirmation);
                                             task.setOnSucceeded(_ -> {
                                                 disablePathActions.set(false);
                                                 createTreeView(task.getValue());
                                                 catalogSizeProp.set(persistenceService.countMediaFiles());
                                             });
                                             task.setOnFailed(_ -> disablePathActions.set(false));
                                         })
                                         .build());
        }
    }

    boolean askConfirmation(long nbFilesToImport) {
        // TODO: Add into preferences
        if (nbFilesToImport > 1000) {
            final Alert dlg = new Alert(Alert.AlertType.CONFIRMATION, "");
            dlg.initOwner(getRootContent().getScene().getWindow());
            dlg.setTitle("Importing a large collection");
            dlg.getDialogPane()
               .setContentText("Do you want to import '%d' files ?".formatted(nbFilesToImport));

            return Nodes.show(dlg, getRootContent().getScene())
                        .map(buttonType -> buttonType == ButtonType.OK)
                        .orElse(false);
        }
        return true;
    }

    private Void detectParentCollection(Path newColPath) {
        // TODO: Do something smarter (do not delete everytinh then recreate all files with thumbnails, ...)
        rootTreeItem.getChildren()
                    .stream()
                    .map(ti -> persistenceService.getMediaCollection(ti.getValue().id()))
                    .filter(mc -> mc.path().startsWith(newColPath))
                    .forEach(collectionManager::deleteCollection);
//                    .ifPresent(collectionManager::deleteCollection); // TODO: Ask user confirmation ?

        return null;
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

    /////////////////////////////////////////////
    //// Event Listeners (Fx Thread guaranted)
    /////////////////////////////////////////////
    @FxEventListener
    public void onDeleteCollection(DeleteCollectionEvent event) {
        persistenceService.findMediaCollection(event.getMcId())
                          .ifPresent(mc -> {
                              final Alert dlg = new Alert(Alert.AlertType.CONFIRMATION, "");
                              dlg.initOwner(root.getScene().getWindow());
                              dlg.setTitle("You do want delete MediaCollection ?");
                              dlg.getDialogPane()
                                 .setContentText("""
                                                         Do you want to delete MediaCollection:
                                                           Path:   %s
                                                           Id:     %d
                                                           #Items: %s
                                                         """.formatted(mc.path(), mc.id(), mc.medias().size()));

                              Nodes.show(dlg, getRootContent().getScene())
                                   .filter(buttonType -> buttonType == ButtonType.OK)
                                   .ifPresent(_ -> collectionManager.deleteCollection(mc,
                                                                                      () -> catalogSizeProp.set(persistenceService.countMediaFiles())));
                          });
    }

    @FxEventListener
    public void initCollections(CollectionsLoadedEvent event) {
        UserPreference.Collection collectionPref = pref.getUserPreference().getCollection();
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
                 catalogSizeProp.set(persistenceService.countMediaFiles());
             });
    }

    @FxEventListener
    public void catalogEvent(final CollectionEvent event) {
        log.info("Catalog CollectionEvent: {}", event);
        if (Objects.requireNonNull(event.getType()) == EventType.READY) {
            rootTreeItem.getChildren()
                        .stream()
                        .filter(treeItem -> treeItem.getValue().id() == event.getMcId())
                        .findFirst()
                        .ifPresent(treeItem -> {
                            treeItem.setExpanded(true);
                            if (treeView.getSelectionModel().isEmpty()) {
                                treeView.getSelectionModel().select(treeItem);
                            }
                        });
        } else if (Objects.requireNonNull(event.getType()) == EventType.DELETED) {
            rootTreeItem.getChildren().removeIf(pathTreeItem -> pathTreeItem.getValue().id() == event.getMcId());
        }
    }

    @FxEventListener
    public void updateImages(CollectionUpdatedEvent event) {
        if (event.isEmpty()) {
            return;
        }
        log.info("Recieved CollectionUpdatedEvent on collection: '{}', newItems: '{}', deletedItems: '{}', modifiedItems: '{}'",
                 event.getMcId(),
                 event.getNewItems().size(),
                 event.getDeletedItems().size(),
                 event.getModifiedItems().size());
        if (!event.getNewItems().isEmpty()) {
            var mc = persistenceService.getMediaCollection(event.getMcId());
            var directories = event.getNewItems()
                                   .stream()
                                   .map(mediaFile -> mediaFile.getFullPath().normalize().getParent())
                                   .distinct()
                                   .filter(FileUtil::isLastDir)
                                   .filter(p -> !p.equals(mc.path()))
                                   .map(p -> MediaCollectionEntry.builder().name(p).build())
                                   .toList();
            mc.subPaths().addAll(directories);

            // TODO: Update raw by raw mediaCollectrion entries.
            var mcSaved = persistenceService.saveCollection(mc);

            rootTreeItem.getChildren()
                        .stream()
                        .filter(treeItem -> treeItem.getValue().id() == event.getMcId())
                        .findFirst()
                        .ifPresent(treeItem -> updateTreeView(treeItem, mcSaved));
        }
        catalogSizeProp.set(persistenceService.countMediaFiles());
    }

    @FxEventListener
    public void selectCollectionAndDirectory(ShowOrganizeEvent event) {
        log.info("Receive ShowOrganizeEvent: {}", event);
        Nodes.searchTreeItemByPredicate(rootTreeItem, collectionNode -> collectionNode.path().equals(event.getDirectory()))
             .ifPresent(ti -> treeView.getSelectionModel().select(ti));
    }

    @FxEventListener
    public void updateCollectionStatus(CollectionsStatusEvent event) {
        var statuses = event.getStatuses();
        rootTreeItem.getChildren().forEach(treeItem -> {
            var status = statuses.getOrDefault(treeItem.getValue().id(), false);
            log.debug("Collection ID: {} ({}), Status: {}", treeItem.getValue().id(), treeItem.getValue().path(), status);

            if (status != treeItem.getValue().pathExist()) {
                treeItem.setValue(treeItem.getValue().withPathExist(status));
            }
        });
    }
}
