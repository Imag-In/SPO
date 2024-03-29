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
    private final Collection<PathItem> created;
    private final Collection<PathItem> deleted;
    private final Collection<PathItem> modified;

    public record PathItem(Path path, boolean isDirectory) {
        public PathItem(Path path) {
            this(path, false);
        }

        public boolean isRegularFile() {
            return !isDirectory;
        }
    }

    public boolean isEmpty() {
        return created.isEmpty() && deleted.isEmpty() && modified.isEmpty();
    }

    public boolean isNotEmpty() {
        return !isEmpty();
    }
}
