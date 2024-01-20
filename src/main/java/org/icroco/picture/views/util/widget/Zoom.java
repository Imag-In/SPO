package org.icroco.picture.views.util.widget;

import javafx.scene.input.GestureEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;
import lombok.Getter;
import org.icroco.picture.util.Constant;

public enum Zoom {

    /**
     * If Shift was pressed while
     * Scrolling, the action
     * will be ignored
     */
    ZOOM_NONE(0, Double.NaN),
    /**
     * = Mouse Scroll Up
     */
    ZOOM_IN(1, Constant.ZOOM_IN_SCALE),
    /**
     * = Mouse Scroll Down
     */
    ZOOM_OUT(-1, 1.0d / Constant.ZOOM_IN_SCALE);

    /**
     * 0, 1 or -1
     */
    @Getter
    private final int    zoomLevelDelta;
    /**
     * Zoom-Factor
     */
    @Getter
    private final double scale;


    Zoom(final int zoomLevelDelta, final double scale) {
        this.zoomLevelDelta = zoomLevelDelta;
        this.scale = scale;
    }

    public static Zoom of(final GestureEvent event) {
        double scrollAmount = 0;

        if (event instanceof ScrollEvent se) {
            scrollAmount = se.getDeltaY();
        } else if (event instanceof ZoomEvent ze) {
            scrollAmount = ze.getZoomFactor() - 1;
        }

        if (scrollAmount == 0) {
            return ZOOM_NONE;
        }
        if (scrollAmount > 0) {
            return ZOOM_IN;
        } else {
            return ZOOM_OUT;
        }
    }

    private static Zoom of(final ZoomEvent event) {
        final double scrollAmount = event.getZoomFactor();

        if (scrollAmount == 0) {
            return ZOOM_NONE;
        }
        if (scrollAmount > 0) {
            return ZOOM_IN;
        } else {
            return ZOOM_OUT;
        }
    }
}
