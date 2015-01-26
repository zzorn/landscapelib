package org.landscapelib;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

/**
 *
 */
public class ChunkManager {

    private final Array<Chunk> chunks = new Array<Chunk>();

    private final WorldFunction worldFunction;

    public ChunkManager(WorldFunction worldFunction) {
        this.worldFunction = worldFunction;
    }

    public void createTestStuff() {

        Vector3 origo = new Vector3();
        Vector3 v = new Vector3();

        float chunkSizeInMeters = 8;

        int start = -3;
        int end =  3;
        for (int z = start; z < end; z++) {
            for (int y = start; y < end; y++) {
                for (int x = start; x < end; x++) {

                    v.set(origo);
                    v.x += x * chunkSizeInMeters;
                    v.y += y * chunkSizeInMeters;
                    v.z += z * chunkSizeInMeters;

                    Chunk chunk = new Chunk();

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


    public void render(ModelBatch modelBatch, Environment environment) {
        for (Chunk chunk : chunks) {
            chunk.render(modelBatch, environment);
        }
    }
}
