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

package org.ams.testapps.paintandphysics.cardhouse;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import org.ams.prettypaint.PrettyPolygonBatch;
import org.ams.prettypaint.TexturePolygon;

/** For drawing seamless textures. */
public class Background {
        private boolean debug = false;

        private OrthographicCamera backgroundCamera;
        private TexturePolygon background;

        // it can have its own batch because it uses a texture that
        // is not from atlas, therefore the number of flushes would remain the same
        // if it shared batch with other classes
        private PrettyPolygonBatch polygonBatch;

        private Color color = new Color(Color.WHITE);

        /** For drawing seamless textures. */
        public Background() {
                if (debug) Gdx.app.setLogLevel(Application.LOG_DEBUG);
                if (debug) debug("New instance.");

                polygonBatch = new PrettyPolygonBatch();
                backgroundCamera = new OrthographicCamera();
        }


        private void debug(String text) {
                if (debug) Gdx.app.log("Background", text);
        }

        /** Hardcoded because it's hard to find the files with GWT. */
        private Array<String> getAvailableBackgrounds(String match) {
                Array<String> backgrounds = new Array<String>();

                String path = "images/backgrounds-light/green_cup.png";
                if (path.contains(match)) backgrounds.add(path);

                path = "images/backgrounds-light/diamond_upholstery_2X.png";
                if (path.contains(match)) backgrounds.add(path);

                return backgrounds;
        }

        /** Color the texture. */
        public void setColor(Color color) {
                this.color = color;
                if (background != null)
                        background.setColor(color);
        }

        /** Color the texture. */
        public Color getColor() {
                return color;
        }

        public void render() {
                polygonBatch.begin(backgroundCamera);
                background.draw(polygonBatch);
                polygonBatch.end();
        }

        public void resize(int width, int height) {
                updateBackgroundBounds(background);
        }

        /**
         * Dispose all resources and nullify references.
         * Must be called when this object is no longer used.
         */
        public void dispose() {
                if (debug) debug("Disposing resources.");
                if (polygonBatch != null) polygonBatch.dispose();
                polygonBatch = null;

                // seamless texture from atlas doesn't work well with prettypaint
                // on many mobile devices
                if (background != null) background.getTextureRegion().getTexture().dispose();

                background = null;

                backgroundCamera = null;
                if (debug) debug("Finished disposing resources.");
        }

        /** Update the background so it looks proper. Must be done after every resize. */
        private void updateBackgroundBounds(TexturePolygon texturePolygon) {

                backgroundCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

                Array<Vector2> vertices = new Array<Vector2>();

                float halfWidth = Gdx.graphics.getWidth() * 0.5f;
                float halfHeight = Gdx.graphics.getHeight() * 0.5f;


                vertices.add(new Vector2(-halfWidth, -halfHeight));
                vertices.add(new Vector2(halfWidth, -halfHeight));
                vertices.add(new Vector2(halfWidth, halfHeight));
                vertices.add(new Vector2(-halfWidth, halfHeight));

                texturePolygon.setVertices(vertices);
                texturePolygon.setPosition(halfWidth, halfHeight);
                texturePolygon.setTextureScale(1);

        }

        /** Set a new background. */
        public void setMatchingBackground(String match) {
                if (debug) debug("Background that matches " + match + " requested.");
                Array<String> selectFrom = getAvailableBackgrounds(match);

                if (background != null) {
                        background.getTextureRegion().getTexture().dispose();
                }
                String selected = selectFrom.random();
                Texture texture = new Texture(selected);
                texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);


                background = new TexturePolygon();

                // seamless texture from atlas doesn't work well with prettypaint
                // on many mobile devices
                background.setTextureRegion(new TextureRegion(texture));
                background.setColor(color);

                updateBackgroundBounds(background);

                if (debug) debug("Selected background " + selected + ".");
        }
}
