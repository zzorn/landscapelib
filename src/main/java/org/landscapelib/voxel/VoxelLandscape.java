package org.landscapelib.voxel;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import org.flowutils.Check;

import static org.flowutils.Check.notNull;

/**
 *
 */
public class VoxelLandscape {

    private final DetailLevel[] detailLevels;

    private final WorldFunction worldFunction;
    private final Camera camera;
    private final ChunkManager chunkManager;

    private static final int LAYER_SIZE = 8;
    private static final int MARGIN_SIZE = 0;
    private static final int HOLE_SIZE = 4;

    private static final int DEFAULT_DETAIL_LEVELS = 15;
    private static final float DEFAULT_MOST_DETAILED_BLOCK_SIZE_METERS = 0.5f;

    private final ModelBuilder modelBuilder = new ModelBuilder();
    private final ChunkMeshGenerator chunkMeshGenerator = new ChunkMeshGenerator();



    public VoxelLandscape(WorldFunction worldFunction,
                          Camera camera,
                          ChunkManager chunkManager) {
        this(DEFAULT_DETAIL_LEVELS, DEFAULT_MOST_DETAILED_BLOCK_SIZE_METERS, worldFunction, camera, chunkManager);
    }


    public VoxelLandscape(int numDetailLevels,
                          float mostDetailedBlockSizeMeters,
                          WorldFunction worldFunction,
                          Camera camera,
                          ChunkManager chunkManager) {

        Check.positive(numDetailLevels, "numDetailLevels");
        Check.positive(mostDetailedBlockSizeMeters, "mostDetailedBlockSizeMeters");
        notNull(worldFunction, "worldFunction");
        notNull(camera, "camera");
        notNull(chunkManager, "chunkManager");


        this.worldFunction = worldFunction;
        this.camera = camera;
        this.chunkManager = chunkManager;

        float chunkSizeMeters = mostDetailedBlockSizeMeters * Chunk.CHUNK_SIZE;

        float chunkSizeChange = (float)LAYER_SIZE / HOLE_SIZE;

        int chunksPerLowerDetailLevelChunk = LAYER_SIZE / HOLE_SIZE;
        int levelOfDetailMargin = chunksPerLowerDetailLevelChunk;


        // Create detail levels
        detailLevels = new DetailLevel[numDetailLevels];
        DetailLevel detailLevel = null;
        for (int i = 0; i < numDetailLevels; i++) {
            detailLevel = new DetailLevel(worldFunction,
                                          camera,
                                          chunkSizeMeters,
                                          chunkManager,
                                          LAYER_SIZE,
                                          i == 0 ? 0 : HOLE_SIZE,
                                          levelOfDetailMargin,
                                          MARGIN_SIZE,
                                          detailLevel,
                                          chunkMeshGenerator);
            detailLevels[i] = detailLevel;

            chunkSizeMeters *= chunkSizeChange;
        }
    }

    public void update(double secondsSinceLastCall) {
        for (int i = detailLevels.length - 1; i >= 0; i--) {
            detailLevels[i].update(secondsSinceLastCall);
        }
    }

    public void render(ModelBatch modelBatch, Environment environment) {
        for (DetailLevel detailLevel : detailLevels) {
            detailLevel.render(modelBatch, environment);
        }
    }

    public void dispose() {
        for (int i = 0; i < detailLevels.length; i++) {
            detailLevels[i].dispose();
            detailLevels[i] = null;

        }
    }
}
