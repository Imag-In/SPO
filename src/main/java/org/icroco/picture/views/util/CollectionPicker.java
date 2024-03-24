package org.icroco.picture.views.util;

import atlantafx.base.controls.CustomTextField;
import atlantafx.base.theme.Styles;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.model.MediaCollection;
import org.icroco.picture.persistence.PersistenceService;
import org.icroco.picture.util.I18N;
import org.icroco.picture.util.I18NConstant;
import org.icroco.picture.util.LangUtils;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2OutlinedMZ;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;

import java.nio.file.Path;
import java.util.Comparator;

@Slf4j
public class CollectionPicker extends CustomTextField {

    private final SimpleBooleanProperty showRootProperty = new SimpleBooleanProperty(false);
    private final SimpleObjectProperty<Path> collectionProperty = new SimpleObjectProperty<>();

    private final TreeView<Path>           treeView;
    private final FilterableTreeItem<Path> rootTreeItem;

    public CollectionPicker(PersistenceService persistenceService, I18N i18N, int prefWidth) {
        this(new FontIcon(MaterialDesignF.FOLDER_OPEN_OUTLINE), persistenceService, i18N, prefWidth);
    }

    public CollectionPicker(FontIcon icon, PersistenceService persistenceService, I18N i18N, int prefWidth) {
        super();
        setRight(icon);
        icon.setCursor(Cursor.HAND);
        icon.setOnMouseClicked(event -> chooseCollectionPath(event, persistenceService, i18N));

        rootTreeItem = new FilterableTreeItem<>(Path.of(i18N.get(I18NConstant.COL_PICKER_SHOW_TREE_ROOT)));
        treeView = new TreeView<>(rootTreeItem);

        pseudoClassStateChanged(atlantafx.base.theme.Styles.STATE_DANGER, true);
        textProperty().addListener((_, _, newV) -> {
            pseudoClassStateChanged(atlantafx.base.theme.Styles.STATE_DANGER, false);
            pseudoClassStateChanged(atlantafx.base.theme.Styles.STATE_SUCCESS, false);
            if (LangUtils.isBlank(newV)) {
                pseudoClassStateChanged(atlantafx.base.theme.Styles.STATE_DANGER, true);
            } else {
                pseudoClassStateChanged(Styles.STATE_SUCCESS, true);
            }
        });
        HBox.setHgrow(this, Priority.ALWAYS);

        setPrefWidth(prefWidth);
    }


    private void chooseCollectionPath(MouseEvent e, PersistenceService persistenceService, I18N i18N) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "", ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle(i18N.get(I18NConstant.COL_PICKER_CHOOSE_TGT_TITLE));
        alert.setHeaderText(null);

        treeView.showRootProperty().bind(showRootProperty);
        rootTreeItem.setExpanded(true);
        rootTreeItem.getChildren().clear();
        persistenceService.findAllMediaCollection()
                          .forEach(mediaCollection -> createTreeView(rootTreeItem, mediaCollection));
        treeView.setCellFactory(_ -> new TreeCell<>() {
            @Override
            protected void updateItem(Path item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getFileName().toString());
            }
        });
        treeView.setMinWidth(400);
        treeView.setPrefWidth(300);
        treeView.setEditable(false);
        treeView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        rootTreeItem.setExpanded(true);
        var box    = new VBox(10);
        var search = new CustomTextField();
        search.setPrefWidth(300);
        search.setLeft(new FontIcon(Material2OutlinedMZ.SEARCH));
        search.setRight(new FontIcon(MaterialDesignC.CLOSE));
        search.getRight().setOnMouseClicked(event -> search.setText(""));
        search.getRight().setCursor(Cursor.HAND);
        search.setPromptText("Filter path");

        persistenceService.findAllMediaCollection()
                          .stream()
                          .flatMap(mc -> mc.getSubdir().stream())
                          .map(Path::getFileName)
                          .toList();

//        TextFields.bindAutoCompletion(search, persistenceService.findAllMediaCollection()
//                                                                .stream()
//                                                                .flatMap(mc -> mc.getSubdir().stream())
//                                                                .map(Path::getFileName)
//                                                                .toList());

        rootTreeItem.predicateProperty()
                    .bind(search.textProperty()
                                .map(s -> (_, path) -> path.toString().contains(s)));

        box.getChildren().addAll(search, treeView);
        alert.getDialogPane().contentProperty().setValue(box);
        Nodes.show(alert, getParent().getScene()).filter(buttonType -> buttonType == ButtonType.OK)
             .ifPresent(_ -> {
                 if (treeView.getSelectionModel().getSelectedItem() != null) {
                     log.info("OK: {}", treeView.getSelectionModel().getSelectedItem().getValue());
                     var targetCollection = treeView.getSelectionModel().getSelectedItem().getValue();
                     collectionProperty.setValue(targetCollection);
                     setText(targetCollection.toString());
                 }
             });
    }

    public void selectRoot() {
        treeView.getSelectionModel().select(rootTreeItem);
        setText(rootTreeItem.getValue().toString());
    }

    public boolean isRootSelected() {
        return treeView.getSelectionModel().getSelectedItem() == rootTreeItem;
    }

    private void createTreeView(final FilterableTreeItem<Path> rootTreeItem, final MediaCollection mediaCollection) {
        var pathTreeItem = new FilterableTreeItem<>(mediaCollection.path());
        pathTreeItem.setExpanded(true);
//        rootTreeItem.expandedProperty().bind(expandAll);
        rootTreeItem.getInternalChildren().add(pathTreeItem);
        log.info("Root Path: {}", pathTreeItem.getValue());

        mediaCollection.getSubdir()
                       .forEach(subPath -> {
                           var p = mediaCollection.path().resolve(subPath);
                           addSubDir(pathTreeItem, p, mediaCollection.id());
                       });
    }

    private void addSubDir(FilterableTreeItem<Path> current, Path path, int id) {
        path = current.getValue().relativize(path);
        for (int i = 0; i < path.getNameCount(); i++) {
            var       subPath = path.subpath(i, i + 1);
            final var c       = current;
            final var child   = c.getValue().resolve(subPath);
            current = current.getInternalChildren()
                             .stream()
                             .filter(pathTreeItem -> pathTreeItem.getValue().equals(child))
                             .map(pathTreeItem -> (FilterableTreeItem<Path>) pathTreeItem)
                             .findFirst()
                             .orElseGet(() -> {
                                 var newItem = new FilterableTreeItem<>(child);
                                 c.getInternalChildren().add(newItem);
                                 c.getInternalChildren()
                                  .sort(Comparator.comparing(ti -> ti.getValue().getFileName().toString(),
                                                             String.CASE_INSENSITIVE_ORDER));
//                                 c.expandedProperty().bind(expandAll);
                                 c.setExpanded(true);

                                 return newItem;
                             });
        }
    }

    public ReadOnlyObjectProperty<Path> collectionProperty() {
        return collectionProperty;
    }

    public void setShowRootProperty(boolean value) {
        showRootProperty.set(value);
    }
}
