package org.icroco.picture.thumbnail;

/**
 * Used to define the different types of rotations that can be applied to an
 * image during a resize operation.
 *
 * @author Riyad Kalla (software@thebuzzmedia.com)
 * @since 3.2
 */
public enum ERotation {
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
}
