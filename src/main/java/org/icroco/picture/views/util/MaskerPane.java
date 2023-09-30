package org.icroco.picture.views.util;

import atlantafx.base.controls.RingProgressIndicator;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MaskerPane extends StackPane {
    RingProgressIndicator ring = new RingProgressIndicator(0, false);
    HBox                  hBox = new HBox();

    public MaskerPane() {
        getChildren().add(createMasker());
    }

    public void start(ReadOnlyDoubleProperty doubleProperty) {
        ring.progressProperty().unbind();
        ring.setProgress(-1D);
        ring.progressProperty().bind(doubleProperty);
        hBox.setVisible(true);
    }

    public void start() {
        ring.setProgress(-1D);
        hBox.setVisible(true);
    }

    public DoubleProperty getProgressProperty() {
        return ring.progressProperty();
    }

    public void stop() {
        hBox.setVisible(false);
        ring.progressProperty().unbind();
        ring.setProgress(0D);
    }

    private Node createMasker() {
        VBox vBox = new VBox();
        vBox.setAlignment(Pos.CENTER);
        vBox.setSpacing(10.0);
        VBox.setVgrow(vBox, Priority.ALWAYS);
//        vBox.getStyleClass().add("masker-center"); //$NON-NLS-1$

        vBox.getChildren().add(createLabel());
        vBox.getChildren().add(createProgressIndicator());

        HBox.setHgrow(hBox, Priority.ALWAYS);
        hBox.setAlignment(Pos.CENTER);
        hBox.getChildren().addAll(vBox);

        StackPane glass = new StackPane();
        glass.setAlignment(Pos.CENTER);
//        glass.getStyleClass().add("masker-glass"); //$NON-NLS-1$
        glass.getChildren().add(hBox);

        return glass;
    }

    private Label createLabel() {
        Label text = new Label("Please Wait");
//        text.textProperty().bind(getSkinnable().textProperty());
//        text.getStyleClass().add("masker-text"); //$NON-NLS-1$
        return text;
    }

    private Node createProgressIndicator() {

        ring.setPrefSize(75, 75);
//        ring.progressProperty().bind(Bindings.createDoubleBinding(
//                () -> ringToggle.isSelected() ? -1d : 0d,
//                ringToggle.selectedProperty()
//        ));
        return ring;

    }
}
