package org.icroco.picture.ui.event;

import javafx.concurrent.Task;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Clock;

@Getter

public class TaskEvent extends ApplicationEvent {

    private final Task<?> task;

    public TaskEvent(final Task<?> task, Object source) {
        super(source);
        this.task = task;
    }

    public TaskEvent(final Task<?> task, Object source, Clock clock) {
        super(source, clock);
        this.task = task;
    }
}
