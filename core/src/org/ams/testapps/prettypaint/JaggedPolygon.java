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
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import org.ams.prettypaint.OutlinePolygon;
import org.ams.prettypaint.PrettyPolygonBatch;
import org.ams.prettypaint.TexturePolygon;

/**
 * This is a demo showing how to use PrettyPaint to draw a pretty polygon.
 */
public class JaggedPolygon extends ApplicationAdapter implements InputProcessor {

        // It is best to use a OrthographicCamera with PrettyPaint
        OrthographicCamera camera;

        // some variables used for controlling the camera
        Vector2 lastWorldTouchDown;
        Vector2 cameraPositionAtLastWorldTouch;


        PrettyPolygonBatch polygonBatch;

        TexturePolygon texturePolygon;
        Texture texture;

        OutlinePolygon shadowPolygon;
        OutlinePolygon outlinePolygon;

        float accumulator = 0;

        Array<Vector2> vertices = new Array<Vector2>();

        @Override
        public void dispose() {
                if (polygonBatch != null) polygonBatch.dispose();
                if (texture != null) texture.dispose();
        }

        @Override
        public void create() {
                Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                Gdx.input.setInputProcessor(this);

                float w = Gdx.graphics.getWidth();
                float h = Gdx.graphics.getHeight();
                camera = new OrthographicCamera(10, 10 * (h / w));

                // create the circleVertices used for all 3 polygons

                float radius = 2f;
                Vector2 v = new Vector2(radius, 0);
                float angle = MathUtils.PI2 / 50f;
                for (float rad = 0; rad <= MathUtils.PI2; rad += angle) {
                        v.rotateRad(angle);
                        vertices.add(new Vector2(v).scl(MathUtils.random(1f, 2f)));
                }


                polygonBatch = new PrettyPolygonBatch();


                outlinePolygon = new OutlinePolygon();
                outlinePolygon.setVertices(vertices);
                outlinePolygon.setColor(Color.BLACK);


                shadowPolygon = new OutlinePolygon();
                shadowPolygon.setDrawInside(false);
                shadowPolygon.setVertices(vertices);
                shadowPolygon.setColor(new Color(0, 0, 0, 0.4f));
                shadowPolygon.setHalfWidth(outlinePolygon.getHalfWidth() * 5);

                //shadowPolygon.setDrawFrustum(batch,true);
                //shadowPolygon.setDrawCullingRectangles(true);
                //shadowPolygon.setDrawTriangleStrips(false);
                //shadowPolygon.setDrawLineFromFirstToLast(true);


                texture = new Texture("contemporary_china.png");
                texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

                texturePolygon = new TexturePolygon();
                texturePolygon.setTextureRegion(new TextureRegion(texture));
                texturePolygon.setVertices(vertices);


        }

        @Override
        public void render() {
                Gdx.gl20.glClearColor(1, 1, 1, 1);
                Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);


                accumulator += Gdx.graphics.getDeltaTime() * 0.1f;
                float sin = MathUtils.sin(accumulator % MathUtils.PI);

                float scale = Interpolation.fade.apply(sin);
                texturePolygon.setScale(scale);
                shadowPolygon.setScale(scale);
                outlinePolygon.setScale(scale);

                float angleRad = Interpolation.fade.apply(sin) * MathUtils.PI2 * 2;
                texturePolygon.setAngle(angleRad);
                shadowPolygon.setAngle(angleRad);
                outlinePolygon.setAngle(angleRad);

                float opacity = sin;
                texturePolygon.setOpacity(opacity);
                shadowPolygon.setOpacity(opacity);
                outlinePolygon.setOpacity(opacity);


                polygonBatch.begin(camera);
                texturePolygon.draw(polygonBatch);
                shadowPolygon.draw(polygonBatch);
                outlinePolygon.draw(polygonBatch);
                polygonBatch.end();
        }


        // Everything below here is just for controlling the camera

        @Override
        public void resize(int width, int height) {

                camera.setToOrtho(false, 10, 10 * ((float) height / (float) width));
                camera.position.x = 0;
                camera.position.y = 0;

                camera.update();
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

                return true;
        }

        @Override
        public boolean scrolled(int amount) {
                setZoom(camera.zoom * (1 + amount * 0.1f), Gdx.input.getX(), Gdx.input.getY());

                return true;
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
