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
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import org.ams.core.CameraNavigator;
import org.ams.core.Util;
import org.ams.prettypaint.PrettyPolygonBatch;
import org.ams.prettypaint.TextureAligner;
import org.ams.prettypaint.TexturePolygon;

/**
 * Tests the texture alignment functionality of {@link TexturePolygon} and {@link TextureAligner}.
 */
public class TextureAlignmentTest2 extends ApplicationAdapter {
        // It is best to use a OrthographicCamera with PrettyPaint
        OrthographicCamera camera;
        CameraNavigator cameraNavigator;

        PrettyPolygonBatch polygonBatch;

        // test textures in atlas and not in atlas
        Texture texture;
        TextureAtlas textureAtlas;
        Array<TextureRegion> testTextureRegions;


        TexturePolygon texturePolygon;

        ShapeRenderer shapeRenderer;


        @Override
        public void dispose() {
                if (polygonBatch != null) polygonBatch.dispose();
                if (texture != null) texture.dispose();
                if (textureAtlas != null) textureAtlas.dispose();
        }

        @Override
        public void create() {
                Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

                // prepare camera and camera controls
                float w = Gdx.graphics.getWidth();
                float h = Gdx.graphics.getHeight();
                camera = new OrthographicCamera(10, 10 * (h / w));
                cameraNavigator = new CameraNavigator(camera);
                Gdx.input.setInputProcessor(cameraNavigator);


                polygonBatch = new PrettyPolygonBatch();

                // load textures
                texture = new Texture("images/pp.JPG");
                textureAtlas = new TextureAtlas("packedimages/pack.atlas");
                testTextureRegions = new Array<TextureRegion>();
                testTextureRegions.add(new TextureRegion(texture));
                for (TextureRegion textureRegion : textureAtlas.getRegions()) {
                        testTextureRegions.add(textureRegion);
                }


                // prepare TexturePolygons
                Array<Vector2> vertices = new Array<Vector2>();
                vertices.add(new Vector2(0, 0));
                vertices.add(new Vector2(2, 0));
                vertices.add(new Vector2(2, 2));
                vertices.add(new Vector2(0, 2));
                Util.translateSoCentroidIsAtOrigin(vertices);

                texturePolygon = new TexturePolygon();
                texturePolygon.setVertices(vertices);
                texturePolygon.setTextureRegion(textureAtlas.findRegion("pp"));

                shapeRenderer = new ShapeRenderer();
                shapeRenderer.setAutoShapeType(true);


        }

        private void drawOrigin() {

                shapeRenderer.begin();

                shapeRenderer.setProjectionMatrix(camera.combined);
                shapeRenderer.set(ShapeRenderer.ShapeType.Line);
                shapeRenderer.setColor(Color.ORANGE);

                shapeRenderer.line(-10, 0, 10, 0);
                shapeRenderer.line(0, -10, 0, 10);


                shapeRenderer.end();
        }

        @Override
        public void render() {
                Gdx.gl20.glClearColor(1, 1, 1, 1);
                Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);


                // draw
                polygonBatch.begin(camera);
                texturePolygon.draw(polygonBatch);
                polygonBatch.end();

                drawOrigin();

        }


        @Override
        public void resize(int width, int height) {

                camera.setToOrtho(false, 10, 10 * ((float) height / (float) width));
                camera.position.x = 0;
                camera.position.y = 0;

                camera.update();
        }

}
