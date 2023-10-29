package org.icroco.picture.event;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@ToString
@SuperBuilder
public class NotificationEvent extends IiEvent {
    public enum NotificationType {
        INFO,
        SUCCESS,
        WARNING,
        ERROR;
    }

    private final String           message;
    private final NotificationType type;
}
