package org.icroco.picture.views.util;

import atlantafx.base.controls.RingProgressIndicator;
import atlantafx.base.util.Animations;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.util.Duration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MaskerPane<T extends Node> {
    private final RingProgressIndicator ring = new RingProgressIndicator(0, false);
    private final Pane                  progressPane;
    @Getter
    private final StackPane             rootContent;
    @Getter
    private       T                     content;
    private final boolean               wrapSp;


    public MaskerPane() {
        this(new StackPane(), false);
    }

    public MaskerPane(boolean wrapIntoScrollPane) {
        this(new StackPane(), wrapIntoScrollPane);
    }

    public MaskerPane(StackPane rootContent, boolean wrapIntoScrollPane) {
        progressPane = createMasker();
        this.rootContent = rootContent;
        this.rootContent.getChildren().add(progressPane);
        this.rootContent.setId("maskerRootPane"); // TODO: Remove / replace by class
        wrapSp = wrapIntoScrollPane;
        progressPane.setVisible(false);
    }

    public void setContent(T content) {
        this.content = content;
        rootContent.getChildren().addLast(wrapSp ? new ScrollPane(content) : content);
    }

    public void start(ReadOnlyDoubleProperty doubleProperty) {
        ring.progressProperty().unbind();
        ring.progressProperty().bind(doubleProperty);
        start();
    }

    public void start() {
        progressPane.setVisible(true);
//        progressPane.setOpacity(.5);
        content.setOpacity(0.5);
        if (!ring.progressProperty().isBound()) {
            ring.setProgress(-1D);
        }
    }

    public void stop() {
        ring.progressProperty().unbind();
        progressPane.setVisible(false);
//        ring.setProgress(1);
        var t = Animations.fadeIn(content, Duration.millis(1000));
        t.playFromStart();
        t.setOnFinished(_ -> ring.setProgress(0));
    }

    private Pane createMasker() {
        VBox vBox = new VBox();
        vBox.setAlignment(Pos.CENTER);
        vBox.setSpacing(10.0);
        VBox.setVgrow(vBox, Priority.ALWAYS);
//        vBox.getStyleClass().add("masker-center"); //$NON-NLS-1$

        vBox.getChildren().add(createLabel());
        vBox.getChildren().add(createProgressIndicator());

        HBox hBox = new HBox();
        HBox.setHgrow(hBox, Priority.ALWAYS);
        hBox.setAlignment(Pos.CENTER);
        hBox.getChildren().addAll(vBox);

        StackPane glass = new StackPane();
        glass.setAlignment(Pos.CENTER);
//        glass.getStyleClass().add("masker-glass"); //$NON-NLS-1$
        glass.getChildren().add(hBox);

        glass.setFocusTraversable(false);
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
