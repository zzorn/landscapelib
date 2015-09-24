package org.landscapelib.terrain;

import com.badlogic.gdx.math.Vector3;

/**
 *
 */
// IDEA: Should this implement TerrainLayer?
public interface TerrainFunction {

    void getTerrainStack(Vector3 direction, float stackSideLength, TerrainLayerStack layerStackToFill);

    void getTerrainStacks(int sizeU,
                          int sizeV,
                          float stackSideLength,
                          Vector3 u1v1,
                          Vector3 u2v1,
                          Vector3 u1v2,
                          Vector3 u2v2,
                          TerrainLayerStack[] layerStacksToFill);

}
