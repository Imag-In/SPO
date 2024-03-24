package org.icroco.picture.views.repair;

import atlantafx.base.controls.Card;
import jakarta.annotation.PostConstruct;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTreeTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.model.HashDuplicate;
import org.icroco.picture.model.MediaFile;
import org.icroco.picture.persistence.PersistenceService;
import org.icroco.picture.util.I18N;
import org.icroco.picture.views.CollectionManager;
import org.icroco.picture.views.task.ModernTask;
import org.icroco.picture.views.task.TaskService;
import org.icroco.picture.views.util.CollectionPicker;
import org.icroco.picture.views.util.HyperlinkTreeCell;
import org.icroco.picture.views.util.MaskerPane;
import org.icroco.picture.views.util.Nodes;
import org.icroco.picture.views.util.widget.FxUtil;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignT;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Component
@Lazy
@Slf4j
public class DuplicateByMetadataTool implements RepairTool {
    private final TaskService taskService;
    private final CollectionManager  collectionManager;
    private final PersistenceService persistenceService;
    private final I18N        i18N;

    private final VBox  vb      = new VBox();
    private final Label lbNbDup = new Label("");

    private       TreeTableView<TableRow> treeTableHash;
    private final MaskerPane<VBox> maskerPane = new MaskerPane<>();
    private final CollectionPicker collectionPicker;

    public DuplicateByMetadataTool(TaskService taskService,
                                   CollectionManager collectionManager,
                                   PersistenceService persistenceService,
                                   I18N i18N) {
        this.taskService = taskService;
        this.collectionManager = collectionManager;
        this.persistenceService = persistenceService;
        this.i18N = i18N;
        this.collectionPicker = new CollectionPicker(persistenceService, i18N, 300);
    }

    public enum EViewMode {
        DUPLICATE,
        COLLECTION
    }

    @Getter
    @Setter
    @ToString
    public static class TableRow {
        private       String          hash;
        private       MediaFile       mediaFile;
        private       Boolean         state;
        private final BooleanProperty stateProperty;
        private final SimpleObjectProperty<Path> pathProperty;

        public TableRow(String hash, MediaFile mediaFile) {
            this(hash, mediaFile, null);
        }

        public TableRow(String hash, MediaFile mediaFile, Boolean state) {
            this.hash = hash;
            this.mediaFile = mediaFile;
            this.state = state != null && state;
            if (mediaFile != null) {
                this.stateProperty = new SimpleBooleanProperty(this, "state");
            } else {
                this.stateProperty = null;
            }
            this.pathProperty = new SimpleObjectProperty<>(mediaFile == null ? null : mediaFile.fullPath());

        }

        public Boolean getState() {
            return stateProperty == null ? null : stateProperty.get();
        }

        public void setState(Boolean state) {
            if (stateProperty != null) {
                this.stateProperty.setValue(state);
            }
        }

        public boolean isParent() {
            return mediaFile == null;
        }
    }

