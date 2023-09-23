package org.icroco.picture.views.util;

import javafx.scene.Node;

public interface FxView<T extends Node> {
    T getRootContent();
}
