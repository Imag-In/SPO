package org.icroco.picture.event;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class DeleteCollectionEvent extends IiEvent {
    private final int mcId;

    public DeleteCollectionEvent(int mcId, Object source) {
        super(source);
        this.mcId = mcId;
    }
}
