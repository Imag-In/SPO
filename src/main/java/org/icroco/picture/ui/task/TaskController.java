package org.icroco.picture.ui.task;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.TaskProgressView;
import org.icroco.javafx.FxInitOnce;
import org.icroco.javafx.FxViewBinding;
import org.icroco.picture.ui.status.StatusBarController;


@Slf4j
@FxViewBinding(id = "task", fxmlLocation = "task.fxml")
@RequiredArgsConstructor
public class TaskController extends FxInitOnce {
    private final StatusBarController statusBarController;

    @FXML
    private TaskProgressView<Task<?>> tasks;

    @Override
    protected void initializedOnce() {
        SimpleListProperty<Task<?>> list = new SimpleListProperty<>(tasks.getTasks());
        statusBarController.getContainer()
                           .progressProperty()
                           .bind(Bindings.valueAt(list, 0).flatMap(Task::progressProperty));

        tasks.getTasks().addListener(getTaskListChangeListener());
    }

    private ListChangeListener<Task<?>> getTaskListChangeListener() {
        return c -> {
            c.next();
            final var textProperty = statusBarController.getContainer().textProperty();
            if (c.getList().isEmpty()) {
                textProperty.unbind();
                textProperty.set("");
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

    public <T> void addTask(Task<T> task) {
        tasks.getTasks().add(task);
    }
}
