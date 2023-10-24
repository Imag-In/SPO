package org.icroco.picture.event;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.nio.file.Path;

@Getter
@ToString
@SuperBuilder
public class UsbStorageDeviceEvent extends IiEvent {
    public enum EventType {
        REMOVED,
        CONNECTED;
    }

    private final Path      rootDirectory;
    private final String    deviceName;
    private final EventType type;
}
