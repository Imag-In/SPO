package org.icroco.picture.views.util;

import javafx.event.EventType;
import javafx.scene.input.MouseEvent;

public interface CustomMouseEvent {
    EventType<MouseEvent> MOUSE_DOUBLE_CLICKED = new EventType<>(MouseEvent.ANY, "MOUSE_DBL_CLICKED");
}
