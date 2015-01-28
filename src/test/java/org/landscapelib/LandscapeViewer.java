package org.landscapelib;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import org.landscapelib.voxel.ChunkManager;
import org.landscapelib.voxel.TestWorldFunction;
import org.landscapelib.voxel.VoxelLandscape;
import org.landscapelib.voxel.WorldFunction;

/**
 * Utility application for viewing and moving around in a landscape.
 */
public class LandscapeViewer implements ApplicationListener {

    public PerspectiveCamera cam;
    public ModelBatch modelBatch;
    public Model model;
    public ModelInstance instance;
    public Environment environment;
    public CameraInputController camController;

    private WorldFunction worldFunction;

    private ChunkManager chunkManager;

    private VoxelLandscape voxelLandscape;

    public void create () {

        // Create world
        worldFunction = new TestWorldFunction();

        // Setup model batching
        modelBatch = new ModelBatch();

        // Setup camera
        cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.position.set(10f, 10f, 10f);
        cam.lookAt(0,0,0);
        cam.near = 1f;
        cam.far = 300f;
        cam.update();

        // Setup voxel landscape
        chunkManager = new ChunkManager(new TestWorldFunction());
        System.out.println("Starting chunk generation");
        final int mostDetailedChunkSizeMeters = 10;
        final int numDetailLevels = 5;
        voxelLandscape = new VoxelLandscape(numDetailLevels, mostDetailedChunkSizeMeters, worldFunction, cam, chunkManager);
        System.out.println("Chunk generation done");


        // Create test model
        ModelBuilder modelBuilder = new ModelBuilder();
        model = modelBuilder.createBox(1, 1, 1,
                                       new Material(ColorAttribute.createDiffuse(Color.ORANGE)),
                                       VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);


        // Create instance of model to render
        instance = new ModelInstance(model);
        instance.transform.translate(22, 22, 22);

        // Setup lighting
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        // Setup camera control
        camController = new CameraInputController(cam);
        Gdx.input.setInputProcessor(camController);
    }

    public void render () {
        // Update from input
        camController.update();

        voxelLandscape.update(Gdx.graphics.getDeltaTime());

        // Clear screen
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        // Render scene
        modelBatch.begin(cam);

        voxelLandscape.render(modelBatch, environment);

        modelBatch.render(instance, environment);

        modelBatch.end();
    }

    public void resize (int width, int height) {
    }

    public void pause () {
    }

    public void resume () {
    }

    public void dispose () {
        modelBatch.dispose();
        model.dispose();
        voxelLandscape.dispose();
    }


    public static void main(String[] args) {

        new LwjglApplication(new LandscapeViewer(), "LandscapeViewer", 800, 600);

    }

}
