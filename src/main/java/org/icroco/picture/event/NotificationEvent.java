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
        QUESTION,
        INFO,
        SUCCESS,
        WARNING,
        ERROR;
    }

    private String           message;
    @Builder.Default
    private NotificationType type             = NotificationType.INFO;
    @Builder.Default
    private int              timeoutInSeconds = 5;
}
