package org.landscapelib.voxel;

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

    private final Pool<Chunk> chunkPool = new Pool<Chunk>(100, 200) {
        @Override protected Chunk newObject() {
            return new Chunk();
        }
    };

    public ChunkManager(WorldFunction worldFunction) {
        this.worldFunction = worldFunction;
    }

    public Chunk generateChunk(Vector3 chunkCenter, float chunkSizeMeters) {
        // Get pooled chunk, if available.
        final Chunk chunk = chunkPool.obtain();

        chunk.initialize(chunkCenter, chunkSizeMeters, worldFunction);

        //System.out.println("ChunkManager.generateChunk");
        //System.out.println("pooled chunks = " + chunkPool.getFree());

        return chunk;
    }

    public void releaseChunk(Chunk chunkToRelease) {
        chunks.removeValue(chunkToRelease, true);
        chunkPool.free(chunkToRelease);

        //System.out.println("ChunkManager.releaseChunk");
    }

}
