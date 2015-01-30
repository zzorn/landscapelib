package org.landscapelib.voxel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Pool;


/**
 * Holds data for a section of voxels.
 */
public final class Chunk implements Pool.Poolable{

    /**
     * Size of a chunk along each side.
     * Must be a power of two.
     */
    public final static int CHUNK_SIZE = 8;
    // NOTE: Update this as well if chunk size is changed:
    private final static int CHUNK_SIZE_SHIFT = 3;

    private final static int CHUNK_SIZE_MASK = CHUNK_SIZE - 1;

    public static final byte AIR_TYPE = 0;

    /**
     * Number of blocks in the chunk at most.
     */
    private final static int BLOCK_COUNT = CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE;

    private static final Material DEFAULT_MATERIAL = new Material(ColorAttribute.createDiffuse(Color.GREEN));

    private static final int BLOCK_ATTRIBUTES = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;

    private static final ModelBuilder MODEL_BUILDER = new ModelBuilder();

    private Vector3 center = new Vector3();
    private float chunkSizeInMeters = 1;
    private byte[] blockTypes = new byte[BLOCK_COUNT];
    private boolean modelNeedsRegeneration = true;
    private ModelInstance modelInstance;
    private Mesh mesh;
    private Material blockMaterial = DEFAULT_MATERIAL;

    private boolean allSolid;
    private boolean allAir;

    public Chunk() {
    }

    public void setDebugColor(Color color) {
        blockMaterial = new Material(ColorAttribute.createDiffuse(color));
        modelNeedsRegeneration = true;
    }

    /**
     * Initializes a chunk and generates the data for it.
     *
     * @param center center of the chunk
     * @param chunkSizeInMeters size of the whole chunk along each side, in world units.
     * @param worldFunction landscape function to use for generating the chunk.
     */
    public void initialize(Vector3 center,
                           float chunkSizeInMeters,
                           WorldFunction worldFunction) {
        setCenter(center);
        setChunkSizeInMeters(chunkSizeInMeters);

        // Get density data
        calculateDensityData(worldFunction);
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

    public float getBlockSizeInMeters() {
        return chunkSizeInMeters / CHUNK_SIZE;
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

    public void calculateDensityData(WorldFunction worldFunction) {
        Vector3 blockPos = new Vector3();
        double blockSize = chunkSizeInMeters / CHUNK_SIZE;

        allSolid = true;
        allAir = true;

        for (int z = 0; z < CHUNK_SIZE; z++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                for (int x = 0; x < CHUNK_SIZE; x++) {

                    getChunkLocalBlockCenter(blockPos, x, y, z);

                    blockPos.add(center);

                    final int index = calculateBlockIndex(x, y, z);
                    final byte terrainType = worldFunction.getTerrainType(blockPos, blockSize);
                    blockTypes[index] = terrainType;

                    // Determine if all blocks in a chunk are solid or air.
                    if (terrainType == AIR_TYPE) {
                        allSolid = false;
                    }
                    else {
                        allAir = false;
                    }
                }
            }
        }

        modelNeedsRegeneration = true;
    }

    public boolean isAllSolid() {
        return allSolid;
    }

    public boolean isAllAir() {
        return allAir;
    }

    public ModelInstance getModelInstance(ChunkMeshGenerator chunkMeshGenerator) {
        if (modelInstance == null || modelNeedsRegeneration) {
            modelInstance = generateMesh(chunkMeshGenerator);
            modelNeedsRegeneration = false;
        }

        return modelInstance;
    }

    private ModelInstance generateMesh(ChunkMeshGenerator chunkMeshGenerator) {

        // Generate mesh for this chunk
        Mesh newMesh = chunkMeshGenerator.updateMesh(this, mesh);

        // Dispose old mesh if a new mesh was generated instead of updating an old one
        if (newMesh != mesh && mesh != null) {
            mesh.dispose();
        }

        mesh = newMesh;

        // We create a new model and model instance each time,
        // as it seems to be complicated to just update the mesh and material of a model.
        return createModelInstance(mesh);
    }

    private ModelInstance createModelInstance(final Mesh mesh) {

        // Create Model from mesh and material
        MODEL_BUILDER.begin();
        MODEL_BUILDER.part("chunk", mesh, GL20.GL_TRIANGLES, blockMaterial);
        final Model model = MODEL_BUILDER.end();

        // Create model instance
        ModelInstance modelInstance = new ModelInstance(model);

        // Update position
        modelInstance.transform.setTranslation(center);

        return modelInstance;
    }


    @Override public void reset() {
        modelNeedsRegeneration = true;
    }

    public void render(ModelBatch modelBatch,
                       Environment environment,
                       ChunkMeshGenerator chunkMeshGenerator) {
        final ModelInstance modelInstance = getModelInstance(chunkMeshGenerator);

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
        if (modelInstance != null && modelInstance.model != null) {
            modelInstance.model.dispose();
            modelInstance = null;
        }
    }
}
