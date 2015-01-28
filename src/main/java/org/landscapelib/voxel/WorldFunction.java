package org.landscapelib.voxel;

import com.badlogic.gdx.math.Vector3;

/**
 *
 */
public interface WorldFunction {

    /**
     * @param worldPos position to get terrain type at.
     * @return terrainType at the specified world position.
     */
    byte getTerrainType(Vector3 worldPos, double scale);


}
