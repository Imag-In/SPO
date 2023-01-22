package org.icroco.picture.ui.status;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ProgressBar;
import javafx.util.Duration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.StatusBar;
import org.icroco.javafx.FxInitOnce;
import org.icroco.javafx.FxViewBinding;
import org.icroco.picture.ui.task.TaskView;
import org.icroco.picture.ui.util.Nodes;


@Slf4j
@FxViewBinding(id = "status_bar", fxmlLocation = "status.fxml")
@RequiredArgsConstructor
public class StatusBarController extends FxInitOnce {
    private final TaskView taskController;

    @FXML
    @Getter
    private StatusBar container;

    private PopOver popOver;


    @Override
    protected void initializedOnce() {
        container.textProperty().set("");
        container.setSkin(new CustomStatusBarSkin(container));
        SimpleListProperty<Task<?>> list = new SimpleListProperty<>(taskController.controller().getTasks());
        container.progressProperty()
                 .bind(Bindings.valueAt(list, 0).flatMap(Task::progressProperty));
        taskController.controller().getTasks().addListener(getTaskListChangeListener());
        initPopOver(taskController.scene().getRoot());
    }

    private ListChangeListener<Task<?>> getTaskListChangeListener() {
        return c -> {
            c.next();
            final var textProperty = container.textProperty();
            if (c.getList().isEmpty()) {
                textProperty.unbind();
                textProperty.set("");
                popOver.hide();
            } else if (c.getList().size() == 1) {
                textProperty.unbind();
                textProperty.bind(c.getList().get(0).titleProperty());
            } else if (c.getList().size() == 2) {
                if (c.wasAdded()) {
                    textProperty.unbind();
                    textProperty.bind(Bindings.size(c.getList()).map(number -> number + " tasks left ..."));
                }
            }
        };
    }


    public void initPopOver(Node node) {
        popOver = createPopOver(node);
        Nodes.getFirstChild(((CustomStatusBarSkin) container.getSkin()).getChildren().get(0), ProgressBar.class)
             .ifPresent(p -> p.setOnMouseClicked(event -> {
                 if (popOver.isShowing()) {
                     popOver.hide(Duration.ZERO);
                 } else if (event.getClickCount() >= 1) {
                     var targetX = event.getScreenX();
                     var targetY = event.getScreenY();
                     popOver.show(container, targetX, targetY);
                 }
             }));
    }

    private PopOver createPopOver(Node node) {
        PopOver popOver = new PopOver(node);
        popOver.setDetachable(false);
        popOver.setMinSize(600, 400);
        popOver.setDetached(false);
        popOver.setTitle("Tasks");
        popOver.setHeaderAlwaysVisible(true);
        popOver.setAutoHide(true);
        popOver.setArrowLocation(PopOver.ArrowLocation.BOTTOM_RIGHT);
//        popOver.arrowSizeProperty().bind(masterArrowSize);
//        popOver.arrowIndentProperty().bind(masterArrowIndent);
//        popOver.arrowLocationProperty().bind(masterArrowLocation);
//        popOver.cornerRadiusProperty().bind(masterCornerRadius);
//        popOver.headerAlwaysVisibleProperty().bind(masterHeaderAlwaysVisible);
        popOver.setAnimated(true);
        popOver.setCloseButtonEnabled(true);
//        popOver.closeButtonEnabledProperty().bind(closeButtonEnabled.selectedProperty());
        return popOver;
    }
}
