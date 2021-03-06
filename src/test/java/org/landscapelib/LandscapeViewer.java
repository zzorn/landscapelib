package org.landscapelib;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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

    private static final int VELOCITY_M_PER_SECOND = 10;
    public PerspectiveCamera cam;
    public ModelBatch modelBatch;
    public Model model;
    public ModelInstance instance;
    public Environment environment;
    public FirstPersonCameraController camController;

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
        cam.position.set(0f, 0f, 0f);
        cam.lookAt(1,0,0);
        cam.near = 0.2f;
        cam.far = 100000f;
        cam.update();

        // Setup voxel landscape
        chunkManager = new ChunkManager(new TestWorldFunction());
        System.out.println("Starting chunk generation");
        voxelLandscape = new VoxelLandscape(worldFunction, cam, chunkManager);
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
            private Vector3 upwards = new Vector3(0,1,0);
            private float accelerationMetersPerSecondPerSecond = 2;
            private float defaultSpeedMetersPerSecond = VELOCITY_M_PER_SECOND;
            private float maxVelocityMetersPerSecond = 1000;
            private float velocity = defaultSpeedMetersPerSecond;

            private boolean turboBoost = false;
            private static final int TURBO_BOOST_KEY = Input.Keys.SHIFT_LEFT;

            @Override public boolean keyDown(int keycode) {
                if (keycode == TURBO_BOOST_KEY) {
                    if (!turboBoost) {
                        velocity += 100;
                    }
                    turboBoost = true;
                }

                return super.keyDown(keycode);
            }

            @Override public boolean keyUp(int keycode) {
                if (keycode == TURBO_BOOST_KEY) {
                    if (turboBoost) {
                        velocity -= 100;
                        if (velocity < 0) velocity = 0;
                    }
                    turboBoost = false;
                }

                return super.keyUp(keycode);
            }

            @Override public boolean touchDragged(int screenX, int screenY, int pointer) {
                float deltaX = -Gdx.input.getDeltaX() * degreesPerPixel;
                float deltaY = -Gdx.input.getDeltaY() * degreesPerPixel;

                // This fixes up to always be away from planet center
                cam.up.set(upwards);

                cam.direction.rotate(cam.up, deltaX);

                temp.set(cam.direction).crs(cam.up).nor();
                cam.direction.rotate(temp, deltaY);

                return true;

            }

            @Override public void update(float deltaTime) {

                // Upwards vector is away from gracitation center.
                upwards.set(cam.position).sub(worldFunction.getGravitationCenter()).nor();

                Vector3 oldPos = cam.position.cpy();
                super.update(deltaTime);

                if (cam.position.dst2(oldPos) > 0.000001f) {
                    // We moved, accelerate while key held down
                    if (velocity < maxVelocityMetersPerSecond && turboBoost) {
                        velocity += accelerationMetersPerSecondPerSecond * deltaTime;
                        velocity *= 1 + 0.2f * deltaTime;
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
