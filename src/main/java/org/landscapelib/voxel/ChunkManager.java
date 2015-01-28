package org.landscapelib.voxel;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

/**
 *
 */
public class ChunkManager {

    private final Array<Chunk> chunks = new Array<Chunk>();

    private final WorldFunction worldFunction;

    private ModelBuilder modelBuilder = new ModelBuilder();

    private final Pool<Chunk> chunkPool = new Pool<Chunk>() {
        @Override protected Chunk newObject() {
            return new Chunk(modelBuilder);
        }
    };

    public ChunkManager(WorldFunction worldFunction) {
        this.worldFunction = worldFunction;
    }

    public Chunk generateChunk(Vector3 chunkCenter, float chunkSizeMeters) {
        // Get pooled chunk, if available.
        final Chunk chunk = chunkPool.obtain();

        chunk.initialize(chunkCenter, chunkSizeMeters, worldFunction);

        return chunk;
    }

    public void releaseChunk(Chunk chunkToRelease) {
        chunks.removeValue(chunkToRelease, true);
        chunkPool.free(chunkToRelease);
    }


    public void createTestStuff() {

        Vector3 origo = new Vector3();
        Vector3 v = new Vector3();

        float chunkSizeInMeters = 8;

        int start = -5;
        int end =  5;
        for (int z = start; z < end; z++) {
            for (int y = start; y < end; y++) {
                for (int x = start; x < end; x++) {

                    v.set(origo);
                    v.x += x * chunkSizeInMeters;
                    v.y += y * chunkSizeInMeters;
                    v.z += z * chunkSizeInMeters;

                    Chunk chunk = new Chunk(modelBuilder);

                    generateChunk(v, chunkSizeInMeters, chunk);

                    chunks.add(chunk);
                }
            }
        }

    }


    private void generateChunk(Vector3 center, float chunkSizeInMeters, Chunk chunk) {

        chunk.setCenter(center);
        chunk.setChunkSizeInMeters(chunkSizeInMeters);
        chunk.generate(worldFunction);
    }
}
