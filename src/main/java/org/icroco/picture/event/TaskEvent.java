package org.icroco.picture.event;

import javafx.concurrent.Task;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@ToString
@SuperBuilder
public class TaskEvent extends IiEvent {
    private final Task<?> task;
}
