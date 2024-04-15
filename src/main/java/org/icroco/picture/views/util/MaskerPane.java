package org.icroco.picture.views.util;

import atlantafx.base.controls.RingProgressIndicator;
import atlantafx.base.util.Animations;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.views.task.ModernTask;
import org.icroco.picture.views.task.TaskService;

import java.util.function.Supplier;

@Slf4j
public class MaskerPane<T extends Node> extends StackPane {
    private final RingProgressIndicator ring = new RingProgressIndicator(0, false);
    private final Pane                  progressPane;
    //    @Getter
//    private final StackPane             rootContent;
//    @Getter
    private       T                     content;
    private final boolean               wrapSp;


//    public MaskerPane() {
//        this(new StackPane(), false);
//    }

//    public MaskerPane(boolean wrapIntoScrollPane) {
//        this(new StackPane(), wrapIntoScrollPane);
//    }

    public MaskerPane(T content) {
        this(content, false);
    }

    @Deprecated
    public MaskerPane(T content, boolean wrapIntoScrollPane) {
        this.setId("maskerRootPane"); // TODO: Remove / replace by class
        this.content = content;
        this.setAlignment(Pos.CENTER);
//        glass.getStyleClass().add("masker-glass"); //$NON-NLS-1$
        progressPane = createMasker();
        progressPane.setFocusTraversable(false);
        this.getChildren().addAll(progressPane, wrapIntoScrollPane ? new ScrollPane(content) : content);
        wrapSp = wrapIntoScrollPane;
        progressPane.setVisible(false);
    }

//    public void setContent(T content) {
//        this.content = content;
//        rootContent.getChildren().addLast(wrapSp ? new ScrollPane(content) : content);
//    }

    public <R> Task<R> execute(TaskService service, Supplier<R> task) {
        start();
        var uiTask = ModernTask.<R>builder()
                               .execute(_ -> task.get())
                               .onFinished(this::stop)
                               .build();
        service.supply(uiTask, false);
        return uiTask;
    }

    @Deprecated
    public void start(ReadOnlyDoubleProperty doubleProperty) {
        ring.progressProperty().unbind();
        ring.progressProperty().bind(doubleProperty);
        start();
    }

    @Deprecated
    public void start() {
        progressPane.setVisible(true);
//        progressPane.setOpacity(.5);
        content.setOpacity(0.4);
        if (!ring.progressProperty().isBound()) {
            ring.setProgress(-1D);
        }
    }

    @Deprecated
    public void stop() {
        ring.progressProperty().unbind();
        progressPane.setVisible(false);
//        ring.setProgress(1);
        var t = Animations.fadeIn(content, Duration.millis(500));
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

        return hBox;
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
