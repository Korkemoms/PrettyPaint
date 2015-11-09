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
package org.ams.testapps.paintandphysics;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.Array;
import org.ams.paintandphysics.things.PPCircle;
import org.ams.paintandphysics.things.PPPolygon;
import org.ams.paintandphysics.world.PPWorld;
import org.ams.physics.things.Circle;
import org.ams.physics.things.Polygon;
import org.ams.physics.things.def.CircleDef;
import org.ams.physics.things.def.PolygonDef;
import org.ams.prettypaint.OutlinePolygon;
import org.ams.prettypaint.PrettyPolygonBatch;
import org.ams.prettypaint.TexturePolygon;

/**
 * This is a demo showing how to use PrettyPaint to draw a pretty polygon.
 */
public class SaveThingAsPng extends ApplicationAdapter implements InputProcessor {

        // It is best to use a OrthographicCamera with PrettyPaint
        OrthographicCamera camera;

        // some variables used for controlling the camera
        Vector2 lastWorldTouchDown;
        Vector2 cameraPositionAtLastWorldTouch;

        PrettyPolygonBatch polygonBatch;
        PPWorld world;

        Texture texture;

        PPPolygon square;
        PPCircle circle;


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

                polygonBatch = new PrettyPolygonBatch();

                world = new PPWorld();

                texture = new Texture("skulls.png");
                texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);


                square = addSquare(texture);
                circle = addCircle(texture);

        }

        private PPCircle addCircle(Texture texture) {
                // make a moving circle with texture and outlines
                PPCircle circle = new PPCircle();

                // add box2d circle
                CircleDef def = new CircleDef();
                circle.setPhysicsThing(new Circle(def));

                // add texture
                TexturePolygon texturePolygon = new TexturePolygon();
                texturePolygon.setTextureRegion(new TextureRegion(texture));
                circle.setTexturePolygon(texturePolygon);

                // add outline
                OutlinePolygon outlinePolygon = new OutlinePolygon();
                circle.getOutlinePolygons().add(outlinePolygon);

                // set properties of all 3
                float radius = 0.25f;

                circle.setRadius(radius);
                circle.setVertexCount(30);
                circle.setPosition(0, 5);

                world.addThing(circle);
                return circle;
        }

        private PPPolygon addSquare(Texture texture) {
                // make a moving square with texture and outlines
                PPPolygon square = new PPPolygon();

                // add box2d polygon
                PolygonDef def = new PolygonDef();
                def.type = BodyDef.BodyType.StaticBody;
                square.setPhysicsThing(new Polygon(def));

                // add texture
                TexturePolygon texturePolygon = new TexturePolygon();
                texturePolygon.setTextureRegion(new TextureRegion(texture));
                square.setTexturePolygon(texturePolygon);

                // add outline
                OutlinePolygon outlinePolygon = new OutlinePolygon();
                square.getOutlinePolygons().add(outlinePolygon);

                // set properties of all 3
                float hw = 0.25f;
                float hh = 0.25f;

                Array<Vector2> vertices = new Array<Vector2>();
                vertices.add(new Vector2(-hw, -hh));
                vertices.add(new Vector2(hw, -hh));
                vertices.add(new Vector2(hw, hh));
                vertices.add(new Vector2(-hw, hh));

                square.setVertices(vertices);
                square.setPosition(0, 0);

                world.addThing(square);
                return square;
        }

        int i = 0;

        @Override
        public void render() {
                Gdx.gl20.glClearColor(1, 1, 1, 1);
                Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
                float delta = Gdx.graphics.getDeltaTime();


                polygonBatch.begin(camera);

                world.step(delta);
                world.draw(polygonBatch);


                polygonBatch.end();

                if (i++ ==4) {
                        square.saveAsPng(polygonBatch, "test.png", 1f);
                        circle.saveAsPng(polygonBatch, "test.png", 1f);

                }


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
