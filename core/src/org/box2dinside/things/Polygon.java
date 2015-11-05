/*
 The MIT License (MIT)

 Copyright (c) <2015> <Andreas Modahl>

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 */
package org.box2dinside.things;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.utils.Array;
import org.box2dinside.things.def.PolygonDef;
import org.box2dinside.world.BoxWorld;
import org.box2dinside.world.WorldUtil;
import org.core.Util;

/**
 * @author Andreas
 */
public class Polygon extends AbstractThingWithBody implements ThingWithBody {

        /**
         * The centroid of these vertices should be at the origin.
         */
        public final Array<Vector2> vertices = new Array(true, 10, Vector2.class);

        public final Array<Vector2> verticesRotatedAndTranslated = new Array(true, 10, Vector2.class);

        private final Vector2 v1 = new Vector2();

        private final Rectangle physicsBoundingBox = new Rectangle();
        private final Array<Vector2> rotatedAndTranslatedVerticesForPhysicsBoundingBox = new Array(true, 10, Vector2.class);

        public Polygon(BoxWorld boxWorld, PolygonDef def) {
                super(boxWorld, def);

                verticesRotatedAndTranslated.addAll(def.getCopyOfVertices());

                vertices.addAll(def.getCopyOfVertices());
                Vector2 previousCentroid = Util.translateSoCentroidIsAtOrigin(vertices);

                bodyDef.position.add(previousCentroid);


                postInstantiation(def);
        }

        @Override
        public void thingHasBeenAddedToBoxWorld(BoxWorld boxWorld) {

                // the body is created in this supercall
                super.thingHasBeenAddedToBoxWorld(boxWorld);

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
                physicsBoundingBox.x = 1000000;
                physicsBoundingBox.y = 1000000;
                physicsBoundingBox.width = -100000f;
                physicsBoundingBox.height = -1000000f;

                while (rotatedAndTranslatedVerticesForPhysicsBoundingBox.size < verticesRotatedAndTranslated.size) {
                        rotatedAndTranslatedVerticesForPhysicsBoundingBox.add(new Vector2());
                }

                for (int i = 0; i < vertices.size; i++) {

                        v1.set(vertices.items[i]).rotateRad(body.getAngle()).add(body.getPosition());

                        rotatedAndTranslatedVerticesForPhysicsBoundingBox.items[i].x = v1.x;
                        rotatedAndTranslatedVerticesForPhysicsBoundingBox.items[i].y = v1.y;

                        // compute culling rectangle
                        if (v1.x < physicsBoundingBox.x) {
                                physicsBoundingBox.x = v1.x;
                        }
                        if (v1.y < physicsBoundingBox.y) {
                                physicsBoundingBox.y = v1.y;
                        }

                        if (v1.x > physicsBoundingBox.width) {
                                physicsBoundingBox.width = v1.x;
                        }
                        if (v1.y > physicsBoundingBox.height) {
                                physicsBoundingBox.height = v1.y;
                        }
                }

                physicsBoundingBox.width -= physicsBoundingBox.x;
                physicsBoundingBox.height -= physicsBoundingBox.y;

                return physicsBoundingBox;
        }

        @Override
        public String toString() {
                return "P" + getID() + " N=" + vertices.size;
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
