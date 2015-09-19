package org.landscapelib;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;

/**
 */
public interface LandscapeRenderer {

    void update(double secondsSinceLastCall);

    void render(ModelBatch modelBatch, Environment environment);

}
