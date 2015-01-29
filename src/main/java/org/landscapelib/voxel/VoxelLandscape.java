package org.landscapelib.voxel;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.utils.Array;
import org.flowutils.Check;

import static org.flowutils.Check.notNull;

/**
 *
 */
public class VoxelLandscape {

    private final Array<DetailLevel> detailLevels = new Array<DetailLevel>();

    private final WorldFunction worldFunction;
    private final Camera camera;
    private final ChunkManager chunkManager;

    private static final int LAYER_SIZE = 6;
    private static final int MARGIN_SIZE = 1;
    private static final int HOLE_SIZE = 3;

    public VoxelLandscape(int numDetailLevels,
                          float mostDetailedChunkSizeMeters,
                          WorldFunction worldFunction,
                          Camera camera,
                          ChunkManager chunkManager) {

        Check.positive(numDetailLevels, "numDetailLevels");
        Check.positive(mostDetailedChunkSizeMeters, "mostDetailedChunkSizeMeters");
        notNull(worldFunction, "worldFunction");
        notNull(camera, "camera");
        notNull(chunkManager, "chunkManager");


        this.worldFunction = worldFunction;
        this.camera = camera;
        this.chunkManager = chunkManager;

        float chunkSizeMeters = mostDetailedChunkSizeMeters;

        float chunkSizeChange = (float)LAYER_SIZE / HOLE_SIZE;

        // Create detail levels
        for (int i = 0; i < numDetailLevels; i++) {
            detailLevels.add(new DetailLevel(worldFunction,
                                             camera,
                                             chunkSizeMeters,
                                             chunkManager,
                                             LAYER_SIZE,
                                             i == 0 ? 0 : HOLE_SIZE,
                                             MARGIN_SIZE));

            chunkSizeMeters *= chunkSizeChange;
        }
    }

    public void update(double secondsSinceLastCall) {
        for (DetailLevel detailLevel : detailLevels) {
            detailLevel.update(secondsSinceLastCall);
        }
    }

    public void render(ModelBatch modelBatch, Environment environment) {
        for (DetailLevel detailLevel : detailLevels) {
            detailLevel.render(modelBatch, environment);
        }
    }

    public void dispose() {
        for (DetailLevel detailLevel : detailLevels) {
            detailLevel.dispose();
        }

        detailLevels.clear();
    }
}
