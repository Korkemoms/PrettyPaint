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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import org.ams.core.clipper.*;
import org.ams.core.Util;


/**
 * This class is made for merging {@link OutlinePolygon}'s so that when they overlap
 * they look like one.
 * <p/>
 * Does not work properly when the outline polygons are scaled({@link OutlinePolygon#setScale(float)}).
 * <p/>
 * This class use Clipper to do stuff.
 * http://www.angusj.com/delphi/clipper.php
 * http://www.lighti.de/projects/polygon-clipper-for-java/
 */
public class OutlineMerger {

        /** Contains interesting vertices for debugging. */
        private Array<Array<Vector2>> debug = new Array<Array<Vector2>>();

        /** For debugging. */
        private DebugRenderer debugRenderer;

        /** For debugging. */
        private boolean verbose = false;

        public OutlineMerger() {
                debugRenderer = new DebugRenderer(null) {
                        @Override
                        public void draw(ShapeRenderer shapeRenderer) {
                                debugDraw(shapeRenderer);
                        }
                };
        }

        /** For debugging. */
        public void setVerbose(boolean verbose) {
                this.verbose = verbose;
        }

        /** For debugging. */
        public boolean isVerbose() {
                return verbose;
        }

        /** For debugging. */
        public OutlineMerger setDrawDebugInfo(PrettyPolygonBatch batch, boolean debugDraw) {
                if (debugDraw) {
                        if (!batch.debugRendererArray.contains(debugRenderer, true))
                                batch.debugRendererArray.add(debugRenderer);
                } else {
                        batch.debugRendererArray.removeValue(debugRenderer, true);
                }

                return this;
        }

        /** For debugging. */
        public boolean isDrawingDebugInfo(PrettyPolygonBatch batch) {
                return batch.debugRendererArray.contains(debugRenderer, true);
        }

        /** For debugging. */
        public void clearDebugVertices() {
                debug.clear();
        }

        /** For debugging. */
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

        /**
         * Merge {@link OutlinePolygon}'s so that when they overlap they look like one.
         * <p/>
         * Does not work properly when the outline polygons are scaled({@link OutlinePolygon#setScale(float)}).
         *
         * @param toMerge polygons to merge.
         */
        public void mergeOutlines(OutlinePolygon... toMerge) {
                Array<OutlinePolygon> inArray = new Array<OutlinePolygon>();
                for (OutlinePolygon p : toMerge) {
                        inArray.add(p);
                }
                mergeOutlines(inArray);
        }

        /**
         * Merge {@link OutlinePolygon}'s so that when they overlap they look like one.
         * <p/>
         * Does not work properly when the outline polygons are scaled({@link OutlinePolygon#setScale(float)}).
         *
         * @param toMerge polygons to merge.
         */
        public void mergeOutlines(Array<OutlinePolygon> toMerge) {

                if (toMerge.size == 0) return;
                long begin = System.currentTimeMillis();
                if (verbose) Gdx.app.log("OutlineMerger", "Auto outlining " + toMerge.size + " things");


                // merge all overlapping into new polygons

                Array<Point.LongPoint> previousPoints = new Array<Point.LongPoint>();

                DefaultClipper defaultClipper = new DefaultClipper();

                for (int i = 0; i < toMerge.size; i++) {
                        OutlinePolygon or = toMerge.get(i);

                        or.myParents.clear();

                        Path path = Util.convertToPath(or.getVerticesRotatedAndTranslated());

                        // if vertices are really close they are set to be equal
                        alignReallyCloseVertices(previousPoints, path, 20d);

                        defaultClipper.addPath(path, i == 0 ? Clipper.PolyType.CLIP : Clipper.PolyType.SUBJECT, true);
                }


                Paths unions1 = new Paths();
                defaultClipper.execute(Clipper.ClipType.UNION, unions1, Clipper.PolyFillType.NON_ZERO, Clipper.PolyFillType.NON_ZERO);

                Paths simplified = DefaultClipper.simplifyPolygons(unions1, Clipper.PolyFillType.NON_ZERO);
                Paths simplifiedAndCleaned = simplified.cleanPolygons(20d);

                if (verbose)
                        Gdx.app.log("OutlineMerger", "Auto outlining resulted in " + simplifiedAndCleaned.size() + " patches.");

                // clipper has now done all the hard work

                for (Path path : simplifiedAndCleaned) {

                        // group together all the ones in this polygon(path)

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

                        // one parent will now do all the drawing of this polygon

                        OutlinePolygon outlineParent = new OutlinePolygon();
                        outlineParent.setVertices(vertices);

                        // just let the first one decide many of the properties of the parent
                        OutlinePolygon first = thingsInThisArea.first();
                        outlineParent.setColor(first.getColor());
                        outlineParent.setDrawOutside(first.isOutsideDrawn());
                        outlineParent.setDrawInside(first.isInsideDrawn());
                        outlineParent.setClosedPolygon(first.isClosedPolygon());
                        outlineParent.setHalfWidth(first.getHalfWidth());
                        outlineParent.setScale(first.getScale()); // should be 1 otherwise weird things happen
                        outlineParent.setWeight(first.getWeight());

                        // link parent and children
                        for (OutlinePolygon child : thingsInThisArea) {
                                outlineParent.myChildren.add(child);
                                child.myParents.add(outlineParent);
                        }
                }

                if (verbose) {
                        long end = System.currentTimeMillis();
                        Gdx.app.log("OutlineMerger", "Auto outlining took " + (end - begin) + " milliseconds.");
                }
        }

        /**
         * When a vertex in path is really close to a vertex in previousPoints it is set to be the same as that point.
         * All the ones that have found a really close one are then also added to the previousPoints.
         */
        private void alignReallyCloseVertices(Array<Point.LongPoint> previousPoints, Path path, double radius) {
                for (Point.LongPoint testPoint : path) {
                        long testX = testPoint.getX();
                        long testY = testPoint.getY();
                        for (Point.LongPoint previousPoint : previousPoints) {
                                if (testPoint.equals(previousPoint)) continue;

                                long preX = previousPoint.getX();
                                long preY = previousPoint.getY();


                                double dst = Math.sqrt(Math.pow(testX - preX, 2) + Math.pow(testY - preY, 10));

                                if (dst < radius) {
                                        if (verbose)
                                                Gdx.app.log("OutlineMerger", "replacing " + testPoint + " with " + previousPoint);

                                        testPoint.set(previousPoint);
                                }
                        }
                }

                for (Point.LongPoint newPoint : path) {
                        previousPoints.add(newPoint);
                }

        }
}
