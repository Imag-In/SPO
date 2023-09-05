package org.icroco.picture.ui;

import javafx.scene.Node;

public interface FxView<T extends Node> {
    T getRootContent();
}
