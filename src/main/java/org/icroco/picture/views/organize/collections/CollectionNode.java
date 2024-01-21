package org.icroco.picture.views.organize.collections;

import org.icroco.picture.model.MediaCollection;

import java.nio.file.Path;

public record CollectionNode(Path path, int id, MediaCollection rootCollection) {
    public CollectionNode(Path path, int id) {
        this(path, id, null);
    }

//    public CollectionNode withPathExist(boolean exist) {
//        return new CollectionNode(this.path, this.id, this.isColTopLevel, exist);
//    }
}
