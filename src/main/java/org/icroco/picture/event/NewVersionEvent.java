package org.icroco.picture.event;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.net.URI;

@Getter
@ToString
@SuperBuilder
public class NewVersionEvent extends IiEvent {
    private final String version;
    private final URI    url;
}
