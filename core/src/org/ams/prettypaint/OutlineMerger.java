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

package org.ams.prettypaint;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import org.ams.core.clipper.*;
import org.ams.core.Util;


// TODO Comment
public class OutlineMerger {

        private Array<Array<Vector2>> debug = new Array<Array<Vector2>>();

        private DebugRenderer debugRenderer;

        private boolean verbose = false;

        // TODO Comment
        public OutlineMerger() {
                debugRenderer = new DebugRenderer(null) {
                        @Override
                        public void draw(ShapeRenderer shapeRenderer) {
                                debugDraw(shapeRenderer);
                        }
                };
        }

        public void setVerbose(boolean verbose) {
                this.verbose = verbose;
        }

        public boolean isVerbose() {
                return verbose;
        }

        // TODO Comment
        public OutlineMerger setDrawDebugInfo(PrettyPolygonBatch batch, boolean debugDraw) {
                if (debugDraw) {
                        if (!batch.debugRendererArray.contains(debugRenderer, true))
                                batch.debugRendererArray.add(debugRenderer);
                } else {
                        batch.debugRendererArray.removeValue(debugRenderer, true);
                }

                return this;
        }

        // TODO Comment
        public boolean isDrawingDebugInfo(PrettyPolygonBatch batch) {
                return batch.debugRendererArray.contains(debugRenderer, true);
        }

        // TODO Comment
        public void clearDebugLines() {
                debug.clear();
        }

        // TODO Comment
        private void debugDraw(ShapeRenderer shapeRenderer) {
                Vector2 tmp = new Vector2();
                Vector2 tmp1 = new Vector2();


                shapeRenderer.set(ShapeRenderer.ShapeType.Line);

                int n = 0;
                for (Array<Vector2> vertices : debug) {
                        if (n == 0) {
                                shapeRenderer.setColor(Color.RED);
                        }
                        if (n == 1) {
                                shapeRenderer.setColor(Color.GREEN);
                        }
                        if (n == 2) {
                                shapeRenderer.setColor(Color.BLUE);
                        }
                        if (n == 3) {
                                shapeRenderer.setColor(Color.YELLOW);
                        }
                        n++;

                        for (int i = 0; i < vertices.size; i++) {
                                Util.getEdge(vertices, i, tmp, tmp1);

                                shapeRenderer.line(tmp.x, tmp.y, tmp1.x, tmp1.y);


                        }
                }
        }

        // TODO Comment
        public void mergeOutlines(Array<OutlinePolygon> toMerge) {
                if (toMerge.size == 0) return;
                long begin = System.currentTimeMillis();
                if (verbose) System.out.println("Auto outlining " + toMerge.size + " things");


                Array<Point.LongPoint> previousPoints = new Array<Point.LongPoint>();

                DefaultClipper defaultClipper = new DefaultClipper();

                for (int i = 0; i < toMerge.size; i++) {
                        OutlinePolygon or = toMerge.get(i);
                        or.myParents.clear();

                        Path path = Util.convertToPath(or.getVerticesRotatedAndTranslated());

                        alignReallyCloseVertices(previousPoints, path, 20d);

                        defaultClipper.addPath(path, i == 0 ? Clipper.PolyType.CLIP : Clipper.PolyType.SUBJECT, true);
                }


                Paths unions1 = new Paths();
                defaultClipper.execute(Clipper.ClipType.UNION, unions1, Clipper.PolyFillType.NON_ZERO, Clipper.PolyFillType.NON_ZERO);

                Paths simplified = DefaultClipper.simplifyPolygons(unions1, Clipper.PolyFillType.NON_ZERO);
                Paths simplifiedAndCleaned = simplified.cleanPolygons(20d);

                if (verbose) System.out.println("Auto outlining resulted in " + simplifiedAndCleaned.size() + " patches.");

                for (Path path : simplifiedAndCleaned) {

                        Array<Vector2> vertices = Util.convertToVectors(path);

                        Array<OutlinePolygon> thingsInThisArea = new Array<OutlinePolygon>(true, 4, OutlinePolygon.class);
                        for (OutlinePolygon outlinePolygon : toMerge) {

                                Array<Vector2> vertices1 = outlinePolygon.getVerticesRotatedAndTranslated();

                                if (Util.intersectEdges(vertices, vertices1)) {
                                        thingsInThisArea.add(outlinePolygon);

                                }
                        }

                        // only one in this union, no need to add a parent
                        if (thingsInThisArea.size <= 1) continue;

                        debug.add(vertices);

                        OutlinePolygon outlineParent = new OutlinePolygon();
                        outlineParent.setVertices(vertices);

                        // just let the first one decide many of the properties of the parent
                        OutlinePolygon first = thingsInThisArea.first();
                        outlineParent.setColor(first.getColor());
                        outlineParent.setDrawOutside(first.isOutsideDrawn());
                        outlineParent.setDrawInside(first.isInsideDrawn());
                        outlineParent.setClosedPolygon(first.isClosedPolygon());
                        outlineParent.setHalfWidth(first.getHalfWidth());
                        outlineParent.setScale(first.getScale());
                        outlineParent.setWeight(first.getWeight());

                        // link parent and children
                        for (OutlinePolygon child : thingsInThisArea) {
                                outlineParent.myChildren.add(child);
                                child.myParents.add(outlineParent);
                        }
                }

                if (verbose) {
                        long end = System.currentTimeMillis();
                        System.out.println("Auto outlining took " + (end - begin) + " milliseconds.");
                }
        }

        private void alignReallyCloseVertices(Array<Point.LongPoint> vertices, Path path, double radius) {
                for (Point.LongPoint testPoint : path) {
                        long testX = testPoint.getX();
                        long testY = testPoint.getY();
                        for (Point.LongPoint previousPoint : vertices) {
                                if (testPoint.equals(previousPoint)) continue;

                                long preX = previousPoint.getX();
                                long preY = previousPoint.getY();


                                double dst = Math.sqrt(Math.pow(testX - preX, 2) + Math.pow(testY - preY, 10));

                                if (dst < radius) {
                                        if (verbose)
                                                System.out.println("replacing " + testPoint + " with " + previousPoint);

                                        testPoint.set(previousPoint);
                                }
                        }
                }

                for (Point.LongPoint newPoint : path) {
                        vertices.add(newPoint);
                }

        }

}
