package org.landscapelib.voxel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Pool;

import static org.flowutils.Check.notNull;

/**
 * Holds data for a section of voxels.
 */
public final class Chunk implements Pool.Poolable{

    /**
     * Size of a chunk along each side.
     * Must be a power of two.
     */
    public final static int CHUNK_SIZE = 8;

    private final static float RELATIVE_BLOCK_SIZE = 1f / CHUNK_SIZE;

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

    private boolean modelNeedsRegeneration = false;

    private ModelInstance modelInstance;
    private final ModelBuilder modelBuilder;
    private Model model;

    private Matrix4 tempMatrix = new Matrix4();


    public Chunk(ModelBuilder modelBuilder) {
        notNull(modelBuilder, "modelBuilder");

        this.modelBuilder = modelBuilder;

        // TODO Temporary debug material, replace
        if ((this.hashCode() % 2) == 0) this.blockMaterial =  new Material(ColorAttribute.createDiffuse(Color.RED));
    }

    /**
     * Initializes a chunk and generates the data for it.
     *
     * @param center center of the chunk
     * @param chunkSizeInMeters size of the whole chunk along each side, in world units.
     * @param worldFunction landscape function to use for generating the chunk.
     */
    public void initialize(Vector3 center, float chunkSizeInMeters, WorldFunction worldFunction) {
        setCenter(center);
        setChunkSizeInMeters(chunkSizeInMeters);
        generate(worldFunction);
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
        // Return block type at the specified position.
        return blockTypes[calculateBlockIndex(blockX, blockY, blockZ)];
    }

    /**
     * @return true if the block at the specified block coordinates inside this chunk (0 .. CHUNK_SIZE) is solid.
     *          If the block coordinates would be too large they are wrapped around to chunk size.
     */
    public boolean isSolid(int blockX, int blockY, int blockZ) {
        return blockTypes[calculateBlockIndex(blockX, blockY, blockZ)] != AIR_TYPE;
    }

    /**
     * Updates the block type at the specified block coordinates inside this chunk (0 .. CHUNK_SIZE).
     * If the block coordinates would be too large they are wrapped around to chunk size.
     * @param blockType block type to set.
     */
    public void setBlockType(int blockX, int blockY, int blockZ, byte blockType) {
        blockTypes[calculateBlockIndex(blockX, blockY, blockZ)] = blockType;

        modelNeedsRegeneration = true;
    }

    /**
     * @return true if this chunk has been modified and the model should be regenerated.
     */
    public boolean isModelNeedsRegeneration() {
        return modelNeedsRegeneration;
    }

    public void generate(WorldFunction worldFunction) {
        Vector3 blockPos = new Vector3();
        double blockSize = chunkSizeInMeters / CHUNK_SIZE;

        for (int z = 0; z < CHUNK_SIZE; z++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                for (int x = 0; x < CHUNK_SIZE; x++) {

                    getChunkLocalBlockCenter(blockPos, x, y, z);

                    blockPos.add(center);

                    final int index = calculateBlockIndex(x, y, z);
                    blockTypes[index] = worldFunction.getTerrainType(blockPos, blockSize);
                }
            }
        }

        modelNeedsRegeneration = true;
    }


    public ModelInstance getModelInstance() {
        if (modelInstance == null || modelNeedsRegeneration) {
            modelInstance = createModelInstance();
            modelNeedsRegeneration = false;
        }

        return modelInstance;
    }

    @Override public void reset() {
        clearOldModel();
    }

    private ModelInstance createModelInstance() {

        clearOldModel();

        // Create model

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
        float blockSizeInMeters = (float) chunkSizeInMeters / CHUNK_SIZE;

        for (int z = 0; z < CHUNK_SIZE; z++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                for (int x = 0; x < CHUNK_SIZE; x++) {

                    final int index = calculateBlockIndex(x, y, z);

                    byte blockType = blockTypes[index];

                    getChunkLocalBlockCenter(blockPos, x, y, z);

                    if (blockType != AIR_TYPE) {

                        // Optimization: if all neighbour blocks in the chunk are solid, there is no need to render this.
                        // If the block is at an chunk edge, we need to render it.
                        if (x == 0 || x == CHUNK_SIZE - 1 ||
                            y == 0 || y == CHUNK_SIZE - 1 ||
                            z == 0 || z == CHUNK_SIZE - 1 ||
                            !isSolid(x-1, y, z) ||
                            !isSolid(x+1, y, z) ||
                            !isSolid(x, y-1, z) ||
                            !isSolid(x, y+1, z) ||
                            !isSolid(x, y, z-1) ||
                            !isSolid(x, y, z+1)) {

                            meshBuilder.box(blockPos.x, blockPos.y, blockPos.z,
                                            blockSizeInMeters, blockSizeInMeters, blockSizeInMeters);
                        }
                    }
                }
            }
        }


        model = modelBuilder.end();


        // Create instance of model to render
        final ModelInstance modelInstance = new ModelInstance(model);

        // Position it
        modelInstance.transform.translate(center);

        return modelInstance;
    }

    private void clearOldModel() {
        if (model != null) {
            model.dispose();
            model = null;
        }

        modelNeedsRegeneration = true;
    }

    public void render(ModelBatch modelBatch, Environment environment, Vector3 offset) {
        final ModelInstance modelInstance = getModelInstance();

        //tempMatrix.set(modelInstance.transform);

        //modelInstance.transform.translate(-offset.x, -offset.y, -offset.z);

        modelBatch.render(modelInstance, environment);

        //modelInstance.transform.set(tempMatrix);

    }

    private void getChunkLocalBlockCenter(Vector3 centerOut, int chunkX, int chunkY, int chunkZ) {
        final float blockSizeInMeters = chunkSizeInMeters / CHUNK_SIZE;
        final float centerOffset = 0.5f * chunkSizeInMeters;
        centerOut.x = (0.5f + chunkX) * blockSizeInMeters - centerOffset;
        centerOut.y = (0.5f + chunkY) * blockSizeInMeters - centerOffset;
        centerOut.z = (0.5f + chunkZ) * blockSizeInMeters - centerOffset;
    }

    private int calculateBlockIndex(int blockX, int blockY, int blockZ) {
        // Mask the block coordinates to ensure there is no overflows, without doing if-checks.
        // Shift y and z coordinates to correct bit position.  This works as chunk size is a power of two.
        return (CHUNK_SIZE_MASK & blockX) |
               ((CHUNK_SIZE_MASK & blockY) << CHUNK_SIZE_SHIFT) |
               ((CHUNK_SIZE_MASK & blockZ) << (CHUNK_SIZE_SHIFT * 2));
    }

    public void dispose() {
        model.dispose();
    }
}
