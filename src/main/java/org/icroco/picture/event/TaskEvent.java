package org.icroco.picture.event;

import javafx.concurrent.Task;
import lombok.Getter;
import lombok.ToString;

import java.time.Clock;

@Getter
@ToString
public class TaskEvent extends IiEvent {

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
