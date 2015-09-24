package org.landscapelib.terrain;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;

/**
 * Information for one location on the terrain,
 * what materials there exists at that point, and the heights that the materials
 * are located at.
 */
public final class TerrainLayerStack {

    private static final int DEFAULT_CAPACITY = 64;

    private float maxHeight = 0;

    private final FloatArray layerTopHeights = new FloatArray(true, DEFAULT_CAPACITY);
    private final FloatArray layerBottomHeights = new FloatArray(true, DEFAULT_CAPACITY);
    private final Array<LayerType> layerTypes = new Array<LayerType>(true, DEFAULT_CAPACITY, LayerType.class);


    /**
     * @return number of layers at this point.
     */
    public int getLayerCount() {
        return layerTopHeights.size;
    }

    /**
     * @return Upper heights of layers, starting at highest one.
     * The returned array should not be modified.
     */
    public FloatArray getLayerTopHeights() {
        return layerTopHeights;
    }


    /**
     * @return Lower heights of layers, starting at highest one.
     * The returned array should not be modified.
     */
    public FloatArray getLayerBottomHeights() {
        return layerTopHeights;
    }


    /**
     * @return types of layers, starting at highest one.
     * The returned array should not be modified.
     */
    public Array<LayerType> getLayerTypes() {
        return layerTypes;
    }

    /**
     * Removes all layers from this layer stack.
     */
    public void clear() {
        maxHeight = 0;
        layerTypes.clear();
        layerTopHeights.clear();
        layerBottomHeights.clear();
    }

    /**
     * Adds a new layer to the top of the layer stack.
     * @param thickness used as height above the current topmost layer.
     * @param layerType type of this layer.
     */
    public void addLayer(float thickness, LayerType layerType) {
        addLayer(maxHeight + thickness, maxHeight, layerType);
    }

    /**
     * Adds a new layer to the layer stack.
     * @param topHeight height at the top of the layer.
     * @param bottomHeight height at the bottom of the layer.
     * @param layerType type of this layer.
     */
    public void addLayer(float topHeight, float bottomHeight, LayerType layerType) {
        if (topHeight > bottomHeight) {
            if (bottomHeight >= maxHeight || getLayerCount() == 0) {
                // Topmost (or first) layer, add directly
                insertLayer(0, topHeight, bottomHeight, layerType);
                maxHeight = topHeight;
            } else {
                // This layer splits or covers zero or more existing layers
                // Shrink or remove existing layers as necessary if the new layer overlaps them
                makeSpaceForNewLayer(topHeight, bottomHeight);

                // Add the new layer at the correct position
                addLayerAtCorrectPosition(topHeight, bottomHeight, layerType);

                maxHeight = Math.max(maxHeight, topHeight);
            }
        }
    }

    public void makeSpaceForNewLayer(float topHeight, float bottomHeight) {
        for (int layerIndex = 0; layerIndex < getLayerCount(); ) {
            float existingLayerTop = layerTopHeights.get(layerIndex);
            float existingLayerBottom = layerBottomHeights.get(layerIndex);

            // Adjust existing layers that get overlapped by the new layer
            if (existingLayerTop <= topHeight &&
                existingLayerBottom >= bottomHeight) {
                // Total overlap, remove existing layer
                removeLayer(layerIndex);
            }
            else if (existingLayerTop > topHeight &&
                     existingLayerBottom < bottomHeight) {
                // Existing layer split in two
                setLayerTopAndBottom(layerIndex, bottomHeight, existingLayerBottom);
                insertLayer(layerIndex, existingLayerTop, topHeight, layerTypes.get(layerIndex));
                layerIndex += 2;
            }
            else if (existingLayerTop > topHeight &&
                     existingLayerBottom < topHeight) {
                // Bottom shaved
                layerBottomHeights.set(layerIndex, topHeight);
                layerIndex++;
            }
            else if (existingLayerTop > bottomHeight &&
                     existingLayerBottom < bottomHeight) {
                // Top shaved
                layerTopHeights.set(layerIndex, bottomHeight);
                layerIndex++;
            }
            else {
                // No overlap
                layerIndex++;
            }
        }
    }

    public void addLayerAtCorrectPosition(float topHeight, float bottomHeight, LayerType layerType) {
        for (int layerIndex = 0; layerIndex < getLayerCount(); layerIndex++) {
            float existingLayerTop = layerTopHeights.get(layerIndex);
            if (topHeight <= existingLayerTop) {
                insertLayer(layerIndex, topHeight, bottomHeight, layerType);
                return;
            }
        }

        // Add last
        insertLayer(getLayerCount(), topHeight, bottomHeight, layerType);
    }

    private void insertLayer(int layerIndex, float topHeight, float bottomHeight, LayerType layerType) {
        layerTopHeights.insert(layerIndex, topHeight);
        layerBottomHeights.insert(layerIndex, bottomHeight);
        layerTypes.insert(layerIndex, layerType);
    }

    private void setLayerTopAndBottom(int layerIndex, float topHeight, float bottomHeight) {
        layerTopHeights.set(layerIndex, topHeight);
        layerBottomHeights.set(layerIndex, topHeight);
    }

    private void removeLayer(int layerIndex) {
        layerTopHeights.removeIndex(layerIndex);
        layerBottomHeights.removeIndex(layerIndex);
        layerTypes.removeIndex(layerIndex);
    }

}
