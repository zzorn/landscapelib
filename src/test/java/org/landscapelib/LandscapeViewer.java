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
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import org.landscapelib.voxel.ChunkManager;
import org.landscapelib.voxel.TestWorldFunction;
import org.landscapelib.voxel.VoxelLandscape;
import org.landscapelib.voxel.WorldFunction;

/**
 * Utility application for viewing and moving around in a landscape.
 */
public class LandscapeViewer implements ApplicationListener {

    private static final int VELOCITY_M_PER_SECOND = 1;
    public PerspectiveCamera cam;
    public ModelBatch modelBatch;
    public Model model;
    public ModelInstance instance;
    public Environment environment;
    public FirstPersonCameraController camController;

    private WorldFunction worldFunction;

    private ChunkManager chunkManager;

    private VoxelLandscape voxelLandscape;

    private static final int NUM_DETAIL_LEVELS = 10;
    private static final float MOST_DETAILED_BLOCK_SIZE_METERS = 0.1f;

    public void create () {

        // Create world
        worldFunction = new TestWorldFunction();

        // Setup model batching
        modelBatch = new ModelBatch();

        // Setup camera
        cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.position.set(0f, 0f, 0f);
        cam.lookAt(1,0,0);
        cam.near = 0.2f;
        cam.far = 100000f;
        cam.update();

        // Setup voxel landscape
        chunkManager = new ChunkManager(new TestWorldFunction());
        System.out.println("Starting chunk generation");
        voxelLandscape = new VoxelLandscape(NUM_DETAIL_LEVELS,
                                            MOST_DETAILED_BLOCK_SIZE_METERS, worldFunction, cam, chunkManager);
        System.out.println("Chunk generation done");


        // Create reference test model
        ModelBuilder modelBuilder = new ModelBuilder();
        model = modelBuilder.createBox(1, 1, 1,
                                       new Material(ColorAttribute.createDiffuse(Color.ORANGE)),
                                       VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);


        // Create instance of model to render
        instance = new ModelInstance(model);
        instance.transform.translate(0, 0, 0);

        // Setup lighting
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        // Setup camera control
        camController = createDebugCameraController();

        Gdx.input.setInputProcessor(camController);
    }

    private FirstPersonCameraController createDebugCameraController() {
        return new FirstPersonCameraController(cam) {
            private float degreesPerPixel = 0.5f;
            private Vector3 temp = new Vector3();
            private float accelerationMetersPerSecondPerSecond = 1;
            private float defaultSpeedMetersPerSecond = VELOCITY_M_PER_SECOND;
            private float maxVelocityMetersPerSecond = 1000;
            private float velocity = defaultSpeedMetersPerSecond;

            @Override public boolean touchDragged(int screenX, int screenY, int pointer) {
                float deltaX = -Gdx.input.getDeltaX() * degreesPerPixel;
                float deltaY = -Gdx.input.getDeltaY() * degreesPerPixel;

                // This fixes up to always be up on the screen
                cam.up.set(0, 1, 0);

                cam.direction.rotate(cam.up, deltaX);

                temp.set(cam.direction).crs(cam.up).nor();
                cam.direction.rotate(temp, deltaY);

                return true;

            }

            @Override public void update(float deltaTime) {
                Vector3 oldPos = cam.position.cpy();
                super.update(deltaTime);

                if (cam.position.dst2(oldPos) > 0.000001f) {
                    // We moved, accelerate while key held down
                    if (velocity < maxVelocityMetersPerSecond) {
                        velocity += accelerationMetersPerSecondPerSecond;
                    }
                }
                else {
                    // Remove acceleration
                    velocity = defaultSpeedMetersPerSecond;
                }
                setVelocity(velocity);

            }
        };
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
