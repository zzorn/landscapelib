package org.landscapelib.voxel;

import com.badlogic.gdx.math.Vector3;

/**
 *
 */
public interface WorldFunction {

    Vector3 getGravitationCenter();

    void calculateChunk(byte[] primaryMaterial,
                        byte[] secondaryMaterial,
                        byte[] materialRatio,
                        byte[] volume,
                        double centerX,
                        double centerY,
                        double centerZ,
                        int dataPointsAlongEachAxis,
                        double dataPointDistanceMeters,
                        WorldGenerationListener listener);

}
