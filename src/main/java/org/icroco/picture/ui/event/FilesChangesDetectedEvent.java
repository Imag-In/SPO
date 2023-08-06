package org.icroco.picture.ui.event;

import lombok.Getter;

import java.nio.file.Path;
import java.util.Collection;

@Getter
public class FilesChangesDetectedEvent extends IiEvent {
    private final Collection<Path> created;
    private final Collection<Path> deleted;
    private final Collection<Path> modified;

    public FilesChangesDetectedEvent(Collection<Path> created, Collection<Path> deleted, Collection<Path> modified, Object source) {
        super(source);
        this.created = created;
        this.deleted = deleted;
        this.modified = modified;
    }
}
