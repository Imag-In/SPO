package org.icroco.picture.views.organize.collections;

import java.nio.file.Path;

public record CollectionNode(Path path, int id, boolean isColTopLevel) {
    public CollectionNode(Path path, int id) {
        this(path, id, false);
    }

    boolean isRootCollection() {
        return id >= 0;
    }
}
