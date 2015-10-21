package org.prettypaint;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import org.clipper.*;

/**
 * Created by Andreas on 12.10.2015.
 */
public class OutlineMerger {

        private final DefaultClipper defaultClipper = new DefaultClipper();

        private Array<Array<Vector2>> debug = new Array<Array<Vector2>>();

        private DebugDrawer debugDrawer;

        public OutlineMerger() {
                debugDrawer = new DebugDrawer() {
                        @Override
                        public void draw(ShapeRenderer shapeRenderer) {
                                debugDraw(shapeRenderer);
                        }
                };
        }


        public OutlineMerger setDrawDebugInfo(PolygonBatch batch, boolean debugDraw) {
                if (debugDraw) {
                        if (!batch.debugDrawingTasks.contains(debugDrawer, true))
                                batch.debugDrawingTasks.add(debugDrawer);
                } else {
                        batch.debugDrawingTasks.removeValue(debugDrawer, true);
                }

                return this;
        }

        public boolean isDrawingDebugInfo(PolygonBatch batch) {
                return batch.debugDrawingTasks.contains(debugDrawer, true);
        }

        public void clearDebugLines() {
                debug.clear();
        }

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
                                RenderUtil.getEdge(vertices, i, tmp, tmp1);

                                shapeRenderer.line(tmp.x, tmp.y, tmp1.x, tmp1.y);


                        }
                }
        }

        public void mergeOutlines(Array<PolygonOutline> toMerge, boolean verbose) {
                if (toMerge.size == 0) return;
                long begin = System.currentTimeMillis();
                if (verbose) System.out.println("Auto outlining " + toMerge.size + " things");


                Array<Point.LongPoint> previousPoints = new Array<Point.LongPoint>();

                defaultClipper.clear();

                for (int i = 0; i < toMerge.size; i++) {
                        PolygonOutline or = toMerge.get(i);
                        or.myParents.clear();

                        Path path = RenderUtil.convertToPath(or.getVerticesRotatedAndTranslated());

                        alignReallyCloseVertices(previousPoints, path, 20d, verbose);

                        defaultClipper.addPath(path, i == 0 ? Clipper.PolyType.CLIP : Clipper.PolyType.SUBJECT, true);
                }


                Paths unions1 = new Paths();
                defaultClipper.execute(Clipper.ClipType.UNION, unions1, Clipper.PolyFillType.NON_ZERO, Clipper.PolyFillType.NON_ZERO);

                Paths simplified = DefaultClipper.simplifyPolygons(unions1, Clipper.PolyFillType.NON_ZERO);

                if (verbose) System.out.println("Auto outlining resulted in " + simplified.size() + " patches.");

                for (Path path : unions1) {

                        Array<Vector2> vertices = RenderUtil.convertToVectors(path);

                        Array<PolygonOutline> thingsInThisArea = new Array<PolygonOutline>(true, 4, PolygonOutline.class);
                        for (PolygonOutline polygonOutline : toMerge) {

                                Array<Vector2> vertices1 = polygonOutline.getVerticesRotatedAndTranslated();

                                if (RenderUtil.intersectEdges(vertices, vertices1)) {
                                        thingsInThisArea.add(polygonOutline);

                                }
                        }

                        // only one in this union, no need to add a parent
                        if (thingsInThisArea.size <= 1) continue;

                        debug.add(vertices);

                        PolygonOutline outlineParent = new PolygonOutline();
                        outlineParent.setVertices(vertices);

                        // just let the first one decide many of the properties of the parent
                        PolygonOutline first = thingsInThisArea.first();
                        outlineParent.setColor(first.getColor());
                        outlineParent.setDrawOutside(first.isOutsideDrawn());
                        outlineParent.setDrawInside(first.isInsideDrawn());
                        outlineParent.setClosedPolygon(first.isClosedPolygon());
                        outlineParent.setHalfWidth(first.getHalfWidth());
                        outlineParent.setScale(first.getScale());
                        outlineParent.setWeight(first.getWeight());

                        // link parent and children
                        for (PolygonOutline child : thingsInThisArea) {
                                outlineParent.myChildren.add(child);
                                child.myParents.add(outlineParent);
                        }
                }

                if (verbose) {
                        long end = System.currentTimeMillis();
                        System.out.println("Auto outlining took " + (end - begin) + " milliseconds.");
                }
        }

        private void alignReallyCloseVertices(Array<Point.LongPoint> vertices, Path path, double radius, boolean verbose) {
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
