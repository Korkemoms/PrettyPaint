/*
 *
 *  The MIT License (MIT)
 *
 *  Copyright (c) <2015> <Andreas Modahl>
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 */
package org.ams.testapps.prettypaint;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import org.ams.prettypaint.OutlinePolygon;
import org.ams.prettypaint.PrettyPolygonBatch;
import org.ams.prettypaint.TexturePolygon;

/**
 * This demo shows a spinning circle and a background.
 */
public class CircleAndBackground extends ApplicationAdapter implements InputProcessor {

        // It is best to use a OrthographicCamera with PrettyPaint
        OrthographicCamera camera;

        // some variables used for controlling the camera
        Vector2 lastWorldTouchDown;
        Vector2 cameraPositionAtLastWorldTouch;

        PrettyPolygonBatch batch;


        TexturePolygon background;
        Texture texture;


        OutlinePolygon shadowPolygon;
        OutlinePolygon outlinePolygon;

        float arcAccumulator = 0;
        float rotationAccumulator = 0;

        Array<Vector2> circleVertices = new Array<Vector2>();

        @Override
        public void dispose() {
                if (batch != null) batch.dispose();
                if (texture != null) texture.dispose();

        }

        @Override
        public void create() {
                Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                Gdx.input.setInputProcessor(this);

                float w = Gdx.graphics.getWidth() * 0.5f;
                float h = Gdx.graphics.getHeight() * 0.5f;
                camera = new OrthographicCamera(10, 10 * (h / w));

                batch = new PrettyPolygonBatch();


                // create the vertices used for the circle
                float radius = 2f;
                Vector2 v = new Vector2(radius, 0);
                float angle = MathUtils.PI2 / 300;
                for (float rad = 0; rad <= MathUtils.PI2; rad += angle) {
                        v.rotateRad(angle);
                        circleVertices.add(new Vector2(v));
                }

                // prepare circle
                outlinePolygon = new OutlinePolygon();
                outlinePolygon.setColor(Color.BLACK);
                outlinePolygon.setClosedPolygon(false);

                // prepare shadow around circle
                shadowPolygon = new OutlinePolygon();
                shadowPolygon.setColor(new Color(0, 0, 0, 0.4f));
                shadowPolygon.setHalfWidth(outlinePolygon.getHalfWidth() * 3);
                shadowPolygon.setClosedPolygon(false);


                // prepare the vertices for background and frame
                float hw = camera.viewportWidth * 0.5f;
                float hh = camera.viewportHeight * 0.5f;

                Array<Vector2> cameraBounds = new Array<Vector2>();
                cameraBounds.add(new Vector2(-hw, -hh));
                cameraBounds.add(new Vector2(hw, -hh));
                cameraBounds.add(new Vector2(hw, hh));
                cameraBounds.add(new Vector2(-hw, hh));

                // prepare background
                texture = new Texture("images/for packing/backgrounds-dark/escheresque_ste.png");
                texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

                background = new TexturePolygon();
                background.setTextureRegion(new TextureRegion(texture));
                background.setScale(0.96f);
                background.setVertices(cameraBounds);

        }

        @Override
        public void render() {
                float delta = Gdx.graphics.getDeltaTime();

                // swing the length of the arc
                arcAccumulator += delta;
                float sin = MathUtils.sin(arcAccumulator % MathUtils.PI);
                int n = (int) (circleVertices.size * sin * 0.5f);

                while (outlinePolygon.getVertexCount() > n) {
                        outlinePolygon.removeVertex(outlinePolygon.getVertexCount() - 1);
                        shadowPolygon.removeVertex(shadowPolygon.getVertexCount() - 1);
                }

                while (outlinePolygon.getVertexCount() < n) {
                        Vector2 vertex = circleVertices.get(outlinePolygon.getVertexCount() % circleVertices.size);
                        outlinePolygon.addVertex(vertex);
                        shadowPolygon.addVertex(vertex);
                }


                // rotate the whole thing
                sin = MathUtils.sin((arcAccumulator + MathUtils.PI * 0.5f) % MathUtils.PI);
                rotationAccumulator += 1 * sin * delta;

                outlinePolygon.setAngle(rotationAccumulator * MathUtils.PI2);
                shadowPolygon.setAngle(rotationAccumulator * MathUtils.PI2);

                // draw
                Gdx.gl20.glClearColor(1, 1, 1, 1);
                Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
                batch.begin(camera);

                background.draw(batch);

                shadowPolygon.draw(batch);
                outlinePolygon.draw(batch);

                batch.end();
        }

