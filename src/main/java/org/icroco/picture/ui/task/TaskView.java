package org.icroco.picture.ui.task;

import jakarta.annotation.PostConstruct;
import javafx.concurrent.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.TaskProgressView;
import org.springframework.stereotype.Component;


@Slf4j
@RequiredArgsConstructor
@Component
public class TaskView extends TaskProgressView<Task<?>> {
    public static final String TASK = "task";

    @PostConstruct
    void postConstruct() {
        setPrefWidth(400D);
        setPrefHeight(600D);
    }

    public <T> void addTask(Task<T> task) {
        getTasks().add(task);
    }
}
