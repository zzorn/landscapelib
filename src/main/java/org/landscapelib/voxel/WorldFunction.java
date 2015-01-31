package org.landscapelib.voxel;

import com.badlogic.gdx.math.Vector3;

/**
 *
 */
public interface WorldFunction {

    Vector3 getGravitationCenter();

    /**
     * @param worldPos position to get terrain type at.
     * @return terrainType at the specified world position.
     */
    byte getTerrainType(Vector3 worldPos, double scale);


}
