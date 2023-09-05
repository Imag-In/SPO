package org.icroco.picture.ui.task;

import jakarta.annotation.PostConstruct;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.TaskProgressView;
import org.icroco.picture.ui.FxView;
import org.springframework.stereotype.Component;


@Slf4j
@RequiredArgsConstructor
@Component
public class TaskView implements FxView<TaskProgressView<Task<?>>> {
    public static final String TASK = "task";

    private final TaskProgressView<Task<?>> root = new TaskProgressView<Task<?>>();

    @PostConstruct
    void postConstruct() {
        root.setPrefWidth(400D);
        root.setPrefHeight(600D);
    }

    public <T> void addTask(Task<T> task) {
        root.getTasks().add(task);
    }

    public ObservableList<Task<?>> getTasks() {
        return root.getTasks();
    }

    @Override
    public TaskProgressView<Task<?>> getRootContent() {
        return root;
    }
}
