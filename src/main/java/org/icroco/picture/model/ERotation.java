package org.icroco.picture.model;

import java.util.Map;

/**
 * Used to define the different types of rotations that can be applied to an
 * image during a resize operation.
 *
 * @author Riyad Kalla (software@thebuzzmedia.com)
 * @since 3.2
 */
public enum ERotation {
    CW_0,
    /**
     * 90-degree, clockwise rotation (to the right). This is equivalent to a
     * quarter-turn of the image to the right; moving the picture on to its
     * right side.
     */
    CW_90,
    /**
     * 180-degree, clockwise rotation (to the right). This is equivalent to
     * 1 half-turn of the image to the right; rotating the picture around
     * until it is upside down from the original position.
     */
    CW_180,
    /**
     * 270-degree, clockwise rotation (to the right). This is equivalent to
     * a quarter-turn of the image to the left; moving the picture on to its
     * left side.
     */
    CW_270,
    /**
     * Flip the image horizontally by reflecting it around the y axis.
     * <p/>
     * This is not a standard rotation around a center point, but instead
     * creates the mirrored reflection of the image horizontally.
     * <p/>
     * More specifically, the vertical orientation of the image stays the
     * same (the top stays on top, and the bottom on bottom), but the right
     * and left sides flip. This is different than a standard rotation where
     * the top and bottom would also have been flipped.
     */
    FLIP_HORZ,
    /**
     * Flip the image vertically by reflecting it around the x axis.
     * <p/>
     * This is not a standard rotation around a center point, but instead
     * creates the mirrored reflection of the image vertically.
     * <p/>
     * More specifically, the horizontal orientation of the image stays the
     * same (the left stays on the left and the right stays on the right),
     * but the top and bottom sides flip. This is different than a standard
     * rotation where the left and right would also have been flipped.
     */
    FLIP_VERT;

    private final static ERotation[]               EMPTY = new ERotation[0];
    private final static Map<Integer, ERotation[]> CACHE = Map.ofEntries(Map.entry(1, new ERotation[0]),
                                                                         Map.entry(2, new ERotation[]{ERotation.FLIP_VERT}),
                                                                         Map.entry(3, new ERotation[]{ERotation.CW_180}),
                                                                         Map.entry(4,
                                                                                   new ERotation[]{ERotation.CW_180, ERotation.FLIP_VERT}),
                                                                         Map.entry(5,
                                                                                   new ERotation[]{ERotation.CW_90, ERotation.FLIP_HORZ}),
                                                                         Map.entry(6, new ERotation[]{ERotation.CW_90}),
                                                                         Map.entry(7,
                                                                                   new ERotation[]{ERotation.CW_270, ERotation.FLIP_HORZ}),
                                                                         Map.entry(8, new ERotation[]{ERotation.CW_270})
    );


    public static ERotation[] fromOrientation(int orientation) {
        return CACHE.getOrDefault(orientation, EMPTY);
    }
}
