package org.icroco.picture.views.demo;

import javafx.application.HostServices;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icroco.javafx.FxViewBinding;
import org.icroco.javafx.I18N;

@FxViewBinding(id = "blabla", fxmlLocation = "/fxml/ui.fxml", isPrimary = false)
@RequiredArgsConstructor
@Slf4j
public class SimpleUiController {

    private final HostServices hostServices;
    private final I18N         i18N;

    @FXML
    public Label label;

    @FXML
    public Button button;

//    @Override
//    public void initialize(URL location, ResourceBundle resources) {
//        log.info("initialize {}, location: {}, resources: {}", this, location, resources);
//        this.button.setOnAction(actionEvent -> this.label.setText(this.hostServices.getDocumentBase()));
//    }

    @FXML
    public void initialize() {
//        log.info("initialize: {}", this);
        PseudoClass imageViewBorder = PseudoClass.getPseudoClass("border");
        i18N.bind(button, "button.hello");
        i18N.bind(label, "label.hello");
        this.button.setOnAction(actionEvent -> this.label.textProperty().setValue(this.hostServices.getDocumentBase()));
    }

}
