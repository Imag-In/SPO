package org.icroco.picture.ui.util;

import javafx.beans.DefaultProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.*;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;

import java.util.List;

/**
 * comes from https://gitlab.com/-/snippets/2509295
 */
@DefaultProperty("image")
public class ResizableImageView extends Region {

    private static final Pos     DEFAULT_ALIGNMENT      = Pos.CENTER;
    private static final FitMode DEFAULT_FIT_MODE       = FitMode.FIT_BOTH;
    private static final boolean DEFAULT_PRESERVE_RATIO = true;

    /* **************************************************************************
     *                                                                          *
     * Properties                                                               *
     *                                                                          *
     ****************************************************************************/

    // -- alignment property

    private final StyleableObjectProperty<Pos> alignment =
            new SimpleStyleableObjectProperty<>(StyleableProperties.ALIGNMENT, this, "alignment", DEFAULT_ALIGNMENT) {
                @Override
                protected void invalidated() {
                    requestLayout();
                }
            };

    public final void setAlignment(Pos alignment) {
        this.alignment.set(alignment);
    }

    public final Pos getAlignment() {
        return alignment.get();
    }

    private Pos getAlignmentSafe() {
        var alignment = getAlignment();
        return alignment == null ? DEFAULT_ALIGNMENT : alignment;
    }

    public final ObjectProperty<Pos> alignmentProperty() {
        return alignment;
    }

    // -- fitMode property

    private final StyleableObjectProperty<FitMode> fitMode =
            new SimpleStyleableObjectProperty<>(StyleableProperties.FIT_MODE, this, "fitMode", DEFAULT_FIT_MODE) {
                @Override
                protected void invalidated() {
                    requestLayout();
                }
            };

    public final void setFitMode(FitMode fitMode) {
        this.fitMode.set(fitMode);
    }

    public final FitMode getFitMode() {
        return fitMode.get();
    }

    private FitMode getFitModeSafe() {
        var mode = getFitMode();
        return mode == null ? DEFAULT_FIT_MODE : mode;
    }

    public final ObjectProperty<FitMode> fitModeProperty() {
        return fitMode;
    }

    // -- preserveRatio property

    private final StyleableBooleanProperty preserveRatio =
            new SimpleStyleableBooleanProperty(
                    StyleableProperties.PRESERVE_RATIO, this, "preserveRatio", DEFAULT_PRESERVE_RATIO) {
                @Override
                protected void invalidated() {
                    imageView.setPreserveRatio(get());
                }
            };

    public final void setPreserveRatio(boolean preserveRatio) {
        this.preserveRatio.set(preserveRatio);
    }

    public final boolean isPreserveRatio() {
        return preserveRatio.get();
    }

    public final BooleanProperty preserveRatioProperty() {
        return preserveRatio;
    }

    // -- image property

    private final ObjectProperty<Image> image = new SimpleObjectProperty<>(this, "image") {
        @Override
        protected void invalidated() {
            imageView.setImage(get());
        }
    };

    public final void setImage(Image image) {
        this.image.set(image);
    }

    public final Image getImage() {
        return image.get();
    }

    public final ObjectProperty<Image> imageProperty() {
        return image;
    }

    /* **************************************************************************
     *                                                                          *
     * Instance Fields                                                          *
     *                                                                          *
     ****************************************************************************/

    private final ImageView imageView = new ImageView();

    /* **************************************************************************
     *                                                                          *
     * Constructors                                                             *
     *                                                                          *
     ****************************************************************************/

    public ResizableImageView() {
        imageView.setPreserveRatio(DEFAULT_PRESERVE_RATIO);
        getStyleClass().add(DEFAULT_STYLE_CLASS);
        getChildren().add(imageView);
    }

    public ResizableImageView(Image image) {
        this();
        setImage(image);
    }

    /* **************************************************************************
     *                                                                          *
     * Layout Handling                                                          *
     *                                                                          *
     ****************************************************************************/

    @Override
    protected void layoutChildren() {
        double x = getInsets().getLeft();
        double y = getInsets().getTop();
        double w = getWidth() - getInsets().getRight() - x;
        double h = getHeight() - getInsets().getBottom() - y;

        var mode = getFitModeSafe();
        mode.setFitWidth(imageView, getImageWidth(), w);
        mode.setFitHeight(imageView, getImageHeight(), h);

        var alignment = getAlignmentSafe();
        positionInArea(imageView, x, y, w, h, -1.0, alignment.getHpos(), alignment.getVpos());
    }

