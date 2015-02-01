package org.landscapelib.voxel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Pool;


/**
 * Holds data for a section of voxels.
 */
// TODO: Add method to trigger regeneration of a chunk when the world data has changed for the area it is in.  The VoxelLandscape could listen to the world data and update the relevant chunks as needed.
public final class Chunk implements Pool.Poolable, WorldGenerationListener {

    /**
     * Size of a chunk along each side.
     * Must be a power of two.
     */
    public final static int CHUNK_SIZE = 8;
    // NOTE: Update this as well if chunk size is changed:
    private final static int CHUNK_SIZE_SHIFT = 3;

    private final static int CHUNK_SIZE_MASK = CHUNK_SIZE - 1;

    /**
     * Number of blocks in the chunk at most.
     */
    private final static int BLOCK_COUNT = CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE;

    private static final Material DEFAULT_MATERIAL = new Material(ColorAttribute.createDiffuse(Color.GREEN));

    private static final ModelBuilder MODEL_BUILDER = new ModelBuilder();

    private Vector3 center = new Vector3();
    private float chunkSizeInMeters = 1;

    private byte[] primaryMaterial = new byte[BLOCK_COUNT];
    private byte[] secondaryMaterial = new byte[BLOCK_COUNT];

    /**
     * Relative amounts of the primary and secondary materials. 255 = all primary material, 0 = 50-50% distribution.
     */
    private byte[] materialRatio = new byte[BLOCK_COUNT];

    /**
     * How much of the block that is filled with matter.  0 = nothing, 255 = completely filled.
     */
    private byte[] volume = new byte[BLOCK_COUNT];

    private boolean modelNeedsRegeneration = true;
    private ModelInstance modelInstance;
    private Mesh mesh;
    private Material blockMaterial = DEFAULT_MATERIAL;

    private boolean allSolid;
    private boolean allAir;

    private boolean calculationOngoing = false;
    private boolean cancelCalulation = false;

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
     * @return true if the block at the specified block coordinates inside this chunk (0 .. CHUNK_SIZE) is solid.
     *          If the block coordinates would be too large they are wrapped around to chunk size.
     */
    public boolean isSolid(int blockX, int blockY, int blockZ) {
        return volume[calculateBlockIndex(blockX, blockY, blockZ)] != 0;
    }

    /**
     * @return most abundant material type at the specified block coordinates inside this chunk (0 .. CHUNK_SIZE).
     *          If the block coordinates would be too large they are wrapped around to chunk size.
     */
    public byte getPrimaryMaterial(int blockX, int blockY, int blockZ) {
        return primaryMaterial[calculateBlockIndex(blockX, blockY, blockZ)];
    }

    /**
     * @return second most abundant material type at the specified block coordinates inside this chunk (0 .. CHUNK_SIZE).
     *          If the block coordinates would be too large they are wrapped around to chunk size.
     */
    public byte getSecondaryMaterial(int blockX, int blockY, int blockZ) {
        return secondaryMaterial[calculateBlockIndex(blockX, blockY, blockZ)];
    }

    /**
     * @return distribution between primary and secondary materials (255 = 100% primary material, 0 = 50% primary material)
     *         at the specified block coordinates inside this chunk (0 .. CHUNK_SIZE).
     *          If the block coordinates would be too large they are wrapped around to chunk size.
     */
    public byte getMaterialRatio(int blockX, int blockY, int blockZ) {
        return materialRatio[calculateBlockIndex(blockX, blockY, blockZ)];
    }

    /**
     * @return volume of material at the specified block coordinates inside this chunk (0 .. CHUNK_SIZE).
     *         0 = no material, 255 = 100% of the block filled with materials.
     *          If the block coordinates would be too large they are wrapped around to chunk size.
     */
    public byte getVolume(int blockX, int blockY, int blockZ) {
        return volume[calculateBlockIndex(blockX, blockY, blockZ)];
    }

    /**
     * Updates the material at the specified block coordinates inside this chunk (0 .. CHUNK_SIZE).
     * If the block coordinates would be too large they are wrapped around to chunk size.
     * @deprecated Modifications should be done directly to the worldModel instead of here.  This is not really useful for anything
     */
    private void setMaterial(int blockX, int blockY, int blockZ, byte primaryMaterial, byte secondaryMaterial, byte materialRatio, byte volume) {
        final int index = calculateBlockIndex(blockX, blockY, blockZ);
        this.primaryMaterial[index] = primaryMaterial;
        this.secondaryMaterial[index] = secondaryMaterial;
        this.materialRatio[index] = materialRatio;
        this.volume[index] = volume;

        modelNeedsRegeneration = true;
    }

    /**
     * @return true if this chunk has been modified and the model should be regenerated.
     */
    public boolean isModelNeedsRegeneration() {
        return modelNeedsRegeneration;
    }

    public void calculateDensityData(WorldFunction worldFunction) {

        // TODO: If already ongoing, wait for previous one to stop then recalc?
        calculationOngoing = true;

        final double blockSize = chunkSizeInMeters / CHUNK_SIZE;

        // TODO: Execute the calculation on a separate thread with a task executor
        worldFunction.calculateChunk(primaryMaterial, secondaryMaterial, materialRatio, volume,
                                     center.x, center.y, center.z,
                                     CHUNK_SIZE,
                                     blockSize,
                                     this);

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
        cancelCalulation = true;
    }

    /**
     * @return true if this chunk can be rendered.
     */
    public boolean isReadyToRender() {
        return !calculationOngoing;
    }

    public void render(ModelBatch modelBatch,
                       Environment environment,
                       ChunkMeshGenerator chunkMeshGenerator) {

        // If calculation of the terrain data is still ongoing, we can't render
        if (!calculationOngoing) {
            modelBatch.render(getModelInstance(chunkMeshGenerator), environment);
        }
    }

    @Override public boolean calculationProgress(float progressZeroToOne) {
        // Request interrupt of calculation if this chunk moved out of range already (== reset has been called)
        return !cancelCalulation;
    }

    @Override public void calculationReady() {
        calculationOngoing = false;

        updateAllSolidity();

        // Request regeneration of the visual model
        modelNeedsRegeneration = true;
    }

    @Override public void calculationAborted() {
        calculationOngoing = false;
        cancelCalulation = false;

        // TODO: If the chunk has been initialized again, but a calculation was ongoing, start a new calculation with the new location here.
    }

    private void updateAllSolidity() {
        // Determine if all blocks in a chunk are solid or air
        allSolid = true;
        allAir = true;
        for (int i = 0; i < BLOCK_COUNT; i++) {
            if (volume[i] == 0) {
                allSolid = false;

                // No need to loop the rest of the blocks if we already determined that not all blocks were air as well
                if (!allAir) break;
            }
            else {
                allAir = false;

                // No need to loop the rest of the blocks if we already determined that not all blocks were solid as well
                if (!allSolid) break;
            }
        }
    }

    private void getChunkLocalBlockCenter(Vector3 centerOut, int chunkX, int chunkY, int chunkZ) {
        final float blockSizeInMeters = chunkSizeInMeters / CHUNK_SIZE;
        final float centerOffset = 0.5f * chunkSizeInMeters;
        centerOut.x = (0.5f + chunkX) * blockSizeInMeters - centerOffset;
        centerOut.y = (0.5f + chunkY) * blockSizeInMeters - centerOffset;
        centerOut.z = (0.5f + chunkZ) * blockSizeInMeters - centerOffset;
    }

    public static int calculateBlockIndex(int blockX, int blockY, int blockZ) {
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
