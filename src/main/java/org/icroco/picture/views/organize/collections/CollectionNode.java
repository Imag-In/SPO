package org.icroco.picture.views.organize.collections;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.icroco.picture.model.MediaCollection;

import java.nio.file.Path;

public record CollectionNode(Path path, int id, MediaCollection rootCollection) {
    public CollectionNode(Path path, int id) {
        this(path, id, null);
    }

    public ReadOnlyBooleanProperty rootConnectedProperty() {
        return rootCollection == null ? new SimpleBooleanProperty(true) : rootCollection.connectedProperty();
    }

//    public CollectionNode withPathExist(boolean exist) {
//        return new CollectionNode(this.path, this.id, this.isColTopLevel, exist);
//    }
}
