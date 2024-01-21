package org.icroco.picture.views.organize.collections;

import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import javafx.beans.binding.Bindings;
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
import org.kordamp.ikonli.materialdesign2.MaterialDesignD;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;

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
    private final Label linkOff;

    int mediaCollectionId = -1;

    private final MenuButton flatMenuBtn = new MenuButton(null, new FontIcon(MaterialDesignD.DOTS_VERTICAL));

    public CollectionTreeCell(TaskService taskService) {
        super();
        getStyleClass().add("collection-tree-cell");
        this.taskService = taskService;

        titleLabel = new Label();
        titleLabel.setGraphicTextGap(10);
        titleLabel.getStyleClass().add("title");

        arrowIcon = new FontIcon();
        arrowIcon.getStyleClass().add("arrow");

        tagLabel = new Label("");
        tagLabel.getStyleClass().add("tag");

        linkOff = new Label("", new FontIcon(MaterialDesignL.LINK_OFF));
//        linkOff.getStyleClass().add("tag");
        linkOff.getStyleClass().add(Styles.DANGER);

        flatMenuBtn.getItems().setAll(createMenuItems());
        flatMenuBtn.getStyleClass().setAll(Styles.FLAT, Tweaks.NO_ARROW, Styles.SMALL);
        flatMenuBtn.setManaged(false);
        flatMenuBtn.setVisible(false);
//        FxUtil.styleCircleFlat(flatMenuBtn);

        root = new HBox();
        root.setAlignment(Pos.CENTER_LEFT);
        Spacer spacer = new Spacer();
        root.getChildren().setAll(titleLabel, spacer, linkOff, tagLabel, flatMenuBtn);
        linkOff.setVisible(false);
        linkOff.setManaged(false);
        root.setCursor(Cursor.HAND);
        root.getStyleClass().add("container");

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
    protected void updateItem(CollectionNode item, boolean empty) {
        super.updateItem(item, empty);

        if (item == null || empty) {
            setGraphic(null);
            titleLabel.setText(null);
            titleLabel.setGraphic(null);
            linkOff.visibleProperty().unbind();
            linkOff.managedProperty().unbind();
        } else {
            setGraphic(root);

            titleLabel.setText(item.path().getFileName().toString());
            if (item.rootCollection() == null) {
                flatMenuBtn.setVisible(false);
                flatMenuBtn.setManaged(false);
                mediaCollectionId = -1;
                linkOff.visibleProperty().unbind();
                linkOff.managedProperty().unbind();
            } else {
                // TODO: Better manage resources (menu and linkOf, create on the fly
                flatMenuBtn.setManaged(true);
                flatMenuBtn.setVisible(true);
                mediaCollectionId = item.id();
                Tooltip value = new Tooltip(item.path().toString());
                titleLabel.setTooltip(value);

                linkOff.setTooltip(new Tooltip(STR."Path: '\{getItem().path()}' does not exist, or is not mounted!"));
                linkOff.visibleProperty().bind(Bindings.not(item.rootCollection().connectedProperty()));
                linkOff.managedProperty().bind(Bindings.not(item.rootCollection().connectedProperty()));
            }
        }
    }
}
