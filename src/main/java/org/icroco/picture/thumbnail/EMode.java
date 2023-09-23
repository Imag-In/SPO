package org.icroco.picture.thumbnail;

/**
 * Used to define the different modes of resizing that the algorithm can
 * use.
 *
 * @author Riyad Kalla (software@thebuzzmedia.com)
 * @since 3.1
 */
public enum EMode {
    /**
     * Used to indicate that the scaling implementation should calculate
     * dimensions for the resultant image by looking at the image's
     * orientation and generating proportional dimensions that best fit into
     * the target width and height given
     * <p>
     * See "Image Proportions" in the {@link ImgscalrGenerator} class description for
     * more detail.
     */
    AUTOMATIC,
    /**
     * Used to fit the image to the exact dimensions given regardless of the
     * image's proportions. If the dimensions are not proportionally
     * correct, this will introduce vertical or horizontal stretching to the
     * image.
     * <p/>
     * It is recommended that you use one of the other <code>FIT_TO</code>
     * modes or {@link EMode#AUTOMATIC} if you want the image to look
     * correct, but if dimension-fitting is the #1 priority regardless of
     * how it makes the image look, that is what this mode is for.
     */
    FIT_EXACT,
    /**
     * Used to indicate that the scaling implementation should calculate
     * dimensions for the largest image that fit within the bounding box,
     * without cropping or distortion, retaining the original proportions.
     */
    BEST_FIT_BOTH,
    /**
     * Used to indicate that the scaling implementation should calculate
     * dimensions for the resultant image that best-fit within the given
     * width, regardless of the orientation of the image.
     */
    FIT_TO_WIDTH,
    /**
     * Used to indicate that the scaling implementation should calculate
     * dimensions for the resultant image that best-fit within the given
     * height, regardless of the orientation of the image.
     */
    FIT_TO_HEIGHT;
}
