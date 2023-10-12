package org.icroco.picture.views;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.icroco.picture.views.util.FxView;

public abstract class AbstractView<T extends Node> implements FxView<T> {

    protected static Label createLabel() {
        return createLabel(150, 200);
    }

    protected static Label createLabel(String text, int minWidth, int prefWidth) {
        Label l = new Label(text);
        if (minWidth > 0) {
            l.setMinWidth(minWidth);
        }
        if (prefWidth > 0) {
            l.setPrefWidth(prefWidth);
        }
        l.setWrapText(true);

        return l;
    }

    protected static TextField createTextField(String text, int minWidth, int prefWidth) {
        TextField l = new TextField(text);
        l.setEditable(false);
        if (minWidth > 0) {
            l.setMinWidth(minWidth);
        }
        if (prefWidth > 0) {
            l.setPrefWidth(prefWidth);
        }

        return l;
    }

    protected static Label createLabel(int minWidth, int prefWidth) {
        return createLabel(null, minWidth, prefWidth);
    }
}
