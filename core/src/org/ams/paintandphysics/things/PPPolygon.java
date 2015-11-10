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

package org.ams.paintandphysics.things;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import org.ams.physics.things.Polygon;
import org.ams.physics.things.Thing;
import org.ams.prettypaint.OutlinePolygon;
import org.ams.prettypaint.PrettyPolygonBatch;
import org.ams.prettypaint.TexturePolygon;

/**
 * Created by Andreas on 07.11.2015.
 */
public class PPPolygon implements PPWithBody {
        private PPBasic basic;

        /**
         * When using this constructor you must manually set the {@link TexturePolygon}, {@link Polygon}
         * and {@link OutlinePolygon}'s you wish to use. All are optional.
         */
        public PPPolygon() {
                this(null, null);
        }

        /**
         * Initialize from definition, no {@link com.badlogic.gdx.graphics.g2d.TextureRegion} is set
         * for the {@link TexturePolygon} when using this constructor.
         *
         * @param def the definition that sets this polygons properties.
         */
        public PPPolygon(PPThingDef def) {
                this(def, null);
        }

        /**
         * Initialize from definition. The atlas is needed to find the {@link com.badlogic.gdx.graphics.g2d.TextureRegion}
         * specified in the {@link org.ams.prettypaint.def.TexturePolygonDef}.
         * <p/>
         * All the definition attributes in the {@link PPThingDef} are optional.
         *
         * @param def   the definition that sets this polygons properties.
         * @param atlas the atlas that contains the texture specified.
         */
        public PPPolygon(PPThingDef def, TextureAtlas atlas) {
                basic = new PPBasic(def, atlas);
        }

        /**
         * The {@link TexturePolygon} is drawn first.
         *
         * @param batch batch for drawing.
         * @return this for chaining.
         */
        public PPPolygon draw(PrettyPolygonBatch batch) {
                basic.draw(batch);
                return this;
        }

        /**
         * Set the position of all the polygons.
         *
         * @param vertices the vertices of the polygon.
         * @return this for chaining.
         */
        public PPPolygon setVertices(Array<Vector2> vertices) {

                for (OutlinePolygon outlinePolygon : basic.outlinePolygons) {
                        outlinePolygon.setVertices(vertices);
                }

                if (basic.texturePolygon != null) {
                        basic.texturePolygon.setVertices(vertices);
                }

                if (basic.physicsThing != null) {
                        ((Polygon) basic.physicsThing).setVertices(vertices);
                }

                return this;
        }

        /**
         * Set the position of the painting polygons and the physics thing if it has a body.
         *
         * @param x coordinate.
         * @param y coordinate.
         * @return this for chaining.
         */
        @Override
        public PPPolygon setPosition(float x, float y) {
                basic.setPosition(x, y);
                return this;
        }

        @Override
        public PPPolygon setPosition(Vector2 position) {
                basic.setPosition(position.x, position.y);
                return this;
        }

        /**
         * Set the scale of the {@link OutlinePolygon}'s and the {@link TexturePolygon}.
         *
         * @param scale the painting scale.
         * @return this for chaining.
         */
        public PPPolygon setScale(float scale) {
                basic.setScale(scale);
                return this;
        }

        /**
         * Set the angle of the painting polygons and the physics thing if it has a body.
         *
         * @param radians angle in radians.
         * @return this for chaining.
         */
        @Override
        public PPPolygon setAngle(float radians) {
                basic.setAngle(radians);
                return this;
        }

        /**
         * Set the opacity of the {@link OutlinePolygon}'s and the {@link TexturePolygon}.
         *
         * @param opacity the painting opacity.
         * @return this for chaining.
         */
        public PPPolygon setOpacity(float opacity) {
                basic.setOpacity(opacity);
                return this;
        }

        /**
         * Set the visibility of the {@link OutlinePolygon}'s and the {@link TexturePolygon}.
         *
         * @param visible whether to draw the polygons.
         * @return this for chaining.
         */
        public PPPolygon setVisible(boolean visible) {
                basic.setVisible(visible);
                return this;
        }

        @Override
        public Array<OutlinePolygon> getOutlinePolygons() {
                return basic.outlinePolygons;
        }

        @Override
        public TexturePolygon getTexturePolygon() {
                return basic.texturePolygon;
        }

        @Override
        public Thing getPhysicsThing() {
                return basic.physicsThing;
        }

        @Override
        public PPPolygon setTexturePolygon(TexturePolygon texturePolygon) {
                basic.setTexturePolygon(texturePolygon);
                return this;
        }

        @Override
        public PPPolygon setPhysicsThing(Thing physicsThing) {
                basic.setPhysicsThing(physicsThing);
                return this;
        }
}