    @Override
    protected double computeMinWidth(double height) {
        double minWidth = getInsets().getLeft() + getInsets().getRight();
        minWidth += switch (getFitModeSafe()) {
            case FIT_HEIGHT, NO_FIT -> imageView.minWidth(height);
            case FIT_WIDTH, FIT_BOTH -> 0.0;
        };
        return minWidth;
    }

    @Override
    protected double computeMinHeight(double width) {
        double minHeight = getInsets().getTop() + getInsets().getBottom();
        minHeight += switch (getFitModeSafe()) {
            case FIT_WIDTH, NO_FIT -> imageView.minHeight(width);
            case FIT_HEIGHT, FIT_BOTH -> 0.0;
        };
        return minHeight;
    }

    @Override
    protected double computePrefWidth(double height) {
        double prefWidth = getInsets().getLeft() + getInsets().getRight();
        prefWidth += switch (getFitModeSafe()) {
            case FIT_HEIGHT, NO_FIT -> imageView.prefWidth(height);
            case FIT_WIDTH, FIT_BOTH -> getImageWidth();
        };
        return prefWidth;
    }

    @Override
    protected double computePrefHeight(double width) {
        double prefHeight = getInsets().getTop() + getInsets().getBottom();
        prefHeight += switch (getFitModeSafe()) {
            case FIT_WIDTH, NO_FIT -> imageView.prefHeight(width);
            case FIT_HEIGHT, FIT_BOTH -> getImageHeight();
        };
        return prefHeight;
    }

    @Override
    protected double computeMaxWidth(double height) {
        return Double.MAX_VALUE;
    }

    @Override
    protected double computeMaxHeight(double width) {
        return Double.MAX_VALUE;
    }

    private double getImageWidth() {
        var image = getImage();
        return image == null ? 0.0 : image.getWidth();
    }

    private double getImageHeight() {
        var image = getImage();
        return image == null ? 0.0 : image.getHeight();
    }

    /* **************************************************************************
     *                                                                          *
     * Nested Classes                                                           *
     *                                                                          *
     ****************************************************************************/

    public enum FitMode {
        FIT_WIDTH {
            @Override
            void setFitWidth(ImageView view, double imageWidth, double contentWidth) {
                view.setFitWidth(imageWidth <= contentWidth ? 0.0 : contentWidth);
            }
        },
        FIT_HEIGHT {
            @Override
            void setFitHeight(ImageView view, double imageHeight, double contentHeight) {
                view.setFitHeight(imageHeight <= contentHeight ? 0.0 : contentHeight);
            }
        },
        FIT_BOTH {
            @Override
            void setFitWidth(ImageView view, double imageWidth, double contentWidth) {
                FIT_WIDTH.setFitWidth(view, imageWidth, contentWidth);
            }

            @Override
            void setFitHeight(ImageView view, double imageHeight, double contentHeight) {
                FIT_HEIGHT.setFitHeight(view, imageHeight, contentHeight);
            }
        },
        NO_FIT;

        void setFitWidth(ImageView view, double imageWidth, double contentWidth) {
            view.setFitWidth(0.0);
        }

        void setFitHeight(ImageView view, double imageHeight, double contentHeight) {
            view.setFitHeight(0.0);
        }
    }

    /* **************************************************************************
     *                                                                          *
     * Stylesheet Handling                                                      *
     *                                                                          *
     ****************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "resizable-image-view";

    public static List<CssMetaData<?, ?>> getClassCssMetaData() {
        return StyleableProperties.META_DATA;
    }

    @Override
    public List<CssMetaData<?, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    private static class StyleableProperties {

        private static final CssMetaData<ResizableImageView, Pos>     ALIGNMENT;
        private static final CssMetaData<ResizableImageView, FitMode> FIT_MODE;
        private static final CssMetaData<ResizableImageView, Boolean> PRESERVE_RATIO;

        private static final List<CssMetaData<?, ?>> META_DATA;

        static {
            var factory = new StyleablePropertyFactory<ResizableImageView>(Region.getClassCssMetaData());
            ALIGNMENT = factory.createEnumCssMetaData(Pos.class, "-fx-alignment", s -> s.alignment, DEFAULT_ALIGNMENT);
            FIT_MODE = factory.createEnumCssMetaData(FitMode.class, "-fx-fit-mode", s -> s.fitMode, DEFAULT_FIT_MODE);
            PRESERVE_RATIO = factory.createBooleanCssMetaData(
                    "-fx-preserve-ratio", s -> s.preserveRatio, DEFAULT_PRESERVE_RATIO);
            META_DATA = List.copyOf(factory.getCssMetaData());
        }
    }
}