        private void updateBackgroundPositionAndZoom() {
                float x = camera.position.x;
                float y = camera.position.y;

                background.setPosition(x, y);
                background.setTextureTranslation(x * 0.1f, y * 0.1f);


                float scale = camera.zoom;
                background.setScale(scale);
        }

        @Override
        public void resize(int width, int height) {

                camera.setToOrtho(false, 10, 10 * ((float) height / (float) width));
                camera.position.x = 0;
                camera.position.y = 0;

                camera.update();

                // update the vertices for background
                float hw = camera.viewportWidth * 0.5f;
                float hh = camera.viewportHeight * 0.5f;

                Array<Vector2> cameraBounds = new Array<Vector2>();
                cameraBounds.add(new Vector2(-hw, -hh));
                cameraBounds.add(new Vector2(hw, -hh));
                cameraBounds.add(new Vector2(hw, hh));
                cameraBounds.add(new Vector2(-hw, hh));

                background.setVertices(cameraBounds);

                updateBackgroundPositionAndZoom();

        }


        @Override
        public boolean touchDragged(int screenX, int screenY, int pointer) {
                if (cameraPositionAtLastWorldTouch == null)
                        return false;

                if (lastWorldTouchDown == null)
                        return false;

                Vector2 worldTouchDrag = getPositionOfTouch(screenX, screenY);

                Vector2 pos = new Vector2(cameraPositionAtLastWorldTouch).sub(worldTouchDrag).add(lastWorldTouchDown);

                camera.position.set(pos.x, pos.y, camera.position.z);
                camera.update();

                updateBackgroundPositionAndZoom();
                return true;
        }

        @Override
        public boolean scrolled(int amount) {
                setZoom(camera.zoom * (1 + amount * 0.1f), Gdx.input.getX(), Gdx.input.getY());

                updateBackgroundPositionAndZoom();
                return true;
        }


        @Override
        public boolean keyDown(int keycode) {
                return false;
        }

        @Override
        public boolean keyUp(int keycode) {
                return false;
        }

        @Override
        public boolean keyTyped(char character) {
                return false;
        }

        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {

                lastWorldTouchDown = getPositionOfTouch(screenX, screenY);
                cameraPositionAtLastWorldTouch = new Vector2(camera.position.x, camera.position.y);
                return true;
        }

        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) {

                return false;
        }


        /**
         * @param zoom    the cameras zoom will be set to this value.
         * @param screenX the screen x-coordinate you want to zoom in on(or away from).
         * @param screenY the screen y-coordinate you want to zoom in on(or away from).
         */
        public void setZoom(float zoom, int screenX, int screenY) {
                Vector2 touchWorldBefore = getPositionOfTouch(screenX, screenY);
                touchWorldBefore.add(camera.position.x, camera.position.y);

                camera.zoom = zoom;
                camera.update();

                Vector2 touchWorldAfter = getPositionOfTouch(screenX, screenY);
                touchWorldAfter.add(camera.position.x, camera.position.y);

                camera.translate(touchWorldBefore.sub(touchWorldAfter));
                camera.update();


        }

        /**
         * @param screenX pixel.
         * @param screenY pixel.
         * @return position of touch in "world units", with center of screen as origin.
         */
        public Vector2 getPositionOfTouch(int screenX, int screenY) {

                float halfWidth = Gdx.graphics.getWidth() * 0.5f;
                float halfHeight = Gdx.graphics.getHeight() * 0.5f;

                Vector2 pos = new Vector2(screenX - halfWidth, halfHeight - screenY);
                pos.scl(1f / halfWidth, 1f / halfHeight);

                pos.scl(camera.zoom);

                // convert to world units relative to center of screen
                pos.scl(camera.viewportWidth * 0.5f, camera.viewportHeight * 0.5f);

                return pos;
        }

        @Override
        public boolean mouseMoved(int screenX, int screenY) {
                return false;
        }

}
