package org.icroco.picture.views.util;

import atlantafx.base.theme.Styles;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import org.icroco.picture.model.ERating;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2OutlinedMZ;

public class Rating extends HBox {
    private final SimpleIntegerProperty value    = new SimpleIntegerProperty(0);
    private final SimpleIntegerProperty iconSize = new SimpleIntegerProperty(32);

    record Star(FontIcon selected, FontIcon unselected) {
        public Star() {
            this(new FontIcon(Material2OutlinedMZ.STAR), new FontIcon(Material2OutlinedMZ.STAR_BORDER));
        }
    }

    public Rating() {
        this(5);
    }

    public Rating(int count) {
        super();
        if (count == 0 || count > 10) {
            throw new IllegalArgumentException(STR."Stasrt count must bin in range: [1-10], valie id: '\{count}'");
        }

        getStyleClass().add("rating");

        for (int i = 0; i < count; i++) {
            Star star = new Star();
            star.selected.iconSizeProperty().bind(iconSize);
            star.unselected.iconSizeProperty().bind(iconSize);
            star.selected.setIconColor(Color.ORANGE);
            star.selected.getStyleClass().add(Styles.DANGER);
            var label = new Label(null, star.unselected);
            label.setCursor(Cursor.HAND);
            label.setOnMouseClicked(createEventHnadler(i + 1, value));
            label.graphicProperty().bind(Bindings.when(Bindings.greaterThanOrEqual(value, i + 1))
                                                 .then(star.selected)
                                                 .otherwise(star.unselected));
            getChildren().add(label);
        }
    }

    static private EventHandler<? super MouseEvent> createEventHnadler(int startIdx, SimpleIntegerProperty property) {
        return (_) -> property.setValue(property.getValue() == startIdx ? startIdx - 1 : startIdx);
    }

    public void setValue(ERating rating) {
        value.setValue(rating.getCode());
    }

    public ERating getValue() {
        return ERating.fromCode(value.get());
    }

    public ReadOnlyIntegerProperty getIconSizeProperty() {
        return iconSize;
    }

    public void setIconSize(int size) {
        iconSize.setValue(24);
    }
}
