package org.landscapelib.voxel;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
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
    private final DetailLevel higherDetailLevel;

    private long centerChunkX;
    private long centerChunkY;
    private long centerChunkZ;

    private final Vector3 center = new Vector3();

    private final int holeSize;
    private final int layerSize;
    private final int levelOfDetailMargin;
    private final int cacheMargin;
    private final int storageSize;

    private Color debugColor1;
    private Color debugColor2;

    private boolean showDebugColor = true;

    private Vector3 holeStart = new Vector3();
    private Vector3 holeEnd = new Vector3();
    private Vector3 boundingVolumeStart;
    private Vector3 boundingVolumeEnd;

    private Vector3 temp = new Vector3();

    private final ChunkMeshGenerator chunkMeshGenerator;


    /**
     * @param worldFunction function used to generate the world.
     * @param camera camera to use as the center.
     * @param chunkSizeMeters size of chunks in this detail level in meters.
     * @param chunkManager manager used to generate and release chunks.
     * @param layerSize visible size of this detail level along the edges, in number of chunks.  Includes eventual hole size.
     * @param holeSize size of hole left in the middle of this detail layer for higher detail layers,
     * @param cacheMargin size of the margin around the visible area potentially containing cached chunks, in number of chunks.
     * @param chunkMeshGenerator used to create meshes for chunks.
     */
    public DetailLevel(WorldFunction worldFunction,
                       Camera camera,
                       float chunkSizeMeters,
                       ChunkManager chunkManager,
                       int layerSize,
                       int holeSize,
                       int levelOfDetailMargin,
                       int cacheMargin,
                       DetailLevel higherDetailLevel,
                       ChunkMeshGenerator chunkMeshGenerator) {
        notNull(worldFunction, "worldFunction");
        notNull(camera, "camera");
        notNull(chunkManager, "chunkManager");
        notNull(chunkMeshGenerator, "chunkMeshGenerator");

        Check.positive(chunkSizeMeters, "chunkSizeMeters");
        Check.positive(layerSize, "layerSize");
        Check.positiveOrZero(holeSize, "holeSize");
        Check.positiveOrZero(cacheMargin, "cacheMargin");
        Check.positiveOrZero(levelOfDetailMargin, "levelOfDetailMargin");
        Check.greater(layerSize, "layerSize", holeSize, "holeSize");

        this.chunkMeshGenerator = chunkMeshGenerator;
        this.higherDetailLevel = higherDetailLevel;
        this.worldFunction = worldFunction;
        this.camera = camera;
        this.chunkSizeMeters = chunkSizeMeters;
        this.chunkManager = chunkManager;
        this.holeSize = holeSize;
        this.layerSize = layerSize;
        this.levelOfDetailMargin = levelOfDetailMargin;
        this.cacheMargin = cacheMargin;

        storageSize = layerSize + 2 * cacheMargin + 2 * levelOfDetailMargin;
        chunks = new Chunk[storageSize * storageSize * storageSize];
        tempChunks = new Chunk[storageSize * storageSize * storageSize];

        debugColor1 = new Color(1 - ((chunkSizeMeters) / (chunkSizeMeters + 10f)),
                                0.5f,
                                (chunkSizeMeters) / (chunkSizeMeters + 100f), 1f) ;
        debugColor2 = debugColor1.cpy().lerp(Color.WHITE, 0.25f);

        // Initialize position
        final Vector3 cameraPos = camera.position;
        setCenter(cameraPos);

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

            setCenter(cameraPos);

            updateHoleExtent();

            moveChunks(deltaX, deltaY, deltaZ);

            generateMissingChunks();
        }
    }

    private void updateHoleExtent() {
        if (higherDetailLevel != null) {
            // Notify higher detail level of the area where it can draw itself

            int holeStartChunk = getHoleStartChunk();
            getChunkCenter(holeStartChunk, holeStartChunk, holeStartChunk, holeStart);
            holeStart.sub(chunkSizeMeters * 0.5f);

            int holeEndChunk = getHoleEndChunk();
            getChunkCenter(holeEndChunk, holeEndChunk, holeEndChunk, holeEnd);
            holeEnd.add(chunkSizeMeters * 0.5f);

            higherDetailLevel.setBoundingVolume(holeStart, holeEnd);
        }
    }

    private void setBoundingVolume(Vector3 boundingVolumeStart, Vector3 boundingVolumeEnd) {
        this.boundingVolumeStart = boundingVolumeStart;
        this.boundingVolumeEnd = boundingVolumeEnd;
    }

    public void render(ModelBatch modelBatch, Environment environment) {

        for (int z = 0; z < storageSize; z++) {
            for (int y = 0; y < storageSize; y++) {
                for (int x = 0; x < storageSize; x++) {
                    if (isVisible(x, y, z)) {
                        final Chunk chunk = getChunk(x, y, z);
                        // Do not render all-air chunks
                        if (chunk != null && !chunk.isAllAir()) {
                            // Do not render solid chunks that are surrounded by solid chunks on all sides
                            if (!isSolidChunkSurroundedBySolidChunks(z, y, x, chunk)) {
                                chunk.render(modelBatch, environment, chunkMeshGenerator);
                            }
                        }

                    }
                }
            }
        }
    }

    private boolean isSolidChunkSurroundedBySolidChunks(int z, int y, int x, Chunk chunk) {
        return chunk.isAllSolid() &&
            isAllSolidChunk(x-1, y, z) &&
            isAllSolidChunk(x+1, y, z) &&
            isAllSolidChunk(x, y-1, z) &&
            isAllSolidChunk(x, y+1, z) &&
            isAllSolidChunk(x, y, z-1) &&
            isAllSolidChunk(x, y, z+1);
    }

    private boolean isVisible(int x, int y, int z) {
        // Check which edges overlap the lower detail level and leave them out
        if (boundingVolumeStart != null && boundingVolumeEnd != null) {
            getChunkCenter(x, y, z, temp);
            if (temp.x < boundingVolumeStart.x || temp.x >= boundingVolumeEnd.x ||
                temp.y < boundingVolumeStart.y || temp.y >= boundingVolumeEnd.y ||
                temp.z < boundingVolumeStart.z || temp.z >= boundingVolumeEnd.z) {
                return false;
            }
        }

        // Don't render any chunks that should be rendered by a higher detail level
        if (isInHole(x, y, z)) return false;

        return true;
    }

    private boolean isInHole(int x, int y, int z) {
        if (holeSize > 0) {
            getChunkCenter(x, y, z, temp);
            if (temp.x >= holeStart.x && temp.x < holeEnd.x &&
                temp.y >= holeStart.y && temp.y < holeEnd.y &&
                temp.z >= holeStart.z && temp.z < holeEnd.z) {
                return true;
            }

            /*
            int holeStart = getHoleStartChunk();
            int holeEnd = getHoleEndChunk();
            if (x >= holeStart && x < holeEnd &&
                y >= holeStart && y < holeEnd &&
                z >= holeStart && z < holeEnd) {
                return true;
            }
            */
        }
        return false;
    }

    private int getHoleStartChunk() {
        int center = storageSize / 2;
        return center - holeSize / 2;
    }

    private int getHoleEndChunk() {
        return getHoleStartChunk() + holeSize;
    }

    private void moveChunks(long deltaX, long deltaY, long deltaZ) {
        // If we moved too much, just clear all chunks
        if (Math.abs(deltaX) >= storageSize ||
            Math.abs(deltaY) >= storageSize ||
            Math.abs(deltaZ) >= storageSize) {
            clearAllChunks();
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
            for (int i = 0; i < chunks.length; i++) {
                chunks[i] = tempChunks[i];
            }

            // Clear temp array
            for (int i = 0; i < tempChunks.length; i++) {
                tempChunks[i] = null;
            }
        }
    }


    private void clearAllChunks() {
        for (int i = 0; i < chunks.length; i++) {
            if (chunks[i] != null) {
                chunkManager.releaseChunk(chunks[i]);
            }
            chunks[i] = null;
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

    /**
     * @return true if the specified chunk is all solid, or is outside the storage range.
     */
    private boolean isAllSolidChunk(int x, int y, int z) {
        if (x < 0 || x >= storageSize ||
            y < 0 || y >= storageSize ||
            z < 0 || z >= storageSize) {
            return true;
        }
        else {
            final Chunk chunk = getChunk(x, y, z);
            if (chunk == null) {
                return true;
            }
            else {
                return chunk.isAllSolid();
            }
        }
    }

    private void generateMissingChunks() {
        // TODO: This could be done in threads in the background maybe?  Ideally in that case we should keep showing a lower detail
        // chunk until all the higher detail chunks in its area have been geneated.  Also generate chunks from the center out.

        Vector3 chunkCenter = new Vector3();

        for (int z = 0; z < storageSize; z++) {
            for (int y = 0; y < storageSize; y++) {
                for (int x = 0; x < storageSize; x++) {
                    final int chunkIndex = getChunkIndex(x, y, z);
                    if (chunkIndex > 0 &&
                        chunks[chunkIndex] == null &&
                        isVisible(x, y, z)) {

                        // Generate new chunk if we didn't have any at this location
                        getChunkCenter(x, y, z, chunkCenter);
                        final Chunk newChunk = chunkManager.generateChunk(chunkCenter, chunkSizeMeters);
                        if (showDebugColor) {
                            // != is identical to xor when applied to booleans.
                            Color color = (isEven(x) != (isEven(y))) != (isEven(z)) ? debugColor1 : debugColor2;
                            newChunk.setDebugColor(color);
                        }

                        chunks[chunkIndex] = newChunk;
                    }

                }
            }
        }
    }

    private void setCenter(Vector3 pos) {
        centerChunkX = worldPosToChunk(pos.x);
        centerChunkY = worldPosToChunk(pos.y);
        centerChunkZ = worldPosToChunk(pos.z);

        getChunkCenter(storageSize / 2,
                       storageSize / 2,
                       storageSize / 2,
                       center);
    }

    private void getChunkCenter(int x, int y, int z, Vector3 chunkCenterOut) {
        chunkCenterOut.x = (0.5f + centerChunkX + x - storageSize / 2) * chunkSizeMeters;
        chunkCenterOut.y = (0.5f + centerChunkY + y - storageSize / 2) * chunkSizeMeters;
        chunkCenterOut.z = (0.5f + centerChunkZ + z - storageSize / 2) * chunkSizeMeters;
    }

    private long worldPosToChunk(final float v) {
        float value = v / chunkSizeMeters;
        long chunkCoordinate = value < 0.0f ? (long)(value - 1) : (long) value;
        return chunkCoordinate;

        /*
        float offset = isEven(holeSize) ? chunkSizeMeters : chunkSizeMeters*0.5f;
        return (long) Math.floor((v + offset) / chunkSizeMeters);
        */
    }

    private boolean isEven(int x) {
        return (x % 2) == 0;
    }


    public void dispose() {

        for (Chunk chunk : chunks) {
            if (chunk != null) {
                chunk.dispose();
            }
        }

        for (int i = 0; i < chunks.length; i++) {
            chunks[i] = null;
        }


    }
}
