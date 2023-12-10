package org.icroco.picture.views.organize.collections;

import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.event.DeleteCollectionEvent;
import org.icroco.picture.views.task.TaskService;
import org.icroco.picture.views.util.widget.FxUtil;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2OutlinedMZ;
import org.kordamp.ikonli.materialdesign2.MaterialDesignD;

import java.util.Collection;
import java.util.List;

@Slf4j
public class CollectionTreeCell extends TreeCell<CollectionNode> {

    private static final PseudoClass GROUP = PseudoClass.getPseudoClass("group");
    private final        TaskService taskService;

    @Getter(value = AccessLevel.PACKAGE)
    private final HBox  root;
    private final Label titleLabel;
    private final Node  arrowIcon;
    private final Label tagLabel;

    int mediaCollectionId = -1;

    private final MenuButton flatMenuBtn = new MenuButton(null, new FontIcon(MaterialDesignD.DOTS_VERTICAL));

    public CollectionTreeCell(TaskService taskService) {
        super();
        this.taskService = taskService;

        titleLabel = new Label();
        titleLabel.setGraphicTextGap(10);
        titleLabel.getStyleClass().add("title");

        arrowIcon = new FontIcon();
        arrowIcon.getStyleClass().add("arrow");

        tagLabel = new Label("");
        tagLabel.getStyleClass().add("tag");

        flatMenuBtn.getItems().setAll(createMenuItems());
        flatMenuBtn.getStyleClass().setAll(Styles.FLAT, Tweaks.NO_ARROW, Styles.SMALL);
        flatMenuBtn.setManaged(false);
        flatMenuBtn.setVisible(false);
//        FxUtil.styleCircleFlat(flatMenuBtn);

        root = new HBox();
        root.setAlignment(Pos.CENTER_LEFT);
        Spacer spacer = new Spacer();
        root.getChildren().setAll(titleLabel, spacer, tagLabel, flatMenuBtn);
        root.setCursor(Cursor.HAND);
        root.getStyleClass().add("container");

//        root.addEventHandler(MouseEvent.ANY, event -> {
//            if (event.getClickCount() == 2 && event.getButton().equals(MouseButton.PRIMARY)) {
////                if (event.getEventType().equals(MouseEvent.MOUSE_CLICKED)) {
//                log.info("Clicked: {}", event);
//                taskService.sendEvent(CollectionSubPathSelectedEvent.builder()
//                                                                    .collectionId(findMainCollectionNode(newValue).getValue().id())
//                                                                    .entry(newValue.getValue().path)
//                                                                    .source(this)
//                                                                    .build());
////                }
//
//                event.consume();
//            }
//        });
//        root.setMaxWidth(ApplicationWindow.SIDEBAR_WIDTH - 10);

//        root.setOnMouseClicked(e -> {
////            if (!(getTreeItem() instanceof Item item)) {
////                return;
////            }
//
////            if (item.isGroup() && e.getButton() == MouseButton.PRIMARY) {
////                item.setExpanded(!item.isExpanded());
////                // scroll slightly above the target
////                getTreeView().scrollTo(getTreeView().getRow(item) - 10);
////            }
//        });

        getStyleClass().add("nav-tree-cell");
    }

    private Collection<MenuItem> createMenuItems() {
        var deleteMenu = new MenuItem("Remove the whole collection", new FontIcon(MaterialDesignD.DELETE_OUTLINE));
        deleteMenu.setOnAction(_ -> {
            if (mediaCollectionId >= 0) {
                taskService.sendEvent(DeleteCollectionEvent.builder().mcId(mediaCollectionId).source(this).build());
            }
        });
        FxUtil.styleCircleFlat(deleteMenu);
        deleteMenu.getStyleClass().add(Styles.SMALL);

        return List.of(deleteMenu);
    }

    @Override
    protected void updateItem(CollectionNode nav, boolean empty) {
        super.updateItem(nav, empty);

        if (nav == null || empty) {
            setGraphic(null);
            titleLabel.setText(null);
            titleLabel.setGraphic(null);
        } else {
            setGraphic(root);

            titleLabel.setText(nav.path().getFileName().toString());
            if (nav.isColTopLevel()) {
                flatMenuBtn.setManaged(true);
                flatMenuBtn.setVisible(true);
                mediaCollectionId = nav.id();
                Tooltip value = new Tooltip(nav.path().toString());
                titleLabel.setTooltip(value);
            } else {
                flatMenuBtn.setVisible(false);
                flatMenuBtn.setManaged(false);
                mediaCollectionId = -1;
            }
            if (nav.pathExist()) {
                tagLabel.setGraphic(null);
            } else {
                Label label = new Label(null, new FontIcon(Material2OutlinedMZ.WARNING));
                label.setTooltip(new Tooltip(STR."Path does not exist: '\{getItem().path()}'"));
                tagLabel.setGraphic(label);
            }
        }
    }
}
