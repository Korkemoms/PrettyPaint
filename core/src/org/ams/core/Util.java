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

package org.ams.core;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import org.ams.core.clipper.Path;
import org.ams.core.clipper.Paths;
import org.ams.core.clipper.Point;
import org.ams.core.clipper.Simplify;
import org.ams.core.poly2tri.geometry.polygon.Polygon;
import org.ams.core.poly2tri.geometry.polygon.PolygonPoint;
import org.ams.core.poly2tri.triangulation.delaunay.DelaunayTriangle;

import java.util.ArrayList;
import java.util.List;


// TODO Comment
public class Util {

        public static final double scale = 100000;

        public static boolean intersectEdges(Array<Vector2> a, Array<Vector2> b) {
                Vector2 q = new Vector2();
                Vector2 q1 = new Vector2();

                Vector2 v = new Vector2();
                Vector2 v1 = new Vector2();


                Vector2 w = new Vector2();


                for (int i = 0; i < a.size; i++) {
                        Util.getEdge(a, i, q, q1);
                        for (int j = 0; j < b.size; j++) {
                                Util.getEdge(b, j, v, v1);
                                if (Intersector.intersectSegments(q, q1, v, v1, w)) return true;

                        }
                }

                return false;
        }

        /**
         * Puts the coordinates of the endpoints of the edge with index i into
         * v1 and v2.
         */
        public static void getEdge(Array<Vector2> vertices, int i, Vector2 v1, Vector2 v2) {
                v1.set(vertices.get(i % vertices.size));
                v2.set(vertices.get((i + 1) % vertices.size));
        }

        public static boolean clockwisePolygon(Array<Vector2> vertices) {
                float sum = 0;
                Vector2 a, b;
                for (int i = 0; i < vertices.size; i++) {
                        a = vertices.get(i);
                        b = vertices.get((i + 1) % vertices.size);
                        float edge = (b.x - a.x) * (b.y + a.y);
                        sum += edge;
                }
                return sum > 0;
        }

        public static Path convertToPath(Array<Vector2> vertices) {
                Path path = new Path();
                for (Vector2 v : vertices) {
                        path.add(convertToLongPoint(v));
                }
                return path;
        }

        public static Point.LongPoint convertToLongPoint(Vector2 v) {
                return new Point.LongPoint((long) (v.x * scale), (long) (v.y * scale));
        }

        public static Point.LongPoint convertToLongPoint(float x, float y) {
                return new Point.LongPoint((long) (x * scale), (long) (y * scale));
        }

        public static Vector2 convertToVector(Point.LongPoint lp) {
                return new Vector2((float) (lp.getX() / scale), (float) (lp.getY() / scale));
        }

        public static Array<Array<Vector2>> convertToVectors(Paths paths) {
                Array<Array<Vector2>> converted = new Array<Array<Vector2>>(true, paths.size(), Array.class);
                for (Path p : paths) {
                        converted.add(convertToVectors(p));
                }
                return converted;
        }

        public static float getTouchRadius(float zoom) {
                float f = 0.5f * zoom;
                return (f < 0.05f ? 0.05f : f) * 0.4f;
        }

        public static Array<Triangle> makeTriangles(Array<Vector2> polygon, Array<Array<Vector2>> holes) {

                Array<Triangle> triangles = new Array<Triangle>();

                List<PolygonPoint> asdf = new ArrayList<PolygonPoint>();
                for (Vector2 p : polygon) {
                        asdf.add(new PolygonPoint(p.x, p.y));
                }
                Polygon p = new Polygon(asdf);

                if (holes != null) {
                        for (Array<Vector2> hole : holes) {
                                List<PolygonPoint> qwer = new ArrayList<PolygonPoint>();
                                for (Vector2 q : hole) {
                                        qwer.add(new PolygonPoint(q.x, q.y));
                                }

                                p.addHole(new Polygon(qwer));
                        }
                }

                org.ams.core.poly2tri.Poly2Tri.triangulate(p);
                List<DelaunayTriangle> ll = p.getTriangles();

                for (DelaunayTriangle dl : ll) {

                        Vector2 a = new Vector2((float) dl.points[0].getX(), (float) dl.points[0].getY());
                        Vector2 b = new Vector2((float) dl.points[1].getX(), (float) dl.points[1].getY());
                        Vector2 c = new Vector2((float) dl.points[2].getX(), (float) dl.points[2].getY());

                        triangles.add(new Triangle(a, b, c));

                }

                return triangles;

        }

        public static float[] simplifyAndMakeTriangles(Array<Vector2> vertices) {
                Array<Array<Vector2>> vertices_simplified = Simplify.simplify(vertices);
                Array<float[]> triangles = new Array<float[]>();

                int size = 0;
                for (Array<Vector2> _vertices : vertices_simplified) {
                        float[] _triangles;
                        _triangles = makeTrianglesAsArray(_vertices);
                        triangles.add(_triangles);
                        size += _triangles.length;
                }
                float[] simplified = new float[size];

                int k = 0;
                for (int i = 0; i < triangles.size; i++) {
                        float[] tri = triangles.get(i);

                        for (int j = 0; j < tri.length; j++) {
                                simplified[k++] = tri[j];
                        }
                }
                return simplified;

        }

