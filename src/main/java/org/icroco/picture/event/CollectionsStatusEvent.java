package org.icroco.picture.event;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Getter
@ToString
@SuperBuilder
public class CollectionsStatusEvent extends IiEvent {
    private final Map<Integer, Boolean> statuses;
}
