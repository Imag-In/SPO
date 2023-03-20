package org.icroco.picture.ui.gallery;

import lombok.RequiredArgsConstructor;
import org.icroco.javafx.FxInitOnce;
import org.icroco.javafx.FxViewBinding;

@FxViewBinding(id = "carousel", fxmlLocation = "carousel.fxml")
@RequiredArgsConstructor
public class CarouselController extends FxInitOnce {

    @Override
    protected void initializedOnce() {
    }
}
