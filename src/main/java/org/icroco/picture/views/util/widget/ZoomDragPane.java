package org.icroco.picture.views.util.widget;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.GestureEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;

@Slf4j
public class ZoomDragPane extends BorderPane {
    private static final double HALF          = 0.5d;
    /**
     * This is the number of Zoom-In operations required to
     * <b><i>almost exactly</i></b>
     * halve the size of the Viewport.
     */
    private static final int    ZOOM_N        = 9; // TODO try.: 1 <= ZOOM_N <= 20"-ish"
    /**
     * This factor guarantees that after
     * {@link  #ZOOM_N}
     * times Zoom-In, the Viewport-size halves
     * <b><i>almost exactly</i></b>.<br>
     * (HALF was chosen to - perhaps? - avoid excessive Image degradation when zooming)<br>
     * For ZOOM_N = 9 the factor value is approximately 93%
     */
    private static final double ZOOM_IN_SCALE = Math.pow(HALF, 1.0d / ZOOM_N);
    private static final double MIN_PX        = 10;

    private       int       zoomLevel = 0;
    @Getter
    private final ImageView view;
    private       double    imageWidth;
    private       double    imageHeight;
    private final double    rotation90scale;

    /**
     * Create a
     * {@link  Pane}
     * container for an
     * {@link  ImageView}
     * which encapsulates all the Zoom, Drag & Rotation logic for an Image.
     */
    public ZoomDragPane(Pane parent) {
        prefHeightProperty().bind(parent.heightProperty());
        prefWidthProperty().bind(parent.widthProperty());
//        setStyle("-fx-background-color: LIGHTGREY");

        view = new ImageView();
        view.setPreserveRatio(true);
        view.setSmooth(true);
        view.setCache(true);
        view.setPickOnBounds(true);
        setCenter(view);
        setImage(null);

        /*
         * Unless its square, the Image must be scaled when rotated through 90 (or 270) degrees...
         */
        rotation90scale = Math.min(imageWidth, imageHeight) / Math.max(imageWidth, imageHeight);

        view.fitWidthProperty().bind(widthProperty());
        view.fitHeightProperty().bind(heightProperty());

        setMouseDraggedEventHandler();
        view.setOnScroll(this::zoom);
        view.setOnZoom(this::zoom);
    }

    public final void setImage(@Nullable Image image) {
        zoomLevel = 0;
        if (image != null) {
            view.setImage(image);
            imageWidth = image.getWidth();
            imageHeight = image.getHeight();
            view.setViewport(new Rectangle2D(0, 0, imageWidth, imageHeight));
        } else {
            view.setViewport(null);
            imageHeight = getPrefHeight();
            imageWidth = getPrefWidth();
        }
    }

    /**
     * Drag the Viewport as the Mouse is moved.
     */
    private void setMouseDraggedEventHandler() {

        final ObjectProperty<Point2D> mouseDown = new SimpleObjectProperty<>();
        /*
         * Remember where the Mouse was in the Image when it was pressed...
         */
        view.setOnMousePressed(e -> mouseDown.set(imageViewToImage(e.getX(), e.getY())));
        /*
         * Using the above, work out how far the Mouse has been dragged & adjust the Viewport...
         */
        view.setOnMouseDragged(e -> {
            final Point2D dragPoint = imageViewToImage(e.getX(), e.getY());
            final Point2D dragDelta = dragPoint.subtract(mouseDown.get());

            final Rectangle2D viewport = view.getViewport();

            final double newX = viewport.getMinX() - dragDelta.getX();
            final double newY = viewport.getMinY() - dragDelta.getY();

            setImageViewport(newX, newY, viewport.getWidth(), viewport.getHeight());
        });
    }

    public void zoom(MouseEvent event) {
//        zoomLevel = 20;
        zoomInCentredToLevel(10);
//        zoom(Zoom.ZOOM_IN, event.getX(), event.getY());
    }

    public void noZoom() {
        zoomLevel = 1;
        zoom(Zoom.ZOOM_OUT, 0, 0);
    }

    /**
     * Zoom Event-Handler. Zooms In or Out exactly 1 Level (if at all).
     * <p>
     * Note.: the X-/Y-Coordinates returned by the ScrollEvent are relative-to-the-ImageView
     * and need to be normalised to relative-to-the-Image for the Zoom & Viewport calculations.
     */
    private void zoom(final GestureEvent event) {
        final Zoom zoom = Zoom.of(event);

        zoom(zoom, event.getX(), event.getY());
        event.consume();
    }

    public boolean isZoomed() {
        return zoomLevel <= 1;
    }

    private void zoom(Zoom zoom, double x, double y) {
        final int zoomLevelTry = zoomLevel + zoom.getZoomLevelDelta();

        /*
         * Zoomed out too far or no Zoom at all? Then there's nothing to do...
         */
        if (zoomLevelTry < 0
            || zoom == Zoom.ZOOM_NONE) {
            return;
        }
        /*
         * Calculate the Viewport Size for the desired Zoom-Level...
         */
        final Dimension2D newSize = zoomCalculateViewportSize(zoomLevelTry);
        /*
         * If the maximum Zoom-Level has been exceeded there's nothing to do...
         */
        if (Math.min(newSize.getWidth(), newSize.getHeight()) < MIN_PX) {
            return;
        }
        /* --------------------------------------------------------------
         *
         * OK, the new Zoom-Level is valid:
         * -> calculate the new Viewport X-/Y-coordinates & update the Viewport...
         * (we Zoom in or out centred around the Pixel at the Mouse position)
         */
        zoomLevel = zoomLevelTry;

        final Point2D mouseInImage = imageViewToImage(x, y);

        final Point2D newLocation = zoomCalculateNewViewportXY(mouseInImage, zoom.getScale());
        log.debug("zoomLevel: {}, scale:{}, x:{}, y:{}, nouseImage:{}, newLocation: {}", zoomLevel, zoom.getScale(), x, y, mouseInImage, newLocation);

        /*
         * Store the new Coordinates & Size in the Viewport...
         */
        setImageViewport(newLocation.getX(), newLocation.getY(), newSize.getWidth(), newSize.getHeight());
    }