        public static Array<Triangle> makeTriangles(Array<Vector2> vertices, boolean safe) {
                return makeTriangles(vertices, null, safe);
        }

        private static Array<Vector2> reverse(Array<Vector2> vertices) {

                Array<Vector2> reverse = new Array<Vector2>(vertices.size);
                for (int i = vertices.size - 1; i >= 0; i--) {
                        reverse.add(vertices.get(i));
                }
                return reverse;
        }

        private static boolean checkFixtureArea(Array<Triangle> triangles) {
                float[] triangle = new float[6];
                com.badlogic.gdx.math.Polygon mathPolygon = new com.badlogic.gdx.math.Polygon();

                for (Triangle t : triangles) {
                        triangle[0] = t.a.x;
                        triangle[1] = t.a.y;
                        triangle[2] = t.b.x;
                        triangle[3] = t.b.y;
                        triangle[4] = t.c.x;
                        triangle[5] = t.c.y;
                        mathPolygon.setVertices(triangle);

                        if (Math.abs(mathPolygon.area()) < 0.005) {
                                return false;
                        }
                }
                return true;
        }

        public static Array<Triangle> makeTriangles(Array<Vector2> vertices, Array<Array<Vector2>> holes, boolean safe) {

                Array<Triangle> triangles = makeTriangles(vertices, holes);
                boolean ok = checkFixtureArea(triangles);
                if (!ok) {
                        triangles = makeTriangles(reverse(vertices), holes);
                        ok = checkFixtureArea(triangles);
                }

                if (!ok && safe) {
                        throw new RuntimeException("Could not create polygon with large enough fixtures");
                }
                return triangles;
        }

        public static float[] makeTrianglesAsArray(Array<Vector2> vertices) {
                Array<Triangle> triangles = makeTriangles(vertices, false);
                float[] trianglesAsFloat = new float[triangles.size * 6];

                int j = 0;
                for (int i = 0; i < triangles.size; i++) {
                        Triangle t = triangles.get(i);
                        trianglesAsFloat[j++] = t.a.x;
                        trianglesAsFloat[j++] = t.a.y;
                        trianglesAsFloat[j++] = t.b.x;
                        trianglesAsFloat[j++] = t.b.y;
                        trianglesAsFloat[j++] = t.c.x;
                        trianglesAsFloat[j++] = t.c.y;

                }
                return trianglesAsFloat;
        }

        public static Array<Vector2> convertToVectors(Path path) {
                Array<Vector2> vectors = new Array<Vector2>(true, path.size(), Vector2.class);
                for (Point.LongPoint lp : path) {
                        vectors.add(convertToVector(lp));
                }
                return vectors;
        }

        public static float[] makeTriangles(Array<Vector2> vertices) {

                List<PolygonPoint> converted = new ArrayList<PolygonPoint>();
                for (Vector2 p : vertices) {
                        converted.add(new PolygonPoint(p.x, p.y));
                }
                Polygon p = new Polygon(converted);

                org.ams.core.poly2tri.Poly2Tri.triangulate(p);
                List<DelaunayTriangle> product = p.getTriangles();

                float[] triangles = new float[product.size() * 6];

                int i = 0;
                for (DelaunayTriangle dl : product) {

                        triangles[i++] = (float) dl.points[0].getX();
                        triangles[i++] = (float) dl.points[0].getY();

                        triangles[i++] = (float) dl.points[1].getX();
                        triangles[i++] = (float) dl.points[1].getY();

                        triangles[i++] = (float) dl.points[2].getX();
                        triangles[i++] = (float) dl.points[2].getY();


                }

                return triangles;

        }

        /**
         * Makes pizza slices from a circle.
         */
        public static float[] makePizzaTriangles(Array<Vector2> vertices, Vector2 origin) {

                float[] triangles = new float[vertices.size * 6];

                Vector2 v1 = new Vector2();
                Vector2 v2 = new Vector2();

                int j = 0;
                for (int i = 0; i < vertices.size; i++) {
                        getEdge(vertices, i, v1, v2);

                        triangles[j++] = origin.x;
                        triangles[j++] = origin.y;

                        triangles[j++] = v1.x;
                        triangles[j++] = v1.y;

                        triangles[j++] = v2.x;
                        triangles[j++] = v2.y;

                }
                return triangles;
        }

