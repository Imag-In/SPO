package org.icroco.picture.ui.util.widget;

import javafx.css.Styleable;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import lombok.experimental.UtilityClass;

@UtilityClass
public class FxUtil {
    public static Button styleCircleButton(Button button) {
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        return styleCircleFlat(button);
    }

    public static <T extends Styleable> T styleCircleFlat(T node) {
        node.getStyleClass().addAll(atlantafx.base.theme.Styles.BUTTON_CIRCLE, atlantafx.base.theme.Styles.FLAT);
        return node;
    }
}
