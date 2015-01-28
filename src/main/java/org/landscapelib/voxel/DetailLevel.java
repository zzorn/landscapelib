package org.landscapelib.voxel;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.Vector3;
import org.flowutils.Check;

import static org.flowutils.Check.notNull;

/**
 *
 */
public class DetailLevel {

    private final Chunk[] chunks;
    private final Chunk[] tempChunks;

    private final WorldFunction worldFunction;

    private final Camera camera;
    private final float chunkSizeMeters;
    private final ChunkManager chunkManager;

    private long centerChunkX;
    private long centerChunkY;
    private long centerChunkZ;

    private final Vector3 center = new Vector3();

    private final int holeSize;
    private final int layerSize;
    private final int cacheMargin;
    private final int storageSize;

    /**
     * @param worldFunction function used to generate the world.
     * @param camera camera to use as the center.
     * @param chunkSizeMeters size of chunks in this detail level in meters.
     * @param chunkManager manager used to generate and release chunks.
     * @param layerSize visible size of this detail level along the edges, in number of chunks.  Includes eventual hole size.
     * @param holeSize size of hole left in the middle of this detail layer for higher detail layers,
     * @param cacheMargin size of the margin around the visible area potentially containing cached chunks, in number of chunks.
     */
    public DetailLevel(WorldFunction worldFunction,
                       Camera camera,
                       float chunkSizeMeters,
                       ChunkManager chunkManager,
                       int layerSize,
                       int holeSize,
                       int cacheMargin) {
        notNull(worldFunction, "worldFunction");
        notNull(camera, "camera");
        notNull(chunkManager, "chunkManager");
        Check.positive(chunkSizeMeters, "chunkSizeMeters");
        Check.positive(layerSize, "layerSize");
        Check.positiveOrZero(holeSize, "holeSize");
        Check.positiveOrZero(cacheMargin, "cacheMargin");
        Check.greater(layerSize, "layerSize", holeSize, "holeSize");

        this.worldFunction = worldFunction;
        this.camera = camera;
        this.chunkSizeMeters = chunkSizeMeters;
        this.chunkManager = chunkManager;
        this.holeSize = holeSize;
        this.layerSize = layerSize;
        this.cacheMargin = cacheMargin;

        storageSize = layerSize + 2 * cacheMargin;
        chunks = new Chunk[storageSize * storageSize * storageSize];
        tempChunks = new Chunk[storageSize * storageSize * storageSize];

        // Initialize position
        final Vector3 cameraPos = camera.position;
        centerChunkX = worldPosToChunk(cameraPos.x);
        centerChunkY = worldPosToChunk(cameraPos.y);
        centerChunkZ = worldPosToChunk(cameraPos.z);
        getChunkCenter(layerSize / 2, layerSize / 2, layerSize / 2, center);

        generateMissingChunks();
    }

    public void update(double secondsSinceLastUpdate) {

        final Vector3 cameraPos = camera.position;
        long cameraChunkX = worldPosToChunk(cameraPos.x);
        long cameraChunkY = worldPosToChunk(cameraPos.y);
        long cameraChunkZ = worldPosToChunk(cameraPos.z);

        // Check if the camera moved to another chunk
        if (cameraChunkX != centerChunkX ||
            cameraChunkY != centerChunkY ||
            cameraChunkZ != centerChunkZ) {

            // Center on the new chunk, move existing chunks, release cached chunks that scroll out, and generate new visible chunks that scroll in
            long deltaX = cameraChunkX - centerChunkX;
            long deltaY = cameraChunkY - centerChunkY;
            long deltaZ = cameraChunkZ - centerChunkZ;
            centerChunkX = cameraChunkX;
            centerChunkY = cameraChunkY;
            centerChunkZ = cameraChunkZ;
            getChunkCenter(layerSize / 2, layerSize / 2, layerSize / 2, center);

            moveChunks(deltaX, deltaY, deltaZ);

            generateMissingChunks();
        }
    }

    public void render(ModelBatch modelBatch, Environment environment) {

        for (int z = cacheMargin; z < storageSize - cacheMargin; z++) {
            for (int y = cacheMargin; y < storageSize - cacheMargin; y++) {
                for (int x = cacheMargin; x < storageSize - cacheMargin; x++) {
                    if (isVisible(x, y, z)) {
                        getChunk(x, y, z).render(modelBatch, environment, center);
                    }
                }
            }
        }
    }

