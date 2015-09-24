package org.landscapelib.terrain;

import com.badlogic.gdx.math.Vector3;
import org.flowutils.Check;
import org.flowutils.MathUtils;

/**
 *
 */
public abstract class TerrainLayerBase implements TerrainLayer {

    @Override
    public final void addTerrainLayerToStacks(int sizeU, int sizeV,
                                             float stackSideLength,
                                             Vector3 u1v1,
                                             Vector3 u2v1,
                                             Vector3 u1v2,
                                             Vector3 u2v2,
                                             TerrainLayerStack[] layerStacksToFill) {
        Check.positive(sizeU, "sizeU");
        Check.positive(sizeV, "sizeV");
        Check.positive(stackSideLength, "stackSideLength");
        Check.equal(sizeU * sizeV, "sizeU * sizeV", layerStacksToFill.length, "layerStacksToFill.length");

        final Vector3 dir = new Vector3();
        final Vector3 uDir1 = new Vector3();
        final Vector3 uDir2 = new Vector3();
        for (int v = 0; v < sizeV; v++) {
            float relVPos = MathUtils.relPos(v, 0f, sizeV - 1f);
            uDir1.set(u1v1).lerp(u1v2, relVPos);
            uDir2.set(u2v1).lerp(u2v2, relVPos);

            for (int u = 0; u < sizeU; u++) {
                float relUPos = MathUtils.relPos(u, 0f, sizeU - 1f);
                dir.set(uDir1).lerp(uDir2, relUPos);

                addTerrainLayerToStack(dir, stackSideLength, layerStacksToFill[v * sizeU + u]);
            }
        }
    }

}
