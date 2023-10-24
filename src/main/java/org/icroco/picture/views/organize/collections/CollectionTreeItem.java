package org.icroco.picture.views.organize.collections;

import javafx.scene.Node;
import javafx.scene.control.TreeItem;

public class CollectionTreeItem extends TreeItem<CollectionNode> {
    final CollectionNode node;

    public CollectionTreeItem(CollectionNode node) {
        this.node = node;
    }

    public CollectionTreeItem(CollectionNode value, CollectionNode node) {
        super(value);
        this.node = node;
    }

    public CollectionTreeItem(CollectionNode value, Node graphic, CollectionNode node) {
        super(value, graphic);
        this.node = node;
    }

    boolean isRootCollection() {
        return node.id() >= 0;
    }
}
