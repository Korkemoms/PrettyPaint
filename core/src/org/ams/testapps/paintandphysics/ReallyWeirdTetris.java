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
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.Array;
import org.ams.core.CameraNavigator;
import org.ams.core.Util;
import org.ams.paintandphysics.things.PPPolygon;
import org.ams.paintandphysics.world.PPWorld;
import org.ams.physics.things.Polygon;
import org.ams.physics.things.def.PolygonDef;
import org.ams.physics.tools.BodyMover;
import org.ams.prettypaint.*;

/**
 * This is a demo showing how to use PrettyPaint to draw a pretty polygon.
 */
public class ReallyWeirdTetris extends ApplicationAdapter {

        // It is best to use a OrthographicCamera with PrettyPaint
        OrthographicCamera camera;

        CameraNavigator cameraNavigator;
        BodyMover bodyMover;

        PrettyPolygonBatch polygonBatch;
        PPWorld world;

        float accumulator;

        float dim = 0.5f;

        Float[] longBlock = new Float[]{
                0f, 0f,
                dim, 0f,
                dim, dim * 4f,
                0f, dim * 4f
        };

        Float[] wallVertices = new Float[]{
                -4f, 0.5f,
                -4f, -0.5f,
                4f, -0.5f,
                4f, 0.5f
        };


        Array<Float[]> blocks = new Array<Float[]>();

        Texture texture;

        PPPolygon ground;


        PPPolygon rightWall;

        PPPolygon leftWall;


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

                texture = new Texture("mainme2.png");
                texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);


                ground = addWall(texture, wallVertices);
                ground.setPosition(4, 1.5f);
                ground.setAngle(MathUtils.PI*0.5f);


                rightWall = addWall(texture, wallVertices);
                rightWall.setPosition(2.3f, 1);

                leftWall = addWall(texture, wallVertices);
                leftWall.setAngle(MathUtils.PI * 0.5f);
                leftWall.setPosition(-4, 0);


                Array<OutlinePolygon> toMerge = new Array<OutlinePolygon>();
                toMerge.addAll(ground.getOutlinePolygons());
                toMerge.addAll(rightWall.getOutlinePolygons());
                //toMerge.addAll(leftWall.getOutlinePolygons());

                //new OutlineMerger().mergeOutlines(toMerge);


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

                if ( accumulator > 1) {
                        accumulator -= 1;
                       // addBlock(texture, longBlock);



                        Array<TexturePolygon> toAlign = new Array<TexturePolygon>();
                        toAlign.add(ground.getTexturePolygon());
                        toAlign.add(rightWall.getTexturePolygon());

                        new TextureAligner().alignTextures(toAlign, true);
                }

                polygonBatch.end();

        }

        private PPPolygon addWall(Texture texture, Float... vertices) {
                PPPolygon wall = new PPPolygon();

                // add box2d polygon
                PolygonDef def = new PolygonDef();
                def.type = BodyDef.BodyType.StaticBody;
                wall.setPhysicsThing(new Polygon(def));

                // add texture
                TexturePolygon texturePolygon = new TexturePolygon();
                texturePolygon.setTextureRegion(new TextureRegion(texture));
                wall.setTexturePolygon(texturePolygon);

                // add outline
                OutlinePolygon outlinePolygon = new OutlinePolygon();
                wall.getOutlinePolygons().add(outlinePolygon);

                // add shadow
                OutlinePolygon shadowPolygon = new OutlinePolygon();
                shadowPolygon.setColor(new Color(0, 0, 0, 0.35f));
                shadowPolygon.setHalfWidth(0.2f);
                shadowPolygon.setDrawInside(false);
                wall.getOutlinePolygons().add(shadowPolygon);

                // set properties of all 4

                Array<Vector2> converted = new Array<Vector2>();
                for (int i = 0; i < vertices.length - 1; i += 2) {
                        converted.add(new Vector2(vertices[i], vertices[i + 1]));
                }
                Util.translateSoCentroidIsAtOrigin(converted);

                wall.setVertices(converted);

                world.addThing(wall);

                wall.setPosition(0, 0);
                return wall;
        }

        private void addBlock(Texture texture, Float... vertices) {
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

                Array<Vector2> converted = new Array<Vector2>();
                for (int i = 0; i < vertices.length - 1; i += 2) {
                        converted.add(new Vector2(vertices[i], vertices[i + 1]));
                }
                Util.translateSoCentroidIsAtOrigin(converted);


                square.setVertices(converted);
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
