package org.icroco.picture.ui.util;

import javafx.scene.Node;

import java.util.HashSet;
import java.util.Set;

@Deprecated
public class SelectionModel<T extends Node> {
    private Set<T> selection = new HashSet<>();

    public boolean add(T node) {
//        node.getStyleClass().add("image-grid-cell");
        node.setStyle("-fx-effect: dropshadow(three-pass-box, white, 2,2,0,0);");
        return selection.add(node);
    }

    public boolean remove(T node) {
//        node.getStyleClass().remove("image-grid-cell");
        node.setStyle("-fx-effect: null");
        return selection.remove(node);
    }

    public boolean contains(T node) {
        return selection.contains(node);
    }

    public void clear() {
        selection.forEach(n -> n.setStyle("-fx-effect: null"));
        selection.clear();
    }

    public Set<T> getSelection() {
        return Set.copyOf(selection);
    }

}
