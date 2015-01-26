package org.landscapelib;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;

/**
 * Holds data for a section of voxels.
 */
public final class Chunk {

    /**
     * Size of a chunk along each side.
     * Must be a power of two.
     */
    public final static int CHUNK_SIZE = 8;

    // NOTE: Update these as well if chunk size is changed:
    private final static int CHUNK_SIZE_MASK = CHUNK_SIZE - 1;
    private final static int CHUNK_SIZE_SHIFT = 3;

    public static final byte AIR_TYPE = 0;

    /**
     * Number of blocks in the chunk at most.
     */
    private final static int BLOCK_COUNT = CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE;

    private  Material blockMaterial = new Material(ColorAttribute.createDiffuse(Color.GREEN));
    private static final int BLOCK_ATTRIBUTES = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;

    private Vector3 center = new Vector3();

    private float chunkSizeInMeters = 1;

    private byte[] blockTypes = new byte[BLOCK_COUNT];

    private boolean changed = false;

    private ModelInstance modelInstance;


    public Chunk() {
        if ((this.hashCode() % 2) == 0) this.blockMaterial =  new Material(ColorAttribute.createDiffuse(Color.RED));
    }

    /**
     * @return Center location of this chunk in world coordinates.
     */
    public Vector3 getCenter() {
        return center;
    }

    /**
     * @param center Center location of this chunk in world coordinates.
     */
    public void setCenter(Vector3 center) {
        this.center.set(center);
    }

    public float getChunkSizeInMeters() {
        return chunkSizeInMeters;
    }

    public void setChunkSizeInMeters(float chunkSizeInMeters) {
        this.chunkSizeInMeters = chunkSizeInMeters;
    }

    /**
     * @return block type at the specified block coordinates inside this chunk (0 .. CHUNK_SIZE).
     *          If the block coordinates would be too large they are wrapped around to chunk size.
     */
    public byte getBlockType(int blockX, int blockY, int blockZ) {
        // Mask the block coordinates to ensure there is no overflows, without doing if-checks.
        // Shift y and z coordinates to correct bit position.  This works as chunk size is a power of two.
        final int index = (CHUNK_SIZE_MASK & blockX) |
                          ((CHUNK_SIZE_MASK & blockY) << CHUNK_SIZE_SHIFT) |
                          ((CHUNK_SIZE_MASK & blockZ) << (CHUNK_SIZE_SHIFT * 2));

        // Return block type at the specified position.
        return blockTypes[index];
    }

    /**
     * Updates the block type at the specified block coordinates inside this chunk (0 .. CHUNK_SIZE).
     * If the block coordinates would be too large they are wrapped around to chunk size.
     * @param blockType block type to set.
     */
    public void setBlockType(int blockX, int blockY, int blockZ, byte blockType) {
        // Mask the block coordinates to ensure there is no overflows, without doing if-checks.
        // Shift y and z coordinates to correct bit position.  This works as chunk size is a power of two.
        final int index = (CHUNK_SIZE_MASK & blockX) |
                          ((CHUNK_SIZE_MASK & blockY) << CHUNK_SIZE_SHIFT) |
                          ((CHUNK_SIZE_MASK & blockZ) << (CHUNK_SIZE_SHIFT * 2));

        blockTypes[index] = blockType;

        changed = true;
    }

    /**
     * @return true if this chunk has been modified.
     */
    public boolean isChanged() {
        return changed;
    }

    private void clearChanged() {
        changed = false;
    }


    public void generate(WorldFunction worldFunction) {
        double halfChunkSize = CHUNK_SIZE * 0.5;
        Vector3 v = new Vector3();
        double blockSize = chunkSizeInMeters / CHUNK_SIZE;

        for (int z = 0; z < CHUNK_SIZE; z++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                for (int x = 0; x < CHUNK_SIZE; x++) {

                    final int index = (CHUNK_SIZE_MASK & x) |
                                      ((CHUNK_SIZE_MASK & y) << CHUNK_SIZE_SHIFT) |
                                      ((CHUNK_SIZE_MASK & z) << (CHUNK_SIZE_SHIFT * 2));

                    getBlockCenter(v, x, y, z);

                    blockTypes[index] = worldFunction.getTerrainType(v, blockSize);
                }
            }
        }
    }

    public ModelInstance getModelInstance() {
        if (modelInstance == null) modelInstance = createModelInstance();

        return modelInstance;
    }

    private ModelInstance createModelInstance() {
        // Create model
        ModelBuilder modelBuilder = new ModelBuilder();

        /*
        Model model = modelBuilder.createBox(scale, scale, scale,
                                             BLOCK_MATERIAL,
                                             BLOCK_ATTRIBUTES);
                                             */

        modelBuilder.begin();
        final MeshPartBuilder meshBuilder = modelBuilder.part("chunk",
                                                              GL20.GL_TRIANGLES,
                                                              BLOCK_ATTRIBUTES,
                                                              blockMaterial);

        Vector3 blockPos = new Vector3();
        float blockSizeInMeters = chunkSizeInMeters / CHUNK_SIZE;

        for (int z = 0; z < CHUNK_SIZE; z++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                for (int x = 0; x < CHUNK_SIZE; x++) {

                    final int index = (CHUNK_SIZE_MASK & x) |
                                      ((CHUNK_SIZE_MASK & y) << CHUNK_SIZE_SHIFT) |
                                      ((CHUNK_SIZE_MASK & z) << (CHUNK_SIZE_SHIFT * 2));

                    byte blockType = blockTypes[index];

                    getRelativeBlockPos(blockPos, x, y, z);

                    if (blockType != AIR_TYPE) {
                        meshBuilder.box(blockPos.x, blockPos.y, blockPos.z,
                                        blockSizeInMeters, blockSizeInMeters, blockSizeInMeters);
                    }
                }
            }
        }


        final Model model = modelBuilder.end();


        // Create instance of model to render
        final ModelInstance modelInstance = new ModelInstance(model);

        // Position it
        modelInstance.transform.translate(center);

        return modelInstance;
    }

    public void render(ModelBatch modelBatch, Environment environment) {
        final ModelInstance modelInstance = getModelInstance();

        modelBatch.render(modelInstance, environment);
    }

    private void getBlockCenter(Vector3 centerOut, double x, double y, double z) {
        centerOut.set(center);
        centerOut.x += ((x / CHUNK_SIZE) - 0.5) * chunkSizeInMeters;
        centerOut.y += ((y / CHUNK_SIZE) - 0.5) * chunkSizeInMeters;
        centerOut.z += ((z / CHUNK_SIZE) - 0.5) * chunkSizeInMeters;
    }

    private void getRelativeBlockPos(Vector3 centerOut, double x, double y, double z) {
        centerOut.set(0,0,0);
        centerOut.x += ((x / CHUNK_SIZE) - 0.5) * chunkSizeInMeters;
        centerOut.y += ((y / CHUNK_SIZE) - 0.5) * chunkSizeInMeters;
        centerOut.z += ((z / CHUNK_SIZE) - 0.5) * chunkSizeInMeters;
    }
}
