package org.icroco.picture.ui.navigation;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.SegmentedButton;
import org.icroco.javafx.FxInitOnce;
import org.icroco.javafx.FxViewBinding;


@Slf4j
@FxViewBinding(id = "navigation", fxmlLocation = "nav.fxml")
@RequiredArgsConstructor
public class NavigationController extends FxInitOnce {

    @FXML
    HBox container;

    @FXML
    SegmentedButton btnBar;

    @Override
    protected void initializedOnce() {
        var toggleGroup = new ToggleGroup();

        var importBtn = new ToggleButton("Import");
        var selectBtn = new ToggleButton("Select");
        var editBtn = new ToggleButton("Edit");
        var exportBtn = new ToggleButton("Export");

        importBtn.setDisable(true);
        editBtn.setDisable(true);
        exportBtn.setDisable(true);

        btnBar.getButtons().addAll(importBtn, selectBtn, editBtn, exportBtn);
        btnBar.setToggleGroup(toggleGroup);
        HBox.setHgrow(btnBar, Priority.ALWAYS);
        container.setAlignment(Pos.CENTER);

    }

}
