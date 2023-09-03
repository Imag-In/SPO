package org.icroco.picture.ui.task;

import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.TaskProgressView;
import org.icroco.javafx.FxInitOnce;


@Slf4j
//@FxViewBinding(id = TaskController.TASK, fxmlLocation = "task.fxml")
@RequiredArgsConstructor
@Deprecated
public class TaskController extends FxInitOnce {
    public static final String TASK = "task";

    @FXML
    private TaskProgressView<Task<?>> tasks;

    public ObservableList<Task<?>> getTasks() {
        return tasks.getTasks();
    }

    @Override
    protected void initializedOnce() {
    }

    public <T> void addTask(Task<T> task) {
        tasks.getTasks().add(task);
    }
}
