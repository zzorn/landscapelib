package org.landscapelib.terrain;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

import static org.flowutils.Check.notNull;

/**
 *
 */
public final class TerrainFunctionImpl implements TerrainFunction {

    private final Array<TerrainLayer> layers = new Array<TerrainLayer>();

    /**
     * @param terrainLayer terrain layer to add.  Will be added after existing layers.
     */
    public void addLayer(TerrainLayer terrainLayer) {
        notNull(terrainLayer, "terrainLayer");
        layers.add(terrainLayer);
    }

    /**
     * @param terrainLayer terrain layer to remove.
     */
    public void removeLayer(TerrainLayer terrainLayer) {
        layers.removeValue(terrainLayer, true);
    }

    @Override
    public void getTerrainStack(Vector3 direction, float stackSideLength, TerrainLayerStack layerStackToFill) {
        for (int i = 0; i < layers.size; i++) {
            layers.get(i).addTerrainLayerToStack(direction, stackSideLength, layerStackToFill);
        }
    }

    @Override
    public void getTerrainStacks(int sizeU, int sizeV, float stackSideLength, Vector3 u1v1, Vector3 u2v1, Vector3 u1v2, Vector3 u2v2, TerrainLayerStack[] layerStacksToFill) {
        for (int i = 0; i < layers.size; i++) {
            layers.get(i).addTerrainLayerToStacks(sizeU, sizeV, stackSideLength, u1v1, u2v1, u1v2, u2v2, layerStacksToFill);
        }
    }
}
