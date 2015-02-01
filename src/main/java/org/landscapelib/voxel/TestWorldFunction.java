package org.landscapelib.voxel;

import com.badlogic.gdx.math.Vector3;
import org.flowutils.SimplexGradientNoise;

/**
 *
 */
public class TestWorldFunction implements WorldFunction {

    private static final int LISTENER_UPDATE_INTERVALL = 4;
    private final SimplexGradientNoise noise = new SimplexGradientNoise();

    private float planetRadiusMeters = 10000;
    private Vector3 planetCenter = new Vector3(0, -planetRadiusMeters, 0);

    @Override public Vector3 getGravitationCenter() {
        return planetCenter;
    }

    @Override public final void calculateChunk(final byte[] primaryMaterial,
                                               final byte[] secondaryMaterial,
                                               final byte[] materialRatio,
                                               final byte[] volume,
                                               final double centerX,
                                               final double centerY,
                                               final double centerZ,
                                               final int dataPointsAlongEachAxis,
                                               final double dataPointDistanceMeters,
                                               final WorldGenerationListener listener) {

        // Loop all datapoints

        final double centerOffset = 0.5 * (dataPointsAlongEachAxis - 1) * dataPointDistanceMeters;

        final double startX = centerX - centerOffset;
        final double startY = centerY - centerOffset;

        double xPos;
        double yPos;
        double zPos = centerZ - centerOffset;

        int index = 0;
        for (int z = 0; z < dataPointsAlongEachAxis; z++) {
            yPos = startY;
            for (int y = 0; y < dataPointsAlongEachAxis; y++) {
                xPos = startX;
                for (int x = 0; x < dataPointsAlongEachAxis; x++) {

                    //index = Chunk.calculateBlockIndex(x, y, z);

                    // Determine density at the location
                    int density = (int) (0xFF * getDensity(xPos, yPos, zPos, dataPointDistanceMeters));
                    if (density < 0) density = 0;
                    else if (density > 0xFF) density = 0xFF;

                    // TODO: Determine materials
                    primaryMaterial[index] = 1;
                    secondaryMaterial[index] = 1;
                    materialRatio[index] = (byte) 0xFF;
                    volume[index] = (byte) density;
                    //volume[index] = (centerY > 0) ? 0 : (byte) 1;

                    index++;
                    xPos += dataPointDistanceMeters;
                }
                yPos += dataPointDistanceMeters;
            }
            zPos += dataPointDistanceMeters;

            // Notify listener now and then
            /*
            if (listener != null && (zPos % LISTENER_UPDATE_INTERVALL) == 0) {
                final boolean continueCalculation = listener.calculationProgress((float) z / dataPointsAlongEachAxis);
                if (!continueCalculation) {
                    // Abort requested, stop calculation
                    listener.calculationAborted();
                    return;
                }
            }
            */
        }

        // Notify listener we are ready
        if (listener != null) {
            listener.calculationReady();
        }
    }


    private double getDensity(final double worldX,
                              final double worldY,
                              final double worldZ,
                              final double smallestFeatureScale) {
        double scale1 = 2000;
        double scale2 = 100;
        double scale3 = 17;

        final double a = planetCenter.x - worldX;
        final double b = planetCenter.y - worldY;
        final double c = planetCenter.z - worldZ;
        double distance = Math.sqrt(a * a + b * b + c * c);

        double planetDensity = planetRadiusMeters - distance;

        double density = planetDensity;

        if (smallestFeatureScale < scale1) {
            density += 400 * noise.sdnoise3(worldX / scale1,
                                            worldY / scale1,
                                            worldZ / scale1);
        }

        if (smallestFeatureScale < scale2) {
            double amplitude = 10 * noise.sdnoise3(worldX / (scale2 * 20) + 98213.123,
                                           worldY / (scale2 *32.3)+ 0123.123,
                                           worldZ / (scale2 * 13.32) + 9432.23);

            density += amplitude *amplitude * noise.sdnoise3(worldX / scale2 + 3123,
                                          worldY / scale2 + 434.3,
                                          worldZ / scale2 + 123.321);
        }

        if (smallestFeatureScale < scale3) {
            density += 3 * noise.sdnoise3(worldX / scale3 + 543,
                                            worldY / scale3 + 5434.3,
                                            worldZ / scale3 + 63.41);
        }

        return density;
    }
}
