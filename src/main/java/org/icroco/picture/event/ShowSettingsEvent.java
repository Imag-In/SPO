package org.icroco.picture.event;

import javafx.scene.Scene;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@ToString
@SuperBuilder
public class ShowSettingsEvent extends IiEvent {
    private final Scene scene;
}
