package org.icroco.picture.views.repair;

import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class SideBar extends VBox {
    private final NavTree navTree;

    public SideBar(RepairModel model) {
        this.navTree = new NavTree(model);
        createView();
    }

    private void createView() {
        var header = createLogo();

        VBox.setVgrow(navTree, Priority.ALWAYS);

        setId("sidebar");
        setPadding(new Insets(10, 0, 10, 0));
        getChildren().addAll(header, new Spacer(Orientation.HORIZONTAL), navTree);
    }

    private HBox createLogo() {
        HBox hb = new HBox();

        Label analysisAndRepair = new Label("Analysis and Repair");
        analysisAndRepair.getStyleClass().add(Styles.TITLE_3);
        hb.getChildren().add(analysisAndRepair);

        return hb;

    }
}
