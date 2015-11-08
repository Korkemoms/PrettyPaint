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
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import org.ams.physics.things.Circle;
import org.ams.physics.things.Polygon;
import org.ams.physics.things.Thing;
import org.ams.prettypaint.OutlinePolygon;
import org.ams.prettypaint.PrettyPolygonBatch;
import org.ams.prettypaint.TexturePolygon;

/**
 * Created by Andreas on 07.11.2015.
 */
public class PPCircle implements PPWithBody {
        private PPBasic basic;

        private boolean verticesMustBeUpdated = false;
        private float radius = 0.5f;
        private int vertexCount = (int) (radius * 30);

        /**
         * When using this constructor you must manually set the {@link TexturePolygon}, {@link Polygon}
         * and {@link OutlinePolygon}'s you wish to use. All are optional.
         */
        public PPCircle() {
                this(null, null);
        }

        /**
         * Initialize from definition, no {@link com.badlogic.gdx.graphics.g2d.TextureRegion} is set
         * for the {@link TexturePolygon} when using this constructor.
         *
         * @param def the definition that sets this polygons properties.
         */
        public PPCircle(PPThingDef def) {
                this(def, null);
        }

        /**
         * Initialize from definition. The atlas is needed to find the {@link com.badlogic.gdx.graphics.g2d.TextureRegion}
         * specified in the {@link org.ams.prettypaint.def.TexturePolygonDef}.
         * <p>
         * All the definition attributes in the {@link PPThingDef} are optional.
         *
         * @param def   the definition that sets this polygons properties.
         * @param atlas the atlas that contains the texture specified.
         */
        public PPCircle(PPThingDef def, TextureAtlas atlas) {
                basic = new PPBasic(def, atlas);
        }

        /**
         * The {@link TexturePolygon} is drawn first.
         *
         * @param batch batch for drawing.
         * @return this for chaining.
         */
        public PPCircle draw(PrettyPolygonBatch batch) {
                if (verticesMustBeUpdated) {
                        updateVertices();
                        verticesMustBeUpdated = false;
                }


                basic.draw(batch);
                return this;
        }

        private void updateVertices() {
                verticesMustBeUpdated = false;

                Array<Vector2> vertices = new Array<Vector2>();

                float step = MathUtils.PI2 / (float)vertexCount;

                for (float i = 0; i < vertexCount; i++) {
                        Vector2 v = new Vector2(radius, 0);
                        v.rotateRad(step * i);
                        vertices.add(v);
                }

                for (OutlinePolygon outlinePolygon : basic.outlinePolygons) {
                        outlinePolygon.setVertices(vertices);
                }

                if (basic.texturePolygon != null) {
                        basic.texturePolygon.setVertices(vertices);
                }

                if (basic.physicsThing != null) {
                        ((Circle) basic.physicsThing).setRadius(radius);
                }

        }

        public PPCircle setVertexCount(int vertexCount) {
                verticesMustBeUpdated |= this.vertexCount != vertexCount;
                this.vertexCount = vertexCount;
                return this;
        }

        /**
         * Set the position of all the polygons.
         *
         * @param radius the radius of the circle.
         * @return this for chaining.
         */
        public PPCircle setRadius(float radius) {
                verticesMustBeUpdated |= this.radius != radius;
                this.radius = radius;

                return this;
        }

        /**
         * Set the position of the painting polygons and the physics thing if it has a body.
         *
         * @param x coordinate.
         * @param y coordinate.
         * @return this for chaining.
         */
        public PPCircle setPosition(float x, float y) {
                basic.setPosition(x, y);
                return this;
        }

        @Override
        public PPWithBody setPosition(Vector2 position) {
                basic.setPosition(position.x, position.y);
                return this;
        }

        /**
         * Set the angle of the painting polygons and the physics thing if it has a body.
         *
         * @param radians angle in radians.
         * @return this for chaining.
         */
        public PPCircle setAngle(float radians) {
                basic.setAngle(radians);
                return this;
        }

        /**
         * Set the scale of the {@link OutlinePolygon}'s and the {@link TexturePolygon}.
         *
         * @param scale the painting scale.
         * @return this for chaining.
         */
        public PPCircle setScale(float scale) {
                basic.setScale(scale);
                return this;
        }

        /**
         * Set the opacity of the {@link OutlinePolygon}'s and the {@link TexturePolygon}.
         *
         * @param opacity the painting opacity.
         * @return this for chaining.
         */
        public PPCircle setOpacity(float opacity) {
                basic.setOpacity(opacity);
                return this;
        }

        /**
         * Set the visibility of the {@link OutlinePolygon}'s and the {@link TexturePolygon}.
         *
         * @param visible whether to draw the polygons.
         * @return this for chaining.
         */
        public PPCircle setVisible(boolean visible) {
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
        public PPCircle setTexturePolygon(TexturePolygon texturePolygon) {
                basic.setTexturePolygon(texturePolygon);
                return this;
        }

        @Override
        public PPCircle setPhysicsThing(Thing physicsThing) {
                basic.setPhysicsThing(physicsThing);
                return this;
        }
}
