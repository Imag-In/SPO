package org.icroco.picture.views.organize.collections;

import javafx.scene.Node;
import javafx.scene.control.TreeItem;

public class CollectionTreeItem extends TreeItem<CollectionView.CollectionNode> {
    final CollectionView.CollectionNode node;

    public CollectionTreeItem(CollectionView.CollectionNode node) {
        this.node = node;
    }

    public CollectionTreeItem(CollectionView.CollectionNode value, CollectionView.CollectionNode node) {
        super(value);
        this.node = node;
    }

    public CollectionTreeItem(CollectionView.CollectionNode value, Node graphic, CollectionView.CollectionNode node) {
        super(value, graphic);
        this.node = node;
    }

    boolean isRootCollection() {
        return node.id() >= 0;
    }
}
