package org.icroco.picture.util;

import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class SceneReadyEvent extends ApplicationEvent {
    private final Scene scene;
    private final Stage stage;

    public SceneReadyEvent(Scene scene, Stage stage) {
        super(scene);
        this.scene = scene;
        this.stage = stage;
    }
}
