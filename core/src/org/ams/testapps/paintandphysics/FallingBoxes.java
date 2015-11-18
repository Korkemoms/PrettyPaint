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
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.Array;
import org.ams.core.CameraNavigator;
import org.ams.core.Util;
import org.ams.paintandphysics.things.PPCircle;
import org.ams.paintandphysics.things.PPPolygon;
import org.ams.paintandphysics.world.PPWorld;
import org.ams.physics.things.Circle;
import org.ams.physics.things.Polygon;
import org.ams.physics.things.def.CircleDef;
import org.ams.physics.things.def.PolygonDef;
import org.ams.physics.tools.BodyMover;
import org.ams.prettypaint.OutlinePolygon;
import org.ams.prettypaint.PrettyPolygonBatch;
import org.ams.prettypaint.TexturePolygon;

/**
 * This is a demo showing how to use PrettyPaint to draw a pretty polygon.
 */
public class FallingBoxes extends ApplicationAdapter {

        // It is best to use a OrthographicCamera with PrettyPaint
        OrthographicCamera camera;

        CameraNavigator cameraNavigator;
        BodyMover bodyMover;

        PrettyPolygonBatch polygonBatch;
        PPWorld world;

        float accumulator;

        Texture texture;


        @Override
        public void dispose() {
                if (polygonBatch != null) polygonBatch.dispose();
                if (texture != null) texture.dispose();
        }

        @Override
        public void create() {
                Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

                float w = Gdx.graphics.getWidth();
                float h = Gdx.graphics.getHeight();
                camera = new OrthographicCamera(10, 10 * (h / w));
                cameraNavigator = new CameraNavigator(camera);

                polygonBatch = new PrettyPolygonBatch();

                world = new PPWorld();

                bodyMover = new BodyMover(world.boxWorld, camera);

                InputMultiplexer inputMultiplexer = new InputMultiplexer();
                inputMultiplexer.addProcessor(bodyMover);
                inputMultiplexer.addProcessor(cameraNavigator);

                Gdx.input.setInputProcessor(inputMultiplexer);

                texture = new Texture("images/for packing/backgrounds-light/giftly.png");
                texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

                addGround(texture);

        }

        @Override
        public void render() {
                Gdx.gl20.glClearColor(1, 1, 1, 1);
                Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
                float delta = Gdx.graphics.getDeltaTime();

                polygonBatch.begin(camera);
                world.step(delta);
                world.draw(polygonBatch);

                accumulator += delta;

                if (accumulator > 1) {
                        accumulator -= 1;
                        addSquare(texture);
                }

                polygonBatch.end();

        }

        private void addGround(Texture texture) {
                PPPolygon ground = new PPPolygon();

                // add box2d polygon
                PolygonDef def = new PolygonDef();
                def.type = BodyDef.BodyType.StaticBody;
                ground.setPhysicsThing(new Polygon(def));

                // add texture
                TexturePolygon texturePolygon = new TexturePolygon();
                texturePolygon.setTextureRegion(new TextureRegion(texture));
                ground.setTexturePolygon(texturePolygon);

                // add outline
                OutlinePolygon outlinePolygon = new OutlinePolygon();
                ground.getOutlinePolygons().add(outlinePolygon);

                // add shadow
                OutlinePolygon shadowPolygon = new OutlinePolygon();
                shadowPolygon.setColor(new Color(0, 0, 0, 0.35f));
                shadowPolygon.setHalfWidth(0.2f);
                shadowPolygon.setDrawInside(false);
                ground.getOutlinePolygons().add(shadowPolygon);

                // set properties of all 4
                float hw = 12f;
                float hh = 3f;

                Array<Vector2> vertices = new Array<Vector2>();
                vertices.add(new Vector2(-hw, -hh));
                vertices.add(new Vector2(hw, -hh));
                vertices.add(new Vector2(hw, hh));
                vertices.add(new Vector2(-hw, hh));

                ground.setVertices(vertices);

                world.addThing(ground);

                ground.setPosition(0, -4f);
        }

        private void addSquare(Texture texture) {
                // make a moving square with texture and outlines
                PPPolygon square = new PPPolygon();

                // add box2d polygon
                PolygonDef def = new PolygonDef();
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
                square.setPosition(0, 10);

                world.addThing(square);
        }

        @Override
        public void resize(int width, int height) {

                camera.setToOrtho(false, 10, 10 * ((float) height / (float) width));
                camera.position.x = 0;
                camera.position.y = 0;

                camera.update();
        }
}
