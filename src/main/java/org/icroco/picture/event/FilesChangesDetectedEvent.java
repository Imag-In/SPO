package org.icroco.picture.event;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.nio.file.Path;
import java.util.Collection;

@Getter
@ToString(onlyExplicitlyIncluded = true)
@SuperBuilder
public final class FilesChangesDetectedEvent extends IiEvent {
    private final Collection<Path> created;
    private final Collection<Path> deleted;
    private final Collection<Path> modified;

    public boolean isEmpty() {
        return created.isEmpty() && deleted.isEmpty() && modified.isEmpty();
    }

    public boolean isNotEmpty() {
        return !isEmpty();
    }
}
