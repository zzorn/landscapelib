package org.landscapelib.terrain;

import com.badlogic.gdx.math.Vector3;

/**
 *
 */
public interface TerrainLayer {

    void addTerrainLayerToStack(Vector3 direction, float stackSideLength, TerrainLayerStack layerStackToFill);

    void addTerrainLayerToStacks(int sizeU,
                                 int sizeV,
                                 float stackSideLength,
                                 Vector3 u1v1,
                                 Vector3 u2v1,
                                 Vector3 u1v2,
                                 Vector3 u2v2,
                                 TerrainLayerStack[] layerStacksToFill);

}
