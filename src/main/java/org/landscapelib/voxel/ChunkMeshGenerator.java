package org.landscapelib.voxel;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;

/**
 *
 */
public class ChunkMeshGenerator {

    private static final int VERTEXES_PER_SIDE = Chunk.CHUNK_SIZE + 1;
    private static final int MAX_VERTEXES = 8 // 8 possible neighboring blocks for each vertex
                                            * VERTEXES_PER_SIDE * VERTEXES_PER_SIDE * VERTEXES_PER_SIDE; // (chunk size + 1) ^ 3 vertexes.
    private static final int MAX_INDEXES = 3 // Triangles
                                           * 2 // 2 triangles per side
                                           * 6 // 6 sides per block
                                           * Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE; // Chunk size ^ 3 blocks

    private static final VertexAttributes VERTEX_ATTRIBUTES = new VertexAttributes(VertexAttribute.Position(),
                                                                                   VertexAttribute.Normal(),
                                                                                   VertexAttribute.ColorUnpacked());

    private static final int VERTEX_ATTRIBUTE_DATA_SIZE = VERTEX_ATTRIBUTES.vertexSize / 4; // Size in floats

    /**
     * Fraction that new chunk mesh size has to be under the previous one to create a new mesh (to free up unused memory from previously used exceptionally complicated chunks).
     */
    private static final float RECREATE_MESH_THRESHOLD = 0.1f;
    private static final boolean ALWAYS_RECREATE_MESH = false;

    private float[] vertexData = new float[VERTEX_ATTRIBUTE_DATA_SIZE * MAX_VERTEXES];
    private short[] indexData = new short[MAX_INDEXES];

    private short vertexCount = 0;
    private int indexCount = 0;

    public Mesh updateMesh(Chunk chunk, Mesh mesh) {

        generateMeshData(chunk);

        // Check if the data fits inside the old mesh, or if the old mesh is too large and should be recreated
        if (ALWAYS_RECREATE_MESH ||
            mesh == null ||
            vertexCount > mesh.getMaxVertices() ||
            indexCount > mesh.getMaxIndices() ||
            vertexCount < mesh.getMaxVertices() * RECREATE_MESH_THRESHOLD) {
            // Create new mesh
            mesh = new Mesh(ALWAYS_RECREATE_MESH, vertexCount, indexCount, VERTEX_ATTRIBUTES);
        }

        // Set the data
        mesh.setVertices(vertexData, 0, vertexCount * VERTEX_ATTRIBUTE_DATA_SIZE);
        mesh.setIndices(indexData, 0, indexCount);

        return mesh;
    }

    private void generateMeshData(Chunk chunk) {
        vertexCount = 0;
        indexCount = 0;

        float x0;
        float x1;
        float y0;
        float y1;
        float z0;
        float z1;

        float blockSize = chunk.getBlockSizeInMeters();
        float offs = -chunk.getChunkSizeInMeters() * 0.5f;

        final int chunkSize = Chunk.CHUNK_SIZE;

        // Fill up data
        for (int z = 0; z < chunkSize; z++) {
            for (int y = 0; y < chunkSize; y++) {
                for (int x = 0; x < chunkSize; x++) {
                    boolean solid = chunk.isSolid(x, y, z);

                    if (solid) {
                        // Determine corners
                        x0 = offs + x * blockSize;
                        x1 = x0 + blockSize;
                        y0 = offs + y * blockSize;
                        y1 = y0 + blockSize;
                        z0 = offs + z * blockSize;
                        z1 = z0 + blockSize;


                        // Determine what walls are needed
                        // A wall is needed if the neighboring block in some direction is non-solid or a chunk edge.
                        boolean wallAtx0 = x == 0 || !chunk.isSolid(x - 1, y, z);
                        boolean wallAtx1 = x == chunkSize - 1 || !chunk.isSolid(x + 1, y, z);
                        boolean wallAty0 = y == 0 || !chunk.isSolid(x, y - 1, z);
                        boolean wallAty1 = y == chunkSize - 1 || !chunk.isSolid(x, y + 1, z);
                        boolean wallAtz0 = z == 0 || !chunk.isSolid(x, y, z - 1);
                        boolean wallAtz1 = z == chunkSize - 1 || !chunk.isSolid(x, y, z + 1);

                        // Create walls
                        if (wallAtx0) addQuad(x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0, -1,  0,  0);
                        if (wallAtx1) addQuad(x1,y0,z0, x1,y1,z0, x1,y1,z1, x1,y0,z1,  1,  0,  0);
                        if (wallAty0) addQuad(x0,y0,z0, x1,y0,z0, x1,y0,z1, x0,y0,z1,  0, -1,  0);
                        if (wallAty1) addQuad(x0,y1,z0, x0,y1,z1, x1,y1,z1, x1,y1,z0,  0,  1,  0);
                        if (wallAtz0) addQuad(x0,y0,z0, x0,y1,z0, x1,y1,z0, x1,y0,z0,  0,  0, -1);
                        if (wallAtz1) addQuad(x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1,  0,  0,  1);

                    }
                }
            }
        }
    }

    private void addQuad(float x0,float y0,float z0,
                         float x1,float y1,float z1,
                         float x2,float y2,float z2,
                         float x3,float y3,float z3,
                         float xn,float yn,float zn) {
        addTriangle(x0,y0,z0,  x1,y1,z1,  x2,y2,z2,  xn,yn,zn);
        addTriangle(x2,y2,z2,  x3,y3,z3,  x0,y0,z0,  xn,yn,zn);
    }

    private void addTriangle(float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float xn, float yn, float zn) {

        short a = addVertex(x0, y0, z0,  xn, yn, zn);
        short b = addVertex(x1, y1, z1,  xn, yn, zn);
        short c = addVertex(x2, y2, z2,  xn, yn, zn);

        indexData[indexCount++] = a;
        indexData[indexCount++] = b;
        indexData[indexCount++] = c;
    }

    private short addVertex(float x, float y, float z,
                           float normalX, float normalY, float normalZ) {

        int offset = vertexCount * VERTEX_ATTRIBUTE_DATA_SIZE;

        // Position
        vertexData[offset++] = x;
        vertexData[offset++] = y;
        vertexData[offset++] = z;

        // Normal
        vertexData[offset++] = normalX;
        vertexData[offset++] = normalY;
        vertexData[offset++] = normalZ;

        // Color (unpacked)
        vertexData[offset++] = 1f;
        vertexData[offset++] = 1f;
        vertexData[offset++] = 1f;
        vertexData[offset++] = 1f;

        short addedVertexIndex = vertexCount;

        vertexCount++;

        return addedVertexIndex;
    }

}
