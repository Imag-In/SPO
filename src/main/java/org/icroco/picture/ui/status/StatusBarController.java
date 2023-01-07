package org.icroco.picture.ui.status;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.StatusBar;
import org.icroco.javafx.FxInitOnce;
import org.icroco.javafx.FxViewBinding;

import java.util.ArrayList;


@Slf4j
@FxViewBinding(id = "status_bar", fxmlLocation = "status.fxml")
@RequiredArgsConstructor
public class StatusBarController extends FxInitOnce {
    @FXML
    @Getter
    StatusBar container;

    @Override
    protected void initializedOnce() {
        container.textProperty().set("");
        container.setSkin(new CustomStatusBarSkin(container));
    }
}
