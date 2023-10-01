package org.icroco.picture.event;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@ToString
@SuperBuilder
public class GalleryRefreshEvent extends IiEvent {
    private final int mediaCollectionId;
}