    @PostConstruct
    void init() {
        FontIcon graphic = new FontIcon(MaterialDesignP.PLAY_CIRCLE_OUTLINE);
        graphic.setIconSize(36);
        graphic.getStyleClass().add("button-top-bar");
        Button run = new Button(null, graphic);
        FxUtil.styleCircleButton(run).setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                maskerPane.start();
                taskService.supply(ModernTask.<List<HashDuplicate>>builder()
                                             .execute(myself -> findDuplicates())
                                             .onSuccess((listModernTask, duplicates) -> updateTable(duplicates))
                                             // TODO: Error Mgt
                                             .build());
            }
        });

        Label               viewMode   = new Label("View mode");
        ComboBox<EViewMode> cbViewMode = new ComboBox<>(FXCollections.observableArrayList(EViewMode.values()));
        cbViewMode.getSelectionModel().select(EViewMode.DUPLICATE);
        cbViewMode.setEditable(false);
        cbViewMode.setDisable(true);
        HBox hbCollection = new HBox();
        collectionPicker.setShowRootProperty(true);
        collectionPicker.selectRoot();

        hbCollection.getChildren().addAll(new Label("Choose a collection: "), collectionPicker);
        hbCollection.setAlignment(Pos.CENTER);

        var hbButton = new HBox(viewMode, cbViewMode, hbCollection, new Label(STR."\{persistenceService.countMediaFiles()} files."), run);
        hbButton.setSpacing(15);
        hbButton.setAlignment(Pos.CENTER_LEFT);

        var hbResults = new HBox(lbNbDup);
        hbResults.setSpacing(15);
        hbResults.setAlignment(Pos.CENTER_LEFT);

        vb.setSpacing(10);
        vb.setPrefWidth(400);
        HBox.setHgrow(vb, Priority.ALWAYS);

        treeTableHash = createTreeTable();
        vb.getChildren().addAll(hbButton, hbResults, treeTableHash);
        VBox.setVgrow(treeTableHash, Priority.ALWAYS);
        StackPane.setAlignment(vb, Pos.TOP_CENTER);

        Platform.runLater(() -> {
            var underConstruction = new Image("/images/under-construction.png", 200, -1, true, true);
            var view              = new ImageView(underConstruction);
            StackPane.setAlignment(view, Pos.TOP_RIGHT);
            maskerPane.getRootContent().getChildren().add(view);
        });
        maskerPane.setContent(vb);
    }

    private List<HashDuplicate> findDuplicates() {
        if (collectionPicker.isRootSelected()) {
            return collectionManager.findDuplicateByHash();
        } else {
            return collectionManager.findDuplicateByHash(collectionPicker.getText());
        }
    }

    TreeTableView<TableRow> createTreeTable() {
        var hashCol  = new TreeTableColumn<TableRow, String>("Duplicates");
        var stateCol = new TreeTableColumn<TableRow, Boolean>("Delete ?");
        var idCol = new TreeTableColumn<TableRow, String>("ID");
        var pathCol = new TreeTableColumn<TableRow, Path>("Path");

        hashCol.setSortable(false);

        hashCol.setEditable(false);
        stateCol.setEditable(true);
        idCol.setEditable(false);
        pathCol.setEditable(false);

        hashCol.setMaxWidth(150);
        stateCol.setMaxWidth(80);
        idCol.setMaxWidth(60);

        stateCol.setCellValueFactory(param -> param.getValue().getValue().stateProperty);
        stateCol.setOnEditCommit((TreeTableColumn.CellEditEvent<TableRow, Boolean> t) -> {
            t.getTreeTableView().getTreeItem(t.getTreeTablePosition().getRow()).getValue().stateProperty.set(t.getNewValue());
        });
//        stateCol.setCellFactory(CheckBoxTreeTableCell.forTreeTableColumn(stateCol));
        stateCol.setCellFactory(_ -> new CheckBoxTreeTableCell<>(null, null) {
            @Override
            public void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item == null) {
                    setGraphic(null);
                }
            }
        });

        idCol.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getValue().isParent() ? "" : STR."\{c.getValue().getValue().mediaFile.getId()}")
        );
        hashCol.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getValue().isParent()
                                              ? STR."\{c.getValue().getChildren().size()} files (\{c.getValue().getValue().getHash()})"
                                              : "")
        );

        pathCol.setCellValueFactory(param -> param.getValue().getValue().pathProperty);
        pathCol.setCellFactory(new HyperlinkTreeCell<>());

        var treeTable = new TreeTableView<TableRow>();
        treeTable.setEditable(true);
        treeTable.getColumns().setAll(hashCol, stateCol, idCol, pathCol);
        treeTable.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TreeItem<TableRow> root = new TreeItem<>(new TableRow("root", null));
        treeTable.setRoot(root);
        treeTable.setShowRoot(false);

        return treeTable;
    }

    void updateTable(List<HashDuplicate> duplicates) {
        lbNbDup.setText(STR."'\{duplicates.size()}' duplicates found.");
        TreeItem<TableRow> root = treeTableHash.getRoot();
        root.getChildren().clear();
        duplicates.stream()
                  .flatMap(hd -> Stream.concat(Stream.of(new TreeItem<>(new TableRow(hd.hash(), null))),
                                               hd.files()
                                                 .stream()
                                                 .map(mf -> new TreeItem<>(new TableRow(hd.hash(), mf, false)))))
                  .forEach(ti -> {
                      if (ti.getValue().isParent()) {
                          root.getChildren().add(ti);
//                          if (ti.getValue().stateProperty != null)
//                            ti.getValue().stateProperty.addListener((_, _, newValue) -> ti.getChildren()
//                                                                                        .forEach(child -> child.getValue()
//                                                                                                .stateProperty.set(newValue)));
                      } else {
                          Nodes.searchTreeItemByPredicate(root, tableRow -> Objects.equals(tableRow.hash, ti.getValue().hash))
                               .ifPresent(tableRowTreeItem -> {
                                   tableRowTreeItem.getChildren().add(ti);
                                   tableRowTreeItem.setExpanded(true);
                               });
                      }
                  });
        maskerPane.stop();
    }

    List<? extends Node> createDuplicates(List<HashDuplicate> duplicates) {
        return duplicates.stream()
                         .map(dup -> {
                             var card = new Card();
                             card.setHeader(new Label(dup.hash()));

                             GridPane grid = new GridPane();
                             grid.setPadding(new Insets(10, 10, 10, 10));
                             grid.setHgap(10);
                             grid.setVgap(10);
                             grid.setAlignment(Pos.TOP_LEFT);
                             int row = 0;
                             for (MediaFile mf : dup.files()) {
                                 Label child = new Label(mf.getFullPath().toString());
                                 child.setMinWidth(300);
                                 grid.add(new Label(null, new FontIcon(MaterialDesignT.TRASH_CAN_OUTLINE)), 0, row);
                                 grid.add(child, 1, row);
                                 row++;
                             }

                             card.setBody(grid);

                             return card;
                         })
                         .toList();
    }

    @Override
    public String getName() {
        return "By hash";
    }

    @Override
    public Ikon getIcon() {
        return null;
    }

    @Override
    public Node getView() {
        return maskerPane.getRootContent();
    }

    @Override
    public NavTree.Item getGroup() {
        return RepairTool.DUPLICATE;
    }
}
