package org.landscapelib.terrain;

import com.badlogic.gdx.math.Vector3;

import static org.flowutils.Check.notNull;

/**
 *
 */
public final class AddOnTerrainLayer extends TerrainLayerBase {

    private final LayerType layerType;
    private final LayerFunction thickness;

    public AddOnTerrainLayer(LayerType layerType, LayerFunction thickness) {
        notNull(layerType, "layerType");
        notNull(thickness, "thickness");

        this.layerType = layerType;
        this.thickness = thickness;
    }

    @Override
    public void addTerrainLayerToStack(Vector3 direction, float stackSideLength, TerrainLayerStack layerStackToFill) {
        float layerThickness = thickness.getValue(direction, stackSideLength, layerStackToFill);
        layerStackToFill.addLayer(layerThickness, layerType);
    }
}
