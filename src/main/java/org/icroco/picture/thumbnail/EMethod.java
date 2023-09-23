package org.icroco.picture.thumbnail;

/**
 * Used to define the different scaling hints that the algorithm can use.
 *
 * @author Riyad Kalla (software@thebuzzmedia.com)
 * @since 1.1
 */
public enum EMethod {
    /**
     * Used to indicate that the scaling implementation should decide which
     * method to use in order to get the best looking scaled image in the
     * least amount of time.
     * <p/>
     * The scaling algorithm will use the
     * {@link #THRESHOLD_QUALITY_BALANCED} or
     * {@link #THRESHOLD_BALANCED_SPEED} thresholds as cut-offs to
     * decide between selecting the <code>QUALITY</code>,
     * <code>BALANCED</code> or <code>SPEED</code> scaling algorithms.
     * <p/>
     * By default the thresholds chosen will give nearly the best looking
     * result in the fastest amount of time. We intend this method to work
     * for 80% of people looking to scale an image quickly and get a good
     * looking result.
     */
    AUTOMATIC,
    /**
     * Used to indicate that the scaling implementation should scale as fast
     * as possible and return a result. For smaller images (800px in size)
     * this can result in noticeable aliasing but it can be a few magnitudes
     * times faster than using the QUALITY method.
     */
    SPEED,
    /**
     * Used to indicate that the scaling implementation should use a scaling
     * operation balanced between SPEED and QUALITY. Sometimes SPEED looks
     * too low quality to be useful (e.g. text can become unreadable when
     * scaled using SPEED) but using QUALITY mode will increase the
     * processing time too much. This mode provides a "better than SPEED"
     * quality in a "less than QUALITY" amount of time.
     */
    BALANCED,
    /**
     * Used to indicate that the scaling implementation should do everything
     * it can to create as nice of a result as possible. This approach is
     * most important for smaller pictures (800px or smaller) and less
     * important for larger pictures as the difference between this method
     * and the SPEED method become less and less noticeable as the
     * source-image size increases. Using the AUTOMATIC method will
     * automatically prefer the QUALITY method when scaling an image down
     * below 800px in size.
     */
    QUALITY,
    /**
     * Used to indicate that the scaling implementation should go above and
     * beyond the work done by {@link EMethod#QUALITY} to make the image look
     * exceptionally good at the cost of more processing time. This is
     * especially evident when generating thumbnails of images that look
     * jagged with some of the other {@link EMethod}s (even
     * {@link EMethod#QUALITY}).
     */
    ULTRA_QUALITY;
}
