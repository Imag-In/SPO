package org.icroco.picture.event;

import lombok.ToString;
import org.springframework.context.ApplicationEvent;

import java.time.Clock;

@ToString
public class IiEvent extends ApplicationEvent {
    public IiEvent(Object source) {
        super(source);
    }

    public IiEvent(Object source, Clock clock) {
        super(source, clock);
    }
}