    private boolean isVisible(int x, int y, int z) {
        if (x < cacheMargin || x >= storageSize - cacheMargin ||
            y < cacheMargin || y >= storageSize - cacheMargin ||
            z < cacheMargin || z >= storageSize - cacheMargin) {
            return false;
        }

        int half = storageSize / 2;
        if (x > half - holeSize && x <= half + holeSize &&
            y > half - holeSize && y <= half + holeSize &&
            z > half - holeSize && z <= half + holeSize) {
            return false;
        }

        return true;
    }


    private void moveChunks(long deltaX, long deltaY, long deltaZ) {
        // If we moved too much, just clear all chunks
        if (Math.abs(deltaX) >= storageSize ||
            Math.abs(deltaY) >= storageSize ||
            Math.abs(deltaZ) >= storageSize) {
            for (int i = 0; i < chunks.length; i++) {
                chunks[i] = null;
            }
        }
        else {
            // We use a temporary array when copying chunks, it's not optimal but it's not a significant memory hit and makes things simpler
            for (int z = 0; z < storageSize; z++) {
                for (int y = 0; y < storageSize; y++) {
                    for (int x = 0; x < storageSize; x++) {

                        // Get source to move
                        final int sourceChunkIndex = getChunkIndex(x + (int) deltaX,
                                                                   y + (int) deltaY,
                                                                   z + (int) deltaZ);

                        if (sourceChunkIndex >= 0) {
                            // Move it
                            tempChunks[getChunkIndex(x, y, z)] = chunks[sourceChunkIndex];

                            // Clear the source so that we know it was moved
                            chunks[sourceChunkIndex] = null;
                        }
                    }
                }
            }

            // Release any unmoved blocks
            for (Chunk chunk : chunks) {
                if (chunk != null) {
                    chunkManager.releaseChunk(chunk);
                }
            }

            // Copy results over
            System.arraycopy(tempChunks, 0, chunks, 0, chunks.length);

            // Clear temp array
            for (int i = 0; i < tempChunks.length; i++) {
                tempChunks[i] = null;
            }
        }
    }

    /**
     * @return index for the chunk with the specified coordinates, or -1 if the coordinates are outside the chunk array.
     */
    private int getChunkIndex(int x, int y, int z) {
        if (x < 0 || x >= storageSize ||
            y < 0 || y >= storageSize ||
            z < 0 || z >= storageSize) {
            return -1;
        }
        else {
            return x + y * storageSize + z * storageSize * storageSize;
        }
    }

    /**
     * @return the chunk with the specified coordinates, or null if the coordinates are outside the chunk array.
     */
    private Chunk getChunk(int x, int y, int z) {
        if (x < 0 || x >= storageSize ||
            y < 0 || y >= storageSize ||
            z < 0 || z >= storageSize) {
            return null;
        }
        else {
            return chunks[x + y * storageSize + z * storageSize * storageSize];
        }
    }

    private void generateMissingChunks() {
        // TODO: This could be done in threads in the background maybe?  Ideally in that case we should keep showing a lower detail
        // chunk until all the higher detail chunks in its area have been geneated.  Also generate chunks from the center out.

        Vector3 chunkCenter = new Vector3();

        for (int z = cacheMargin; z < storageSize - cacheMargin; z++) {
            for (int y = cacheMargin; y < storageSize - cacheMargin; y++) {
                for (int x = cacheMargin; x < storageSize - cacheMargin; x++) {
                    final int chunkIndex = getChunkIndex(x, y, z);
                    if (chunkIndex > 0 &&
                        chunks[chunkIndex] == null) {

                        // Generate new chunk if we didn't have any at this location
                        getChunkCenter(x, y, z, chunkCenter);
                        chunks[chunkIndex] = chunkManager.generateChunk(chunkCenter, chunkSizeMeters);
                    }

                }
            }
        }
    }

    private void getChunkCenter(int x, int y, int z, Vector3 chunkCenterOut) {
        chunkCenterOut.x = (centerChunkX + x - storageSize / 2) * chunkSizeMeters;
        chunkCenterOut.y = (centerChunkY + y - storageSize / 2) * chunkSizeMeters;
        chunkCenterOut.z = (centerChunkZ + z - storageSize / 2) * chunkSizeMeters;
    }

    private long worldPosToChunk(final float v) {
        return (long) Math.floor(v / chunkSizeMeters);
    }


    public void dispose() {

        for (Chunk chunk : chunks) {
            chunk.dispose();
        }

        for (int i = 0; i < chunks.length; i++) {
            chunks[i] = null;
        }


    }
}
