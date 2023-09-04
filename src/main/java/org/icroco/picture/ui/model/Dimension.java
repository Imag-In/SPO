package org.icroco.picture.ui.model;

public record Dimension(int width, int height) {
    public static Dimension EMPTY_DIM = new Dimension(0, 0);

    @Override
    public String toString() {
        if (width <= 0 || height <= 0) {
            return "-";
        }
        return width + " x " + height;
    }
}
