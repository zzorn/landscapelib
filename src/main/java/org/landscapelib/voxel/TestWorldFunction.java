package org.landscapelib.voxel;

import com.badlogic.gdx.math.Vector3;
import org.flowutils.SimplexGradientNoise;

/**
 *
 */
public class TestWorldFunction implements WorldFunction {

    private final SimplexGradientNoise noise = new SimplexGradientNoise();

    @Override public byte getTerrainType(Vector3 worldPos, double scale) {
        double scale1 = 0.02;
        double scale2 = 0.08;
        double density1 = noise.sdnoise3(worldPos.x * scale1,
                                         worldPos.y * scale1,
                                         worldPos.z * scale1);
        double density2 = noise.sdnoise3(worldPos.x * scale2,
                                         worldPos.y * scale2,
                                         worldPos.z * scale2);
        double density = 0f - worldPos.y * 0.02 + density1 * 0.9 + density2 * 0.2;

        if (density < 0) return 0;
        else return 1;
    }
}
