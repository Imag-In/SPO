package org.icroco.picture.ui.util.widget;

import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import lombok.experimental.UtilityClass;

@UtilityClass
public class FxUtil {
    public static Button styleCircleButton(Button button) {
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.getStyleClass().addAll(atlantafx.base.theme.Styles.BUTTON_CIRCLE, atlantafx.base.theme.Styles.FLAT);

        return button;
    }
}
