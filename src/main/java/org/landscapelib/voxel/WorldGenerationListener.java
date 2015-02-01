package org.landscapelib.voxel;

/**
 *
 */
public interface WorldGenerationListener {

    /**
     * @param progressZeroToOne calculation progress, between 0 and 1.
     * @return true to continue, false to stop calculation.
     */
    boolean calculationProgress(float progressZeroToOne);

    /**
     * Called when calculation is finished.
     */
    void calculationReady();

    /**
     * Called when calculation is canceled.
     */
    void calculationAborted();

}
