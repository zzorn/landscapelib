package org.landscapelib.terrain;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import org.flowutils.Check;

/**
 *
 */
public class TerrainLayerStack {

    private static final int DEFAULT_CAPACITY = 64;

    private double maxHeight = 0;

    private final FloatArray layerTopHeights = new FloatArray(true, DEFAULT_CAPACITY);
    private final Array<LayerType> layerTypes = new Array<LayerType>(true, DEFAULT_CAPACITY, LayerType.class);


    public void addLayer(double height, LayerType layerType) {
        addLayer(maxHeight, maxHeight + height, layerType);
    }

    public void addLayer(double bottomHeight, double topHeight, LayerType layerType) {
        Check.positiveOrZero(bottomHeight, "bottomHeight");
        if (topHeight > bottomHeight) {
            if (bottomHeight < maxHeight) {
                // This layer splits or covers one or more existing layers
                float existingLayerBottom = 0;
                int layerIndex = 0;
                int insertionIndex = 0;
                while (existingLayerBottom < topHeight && layerIndex < layerTypes.size) {
                    final float existingLayerTop = layerTopHeights.get(layerIndex);

                    // Determine index to add new layer at
                    // TODO: Test that this is correct
                    if (bottomHeight >= existingLayerBottom) insertionIndex = layerIndex;

                    if (existingLayerBottom < bottomHeight && existingLayerTop > topHeight) {
                        // Existing layer top cut off
                        layerTopHeights.set(layerIndex, (float) bottomHeight);

                        layerIndex++;
                    } else if (existingLayerBottom > topHeight) {
                        // Passed the layer to be added
                        break;
                    }  else if (existingLayerBottom < bottomHeight && existingLayerTop > topHeight) {
                        // Existing layer cut in half
                        layerTopHeights.set(layerIndex, (float) bottomHeight);

                        layerTopHeights.insert(layerIndex + 1, existingLayerTop);
                        layerTypes.insert(layerIndex + 1, layerTypes.get(layerIndex));

                        break;
                    }  else if (existingLayerBottom >= bottomHeight && existingLayerTop <= topHeight) {
                        // Completely covered, remove existing layer
                        layerTopHeights.removeIndex(layerIndex);
                        layerTypes.removeIndex(layerIndex);
                    }

                    existingLayerBottom = existingLayerTop;
                }

                // Add new layer at correct place
                layerTopHeights.insert(insertionIndex, (float) topHeight);
                layerTypes.insert(insertionIndex, layerType);
            }
            else {
                // Topmost layer, add directly
                layerTypes.add(layerType);
                layerTopHeights.add((float) topHeight);
            }

            maxHeight = Math.max(maxHeight, topHeight);
        }

    }

    /**
     * Upper heights of layers, starting at lowest one.  It is assumed that the first layer starts from 0.
     */
    public FloatArray getLayerTopHeights() {
        return layerTopHeights;
    }


    /**
     * @return types of layers, starting at lowest one.
     */
    public Array<LayerType> getLayerTypes() {
        return layerTypes;
    }

    public void clear() {
        maxHeight = 0;
        layerTypes.clear();
        layerTopHeights.clear();
    }
}
