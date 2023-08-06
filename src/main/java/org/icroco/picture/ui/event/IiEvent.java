package org.icroco.picture.ui.event;

import org.springframework.context.ApplicationEvent;

import java.time.Clock;

public class IiEvent extends ApplicationEvent {
    public IiEvent(Object source) {
        super(source);
    }

    public IiEvent(Object source, Clock clock) {
        super(source, clock);
    }
}