    /**
     * To fix the Pixel @ the Mouse X-coordinate, the following is true:
     * <br>
     * {@code  (x - newViewportMinX) / (x - currentViewportMinX) = scale}
     * <p>
     * The new Viewport X-coordinate is therefore given by:
     * <br>
     * {@code  newViewportMinX = x - (x - currentViewportMinX) * scale}
     * <p>
     * The new Viewport Y-coordinate is calculated similarly.
     *
     * @param imageMouse the Mouse coordinates relative to the Image
     * @param scale      the Zoom-factor
     * @return X-/Y-coordinate of the new Viewport<br>
     * (which
     * {@link  #setImageViewport(double, double, double, double)}
     * will bring into Range if necessary)
     */
    private Point2D zoomCalculateNewViewportXY(final Point2D imageMouse, final double scale) {
        final Rectangle2D oldViewport = view.getViewport();

        final double mouseX = imageMouse.getX();
        final double mouseY = imageMouse.getY();

        final double newX = mouseX - (mouseX - oldViewport.getMinX()) * scale;
        final double newY = mouseY - (mouseY - oldViewport.getMinY()) * scale;

        return new Point2D(newX, newY);
    }

    /**
     * Calculate the Viewport size for a particular
     * {@code  zoomLevel}.
     *
     * @param zoomLevel the Zoom Level
     */
    private Dimension2D zoomCalculateViewportSize(final int zoomLevel) {
        final double zoomScale = Math.pow(ZOOM_IN_SCALE, zoomLevel);
        final double newWidth  = imageWidth * zoomScale;
        final double newHeight = imageHeight * zoomScale;

        return new Dimension2D(newWidth, newHeight);
    }

    /**
     * Zoom in to the requested
     * {@code  zoomLevel}.<br>
     * (the Viewport will be centred within the Image)
     */
    private void zoomInCentredToLevel(final int zoomLevel) {

        this.zoomLevel = zoomLevel;
        /*
         * Calculate the Viewport Size for the desired Zoom-Level...
         */
        final Dimension2D newSize = zoomCalculateViewportSize(zoomLevel);

        final double newX = (imageWidth - newSize.getWidth()) / 2;
        final double newY = (imageHeight - newSize.getHeight()) / 2;

        setImageViewport(newX, newY, newSize.getWidth(), newSize.getHeight());
    }

    /**
     * Calculate Mouse coordinates within the Image based on coordinates within the ImageView.
     *
     * @param viewX X-coordinate of the Mouse within the ImageView
     * @param viewY Y-coordinate of the Mouse within the ImageView
     * @return Coordinates of the Mouse within the Image
     */
    private Point2D imageViewToImage(final double viewX, final double viewY) {
        final Bounds boundsLocal = view.getBoundsInLocal();

        final double xProportion = viewX / boundsLocal.getWidth();
        final double yProportion = viewY / boundsLocal.getHeight();

        final Rectangle2D viewport = view.getViewport();

        final double imageX = viewport.getMinX() + xProportion * viewport.getWidth();
        final double imageY = viewport.getMinY() + yProportion * viewport.getHeight();

        return new Point2D(imageX, imageY);
    }

    /**
     * Store the new Coordinates & Size in the Viewport.<br>
     * (making sure that the Viewport remains within the Image)
     */
    private void setImageViewport(final double x, final double y, final double width, final double height) {
        final double xMax = imageWidth - width;
        final double yMax = imageHeight - height;

        final double xClamp = Math.max(0, Math.min(x, xMax)); // 0 <= x <= xMax
        final double yClamp = Math.max(0, Math.min(y, yMax)); // 0 <= y <= yMax

        view.setViewport(new Rectangle2D(xClamp, yClamp, width, height));
    }

    /**
     * The Image will either be rotated or the rotation will be reset to zero.
     * <p>
     * Note:
     * this rotation logic was conceived for multiples of 90 degrees.<br>
     * It can, however, handle any angle, but the rotation90scale logic would need a touch of Pythagoras.
     *
     * @param relativeRotation the rotation angle, in Degrees (0 = reset to zero)<br>
     */
    private void rotateOrReset(final double relativeRotation) {
        final double rotatePrevious = view.getRotate();

        if (relativeRotation == 0) {
            if (rotatePrevious != 0) {
                rotateAndScale(rotatePrevious, 0);
            }
        } else {
            rotateAndScale(rotatePrevious, rotatePrevious + relativeRotation);
        }
    }

    private void rotateAndScale(final double previousRotation, final double absoluteRotation) {
        view.setRotate(absoluteRotation);

        if (imageWidth == imageHeight) {
            return;
        }

        final boolean multiple180previous = previousRotation % 180 == 0;
        final boolean multiple180new      = absoluteRotation % 180 == 0;

        if (multiple180new == multiple180previous) {
            return;
        }
        if (multiple180new) {
            view.setScaleX(1.0d);
            view.setScaleY(1.0d);
        } else {
            view.setScaleX(rotation90scale);
            view.setScaleY(rotation90scale);
        }
    }

}

