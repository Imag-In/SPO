package org.icroco.picture.views.organize.collections;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.icroco.picture.model.MediaCollection;
import org.icroco.picture.model.MediaCollectionEntry;

import java.nio.file.Path;

public record CollectionNode(Path path, int mcId, MediaCollection rootCollection, MediaCollectionEntry mce) {
    public CollectionNode(Path path, int id) {
        this(path, id, null, null);
    }

    public ReadOnlyBooleanProperty rootConnectedProperty() {
        return rootCollection == null ? new SimpleBooleanProperty(true) : rootCollection.connectedProperty();
    }

//    public CollectionNode withPathExist(boolean exist) {
//        return new CollectionNode(this.path, this.mcId, this.isColTopLevel, exist);
//    }
}
