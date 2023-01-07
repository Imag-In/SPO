package org.icroco.picture.ui.task;

import javafx.application.Platform;
import javafx.concurrent.Task;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.ui.event.TaskEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
@AllArgsConstructor
@Slf4j
public class TaskService {
    private final TaskController taskController;
    private final TaskExecutor executor;

    private <T> CompletableFuture<T> supply(final Task<T> task) {
        log.debug("Start new task: {}", task);
        taskController.addTask(task);
        return CompletableFuture.supplyAsync(() -> {
            task.run();
            try {
                return task.get();
            }
            catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Unexpected error while executing this task", e);
            }
        });
    }

    /**
     * Already notified into the Fx Application Thread.
     */
    @EventListener
    public void onEvent(TaskEvent event) {
        supply(event.getTask());
    }
}
