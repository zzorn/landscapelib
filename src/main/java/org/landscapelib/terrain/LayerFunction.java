package org.landscapelib.terrain;

import com.badlogic.gdx.math.Vector3;

/**
 *
 */
public interface LayerFunction {

    /**
     * @param direction the direction vector to get the layer function value for.
     * @param stackSideLength length of the side of a stack at height 0.
     * @param currentStack existing layer stacks without the one produced by this function added.
     * @return layer function value.  Could be used as a layer thickness or absolute layer height.
     */
    float getValue(Vector3 direction, float stackSideLength, TerrainLayerStack currentStack);

}
