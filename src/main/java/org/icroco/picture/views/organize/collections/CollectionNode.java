package org.icroco.picture.views.organize.collections;

import java.nio.file.Path;

public record CollectionNode(Path path, int id, boolean isColTopLevel, boolean pathExist) {
    public CollectionNode(Path path, int id) {
        this(path, id, false, true);
    }

    boolean isRootCollection() {
        return id >= 0;
    }

    public CollectionNode withPathExist(boolean exist) {
        return new CollectionNode(this.path, this.id, this.isColTopLevel, exist);
    }
}
