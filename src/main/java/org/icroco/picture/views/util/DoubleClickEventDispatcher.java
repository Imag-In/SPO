package org.icroco.picture.views.util;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.*;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor
@Slf4j
public class DoubleClickEventDispatcher implements EventDispatcher {

    /**
     * Default delay to fire a double click event in milliseconds.
     */
    private static final long DEFAULT_DOUBLE_CLICK_DELAY = 250;

    /**
     * Default event dispatcher of a node.
     */

    /**
     * Timeline for dispatching mouse clicked event.
     */
    private Timeline clickedTimeline;

    @Override
    public Event dispatchEvent(final Event event, final EventDispatchChain tail) {
        final EventType<? extends Event> type = event.getEventType();
        if (type == MouseEvent.MOUSE_CLICKED) {
            final MouseEvent  mouseEvent  = (MouseEvent) event;
            final EventTarget eventTarget = event.getTarget();
            if (mouseEvent.getClickCount() == 2) {
                if (clickedTimeline != null) {
                    clickedTimeline.stop();
                    clickedTimeline = null;
                    final MouseEvent dblClickedEvent = copy(mouseEvent, CustomMouseEvent.MOUSE_DOUBLE_CLICKED);
                    Event.fireEvent(eventTarget, dblClickedEvent);
                } else {
                    final MouseEvent clickedEvent = copy(mouseEvent, mouseEvent.getEventType());
                    clickedTimeline = new Timeline(new KeyFrame(Duration.millis(DEFAULT_DOUBLE_CLICK_DELAY), e -> {
                        Event.fireEvent(eventTarget, clickedEvent);
                        clickedTimeline = null;
                    }));
                    clickedTimeline.play();
                }
                mouseEvent.consume();
                return mouseEvent;
            }
        }
        return tail.dispatchEvent(event);
    }

    /**
     * Creates a copy of the provided mouse event type with the mouse event.
     *
     * @param e         MouseEvent
     * @param eventType Event type that need to be created
     * @return New mouse event instance
     */
    private MouseEvent copy(final MouseEvent e, final EventType<? extends MouseEvent> eventType) {
        return new MouseEvent(eventType, e.getSceneX(), e.getSceneY(), e.getScreenX(), e.getScreenY(),
                              e.getButton(), e.getClickCount(), e.isShiftDown(), e.isControlDown(), e.isAltDown(),
                              e.isMetaDown(), e.isPrimaryButtonDown(), e.isMiddleButtonDown(),
                              e.isSecondaryButtonDown(), e.isSynthesized(), e.isPopupTrigger(),
                              e.isStillSincePress(), e.getPickResult());
    }
}
