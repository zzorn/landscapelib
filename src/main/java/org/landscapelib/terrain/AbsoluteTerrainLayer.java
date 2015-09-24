package org.landscapelib.terrain;

import com.badlogic.gdx.math.Vector3;

import static org.flowutils.Check.notNull;

/**
 *
 */
public final class AbsoluteTerrainLayer extends TerrainLayerBase {

    private final LayerType layerType;
    private final LayerFunction startHeight;
    private final LayerFunction thickness;

    public AbsoluteTerrainLayer(LayerType layerType, LayerFunction startHeight, LayerFunction thickness) {
        notNull(layerType, "layerType");
        notNull(startHeight, "startHeight");
        notNull(thickness, "thickness");

        this.layerType = layerType;
        this.startHeight = startHeight;
        this.thickness = thickness;
    }

    @Override
    public void addTerrainLayerToStack(Vector3 direction, float stackSideLength, TerrainLayerStack layerStackToFill) {
        float layerThickness = thickness.getValue(direction, stackSideLength, layerStackToFill);
        float layerStart = startHeight.getValue(direction, stackSideLength, layerStackToFill);
        layerStackToFill.addLayer(layerStart + layerThickness, layerStart, layerType);
    }
}
