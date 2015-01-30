package org.landscapelib.voxel;

import com.badlogic.gdx.math.Vector3;
import org.flowutils.SimplexGradientNoise;

/**
 *
 */
public class TestWorldFunction implements WorldFunction {

    private final SimplexGradientNoise noise = new SimplexGradientNoise();

    private float planetRadiusMeters = 10000;
    private Vector3 planetCenter = new Vector3(0, -planetRadiusMeters, 0);


    @Override public byte getTerrainType(Vector3 worldPos, double scale) {
        double scale1 = 0.002;
        double scale2 = 0.07;
        double scale3 = 0.1;
        double density1 = noise.sdnoise3(worldPos.x * scale1,
                                         worldPos.y * scale1,
                                         worldPos.z * scale1);
        double density2 = noise.sdnoise3(worldPos.x * scale2 + 3123,
                                         worldPos.y * scale2 + 434.3,
                                         worldPos.z * scale2 + 123.321);
        /*
        double density3 = noise.sdnoise3(worldPos.x * scale3 + 1234,
                                         worldPos.y * scale3 +3542,
                                         worldPos.z * scale3 +23);
*/
        double planetDensity = planetRadiusMeters - planetCenter.dst(worldPos);

        double density = planetDensity +
                         density1 * 200 +
                         density2 * 2 /*+
                         density3 * 6*/;




        if (density < 0) return 0;
        else return 1;
    }
}
