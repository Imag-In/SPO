package org.icroco.picture.util;

import javafx.stage.Stage;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class StageReadyEvent extends ApplicationEvent {
    private final Stage stage;

    public StageReadyEvent(Stage stage) {
        super(stage);
        this.stage = stage;
    }
}