        /**
         * create a polygon with the shape of a circle
         */
        public static Array<Vector2> makeCircle(Vector2 origin, float radius) {
                Vector2 v2 = new Vector2();
                Vector2 v3 = new Vector2();
                float verticeCount = (int) (36 + radius * 8);
                v2.set(radius, 0);
                // calculate number of segments and length of each segment
                float radiansPerSegment = 0;
                float segmentLength = 0;
                while (segmentLength < 0.05F && verticeCount > 5) {
                        verticeCount--;
                        radiansPerSegment = (float) (2 * Math.PI / verticeCount);
                        v3.x = radius;
                        v3.y = 0;
                        v3.rotateRad(radiansPerSegment);
                        segmentLength = v2.dst(v3);
                }
                Array<Vector2> vertices = new Array<Vector2>(true, (int) verticeCount, Vector2.class);
                for (int i = 0; i < verticeCount; i++) {
                        v2.x = radius;
                        v2.y = 0;
                        v2.rotateRad(i * radiansPerSegment);
                        v2.x += origin.x;
                        v2.y += origin.y;
                        vertices.add(new Vector2(v2));
                }
                return vertices;
        }

        /**
         * Translates given polygon so its centroid is now at the origin.
         *
         * @param vertices some polygon.
         * @return the previous centroid.
         */
        public static Vector2 translateSoCentroidIsAtOrigin(Array<Vector2> vertices) {
                Vector2 centroid = Util.polygonCentroid(vertices);

                for (Vector2 v : vertices) {
                        v.sub(centroid);
                }
                return centroid;
        }

        /**
         * Returns the centroid for the specified non-self-intersecting
         * polygon.
         */
        public static Vector2 polygonCentroid(Array<Vector2> vertices) {
                int count = vertices.size;
                if (count < 3) throw new IllegalArgumentException("A polygon must have 3 or more coordinate pairs.");
                float x = 0, y = 0;

                float signedArea = 0;

                int i = 0;
                for (; i < count - 1; i++) {
                        float x0 = vertices.get(i).x;
                        float y0 = vertices.get(i).y;
                        float x1 = vertices.get(i + 1).x;
                        float y1 = vertices.get(i + 1).y;
                        float a = x0 * y1 - x1 * y0;
                        signedArea += a;
                        x += (x0 + x1) * a;
                        y += (y0 + y1) * a;
                }

                float x0 = vertices.get(i).x;
                float y0 = vertices.get(i).y;
                float x1 = vertices.get(0).x;
                float y1 = vertices.get(0).y;

                float a = x0 * y1 - x1 * y0;
                signedArea += a;
                x += (x0 + x1) * a;
                y += (y0 + y1) * a;

                Vector2 centroid = new Vector2();

                if (signedArea == 0) {
                        centroid.x = 0;
                        centroid.y = 0;
                } else {
                        signedArea *= 0.5f;
                        centroid.x = x / (6 * signedArea);
                        centroid.y = y / (6 * signedArea);
                }
                return centroid;
        }

        public static float getMaximumTranslationX(TextureRegion region) {
                float regWidth = region.getRegionWidth();
                float texWidth = region.getTexture().getWidth();
                return regWidth / texWidth;
        }

        public static float getMaximumTranslationY(TextureRegion region) {
                float regHeight = region.getRegionHeight();
                float texHeight = region.getTexture().getHeight();
                return regHeight / texHeight;
        }

        public static String safeSubstring(Object o, int n) {
                String s = o.toString();
                int y = s.length();
                return s.substring(0, n < y ? n : y);
        }

        /**
         * Find out if a point is inside a polygon. credit:
         * http://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html
         *
         * @param vertx x coordinates of a polygons vertices
         * @param verty y coordinates of a polygons vertices
         * @param nvert number of vertices stored in the arrays
         * @param testx point to Testttstststs
         * @param testy point to Testttstststs
         * @return whether point is inside polygon.
         */
        public static boolean isPointInsidePolygon(float[] vertx, float[] verty,
                                                   int nvert, float testx, float testy) {
                int i;
                int j;
                boolean c = false;
                for (i = 0, j = nvert - 1; i < nvert; j = i++) {
                        if (((verty[i] > testy) != (verty[j] > testy))
                                && (testx < (vertx[j] - vertx[i]) * (testy - verty[i])
                                / (verty[j] - verty[i]) + vertx[i])) {
                                c = !c;
                        }
                }
                return c;
        }

        public static boolean isPointInsidePolygon(Array<Vector2> vert, float testx, float testy) {
                int i;
                int j;
                int nvert = vert.size;
                boolean c = false;
                for (i = 0, j = nvert - 1; i < nvert; j = i++) {
                        if (((vert.items[i].y > testy) != (vert.items[j].y > testy))
                                && (testx < (vert.items[j].x - vert.items[i].x) * (testy - vert.items[i].y)
                                / (vert.items[j].y - vert.items[i].y) + vert.items[i].x)) {
                                c = !c;
                        }
                }
                return c;
        }

        /**
         * This is a public class because GWT doesn't want to compile otherwise.
         */
        public static class Triangle {

                // coordinates
                public final Vector2 a;
                public final Vector2 b;
                public final Vector2 c;


                public Triangle(Vector2 a, Vector2 b, Vector2 c) {
                        this.a = a;
                        this.b = b;
                        this.c = c;
                }

        }
}
