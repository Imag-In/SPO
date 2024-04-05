package org.icroco.picture.model;

import lombok.Builder;

@Builder
public record Dimension(int width, int height) {

    public Dimension(double witdh, double height) {
        this((int) witdh, (int) height);
    }

    public static Dimension EMPTY_DIM = new Dimension(0, 0);

    public boolean isLesserThan(Dimension d) {
        return width <= d.width && height <= d.height();
    }

    @Override
    public String toString() {
        if (width <= 0 || height <= 0) {
            return "-";
        }
        return STR."\{width} x \{height}";
    }
}
