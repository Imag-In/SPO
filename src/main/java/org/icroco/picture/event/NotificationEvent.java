package org.icroco.picture.event;

import lombok.Builder;
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
    @Builder.Default
    private final int              timeoutInSeconds = 5;
}
