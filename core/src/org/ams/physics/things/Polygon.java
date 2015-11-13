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
package org.ams.physics.things;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.utils.Array;
import org.ams.physics.things.def.PolygonDef;
import org.ams.physics.things.def.ThingDef;
import org.ams.physics.world.BoxWorld;
import org.ams.core.Util;

/**
 * @author Andreas
 */
public class Polygon extends AbstractThingWithBody implements ThingWithBody {

        /**
         * The centroid of these vertices should be at the origin.
         */
        public final Array<Vector2> vertices = new Array<Vector2>(true, 10, Vector2.class);

        public final Array<Vector2> verticesRotatedAndTranslated = new Array<Vector2>(true, 10, Vector2.class);

        private final Vector2 v1 = new Vector2();

        private final Rectangle physicsBoundingBox = new Rectangle();


        public Polygon(PolygonDef def) {
                super(def);
                initFromDefinition(def);

                postInstantiation(def);

        }

        protected void initFromDefinition(PolygonDef def) {
                verticesRotatedAndTranslated.addAll(def.getCopyOfVertices());

                vertices.addAll(def.getCopyOfVertices());

                if (vertices.size >= 3) {
                        Vector2 previousCentroid = Util.translateSoCentroidIsAtOrigin(vertices);

                        bodyDef.position.add(previousCentroid);
                }


        }

        @Override
        public void thingHasBeenAddedToBoxWorld(BoxWorld boxWorld) {

                // the body is created in this supercall
                super.thingHasBeenAddedToBoxWorld(boxWorld);


                updateFixtures();
        }

        private void updateFixtures() {

                // destroy old fixtures
                Array<Fixture> fixtures = body.getFixtureList();

                for (int i = fixtures.size - 1; i >= 0; i--) {
                        body.destroyFixture(fixtures.get(i));
                }

                // add new ones

                float[] triangles = Util.simplifyAndMakeTriangles(vertices);
                float[] triangle = new float[6];

                PolygonShape triangleShape = new PolygonShape();

                // the last thing to do is to create fixtures
                for (int i = 0; i < triangles.length; ) {
                        triangle[0] = triangles[i++];
                        triangle[1] = triangles[i++];
                        triangle[2] = triangles[i++];
                        triangle[3] = triangles[i++];
                        triangle[4] = triangles[i++];
                        triangle[5] = triangles[i++];

                        triangleShape.set(triangle);
                        fixtureDef.shape = triangleShape;
                        Fixture fixture = body.createFixture(fixtureDef);
                        fixture.setUserData(this);
                }

                // the triangles array is no longer needed
                triangleShape.dispose();
        }

        @Override
        public Rectangle getPhysicsBoundingBox() {

                boolean initialized = false;
                for (int i = 0; i < vertices.size; i++) {

                        v1.set(vertices.items[i]).rotateRad(body.getAngle()).add(body.getPosition());
                        if (!initialized) {
                                physicsBoundingBox.set(v1.x, v1.y, 0, 0);
                                initialized = true;
                        }
                        else physicsBoundingBox.merge(v1);
                }


                return physicsBoundingBox;
        }

        @Override
        public String toString() {
                return "P" + getID() + " N=" + vertices.size;
        }


        public void setVertices(Array<Vector2> vertices) {
                this.vertices.clear();
                this.vertices.addAll(vertices);

                this.verticesRotatedAndTranslated.clear();
                this.verticesRotatedAndTranslated.addAll(vertices);

                if (boxWorld != null) updateFixtures();
        }


        public Array<Vector2> getVertices() {
                return vertices;
        }

        public Array<Vector2> getVerticesRotatedAndTranslated() {

                for (int i = 0; i < vertices.size; i++) {

                        Vector2 v = verticesRotatedAndTranslated.items[i];

                        v.set(vertices.items[i]).rotateRad(body.getAngle()).add(body.getPosition());

                }

                return verticesRotatedAndTranslated;
        }

        public int getVertexCount() {
                return vertices.size;
        }


}
