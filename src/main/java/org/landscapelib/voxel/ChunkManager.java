package org.landscapelib.voxel;

import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

/**
 *
 */
// TODO: Add executor, chunk density calculation tasks,
// TODO   and a step to voxel landscape where visibility of chunks is updated based on whether more detailed chunks have finished calculating their density
// TODO: Also determine if a chunk is at the edge of the visible area when a LOD is moved/generated, if so, update the mesh to merge with a lower lod along the appropriate sides (store lower lod sides in a byte as bit flags?)
// TODO: Do not generate solid surfaces along chunk edges.  Marching cubes should help with that.
public class ChunkManager {

    private final Array<Chunk> chunks = new Array<Chunk>();

    private final WorldFunction worldFunction;

    private final Pool<Chunk> chunkPool = new Pool<Chunk>(100, 10000) {
        @Override protected Chunk newObject() {
            return new Chunk();
        }

        @Override public void free(Chunk chunk) {
            int previousNumberOfPooledChunks = getFree();

            super.free(chunk);

            if (getFree() == previousNumberOfPooledChunks) {
                // The chunk was not pooled, dispose it
                chunk.dispose();
            }
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
