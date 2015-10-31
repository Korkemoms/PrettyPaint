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
package org.prettypaint;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;


/**
 * Uses triangle strips to draw anti aliased polygon edges.
 * Use together with {@link PrettyPolygonBatch} to draw things.
 *
 * @author Andreas
 */
public class OutlinePolygon implements PrettyPolygon {
        /** A vertex that is given by the user of this class. */
        protected static final int VERTEX_TYPE_USER = 1;

        /**
         * An auxiliary vertex that is needed to create the triangle strips.
         * Each user vertex is associated with just one aux vertex.
         */
        protected static final int VERTEX_TYPE_AUX = 0;

        /** The actual vertices of the polygon. */
        private final Array<Vector2> vertices = new Array<Vector2>(true, 1, Vector2.class);

        /** This array is only updated when {@link #getVerticesRotatedAndTranslated()} is called. */
        private final Array<Vector2> verticesRotatedAndTranslated = new Array<Vector2>(true, 4, Vector2.class);

        /**
         * When a value in this array is true the StripVertex with corresponding index must be updated before drawing.
         * There are several methods that cause values in this array to become true.
         */
        private final Array<Boolean> needsUpdate = new Array<Boolean>(true, 4, Boolean.class);

        /** The actual triangle strip is contained in this array. */
        private final Array<StripVertex> vertexDataArray = new Array<StripVertex>(true, 4, StripVertex.class);

        private final AuxVertexFinder auxVertexFinder = new AuxVertexFinder();

        private final Vector2 tmp = new Vector2(), tmp1 = new Vector2(), tmp2 = new Vector2(), tmp3 = new Vector2();
        private final Rectangle tmpRectangle = new Rectangle();
        private final Color tmpColor = new Color();

        private final Rectangle frustum = new Rectangle();

        protected Array<OutlinePolygon> myParents = new Array<OutlinePolygon>();
        protected Array<OutlinePolygon> myChildren = new Array<OutlinePolygon>();

        private final Vector2 position = new Vector2();
        private float angleRad = 0;
        private float weight = 1.25f;
        private Color color = new Color(Color.BLACK);
        private float scale = 1f;
        private float halfWidth = 0.02f;
        private float opacity = 1;

        private boolean roundSharpCorners = true;

        private Array<BoundingBox> boundingBoxes = new Array<BoundingBox>(true, 4, BoundingBox.class);

        private PrettyPolygonBatch.DebugRenderer debugRenderer;
        private final Color debugFillGreen = new Color(0, 1, 0, 0.1f);
        private final Color debugFillRed = new Color(1, 0, 0, 0.1f);

        /** Whether to draw the inside triangle strip. */
        private boolean drawInside = true;

        /** Whether to draw the outside triangle strip. */
        private boolean drawOutside = true;

        /** I am unsure which value is optimal. */
        private int verticesPerBox = 10;

        private boolean visible = true;

        /**
         * Used by a parent to determine when it should draw. A parent draws
         * after all its children has had their draw method called. A child
         * is never drawn itself.
         */
        private int drawInvocations = 0;

        /** Whether to draw the last edge. */
        private boolean closedPolygon = true;

        /** Used by a {@link PrettyPolygonBatch} to determine if it should stop debug rendering this polygon. */
        private long timeOfLastDrawCall;

        private boolean drawBoundingBoxesForDebugDraw = true;
        private boolean drawTriangleStripsForDebugDraw = true;
        private boolean drawLineFromFirstToLastForDebugDraw = true;

        private boolean changesToStripOrCulling = false;

        private boolean drawDebugInfo = false;

        /** Draws anti aliased polygon edges. */
        public OutlinePolygon() {
                debugRenderer = new PrettyPolygonBatch.DebugRenderer(this) {
                        @Override
                        public void draw(ShapeRenderer shapeRenderer) {
                                debugDraw(shapeRenderer);
                        }
                };

                auxVertexFinder.setHalfWidth(this.halfWidth);
        }

        @Override
        public OutlinePolygon setOpacity(float opacity) {
                this.opacity = opacity;
                return this;
        }

        @Override
        public float getOpacity() {
                return opacity;
        }

        @Override
        public long getTimeOfLastDrawCall() {
                return timeOfLastDrawCall;
        }

        /**
         * You can call this method to update a vertex individually, instead of updating
         * all the vertices with {@link #setVertices(Array).
         *
         * @param vertexIndex the index of the vertex you wish to change
         * @param vertex      the new vertex coordinates
         */
        public void updateVertex(int vertexIndex, Vector2 vertex) {
                updateVertex(vertexIndex, vertex.x, vertex.y);
        }

        /**
         * You can call this method to update a vertex individually, instead of updating
         * all the vertices with {@link #setVertices(Array).
         *
         * @param vertexIndex the index of the vertex you wish to change
         * @param newX        the new x coordinate
         * @param newY        the new y coordinate
         */
        public void updateVertex(int vertexIndex, float newX, float newY) {
                vertices.items[vertexIndex].set(newX, newY);

                // schedule this vertex and its neighbours for a remake
                needsUpdate.items[vertexIndex] = true;
                needsUpdate.items[(vertexIndex + 1) % needsUpdate.size] = true;
                needsUpdate.items[(vertexIndex - 1 + needsUpdate.size) % needsUpdate.size] = true;

                changesToStripOrCulling = true;
        }

        public void addVertex(Vector2 vertex) {
                addVertex(vertex.x, vertex.y);
        }

        public void addVertex(float x, float y) {
                vertices.add(new Vector2(x, y));


                if (needsUpdate.size >= 1) needsUpdate.items[needsUpdate.size - 1] = true;
                needsUpdate.add(true);

                if (closedPolygon) needsUpdate.items[0] = true;

                vertexDataArray.add(new StripVertex());
                verticesRotatedAndTranslated.add(new Vector2());

                changesToStripOrCulling = true;

        }

        public void removeVertex(int index) {
                vertices.removeIndex(index);
                verticesRotatedAndTranslated.removeIndex(index);
                needsUpdate.removeIndex(index);
                vertexDataArray.removeIndex(index);

                changesToStripOrCulling = true;

                if (vertices.size == 0) return;

                int previous = (index - 1 + vertices.size) % vertices.size;

                if (previous == vertices.size - 1 && closedPolygon) needsUpdate.items[previous] = true;
                else if (previous >= 0) needsUpdate.items[previous] = true;


                int next = index % vertices.size;

                if (next == 0 && closedPolygon) needsUpdate.items[next] = true;
                else if (next >= 0) needsUpdate.items[next] = true;

        }

        public int getVertexCount() {
                return vertices.size;
        }

        /**
         * Rounding of sharp corners can make the outlines look better.
         *
         * @return whether sharp corners are being rounded.
         */
        public boolean isRoundingSharpCorners() {
                return roundSharpCorners;
        }

        /**
         * Rounding of sharp corners can make the outlines look better.
         *
         * @param roundSharpCorners whether to round sharp corners.
         */
        public void setRoundSharpCorners(boolean roundSharpCorners) {
                boolean change = this.roundSharpCorners != roundSharpCorners;

                if (change) {
                        for (int i = 0; i < needsUpdate.size; i++) {
                                needsUpdate.items[i] = true;
                        }
                        changesToStripOrCulling = true;
                }

                this.roundSharpCorners = roundSharpCorners;
        }

        /**
         * Draw the edges defined by {@link #setVertices(Array)}.
         * If {@link OutlineMerger#mergeOutlines(Array)} has been used on this {@link OutlinePolygon} then
         * the draw method may just be redirected to this outlinePolygon's parents.
         *
         * @param batch Accumulates data and sends it in large portions to the gpu, instead of sending small portions more often.
         * @return this for chaining.
         */
        public OutlinePolygon draw(PrettyPolygonBatch batch) {

                if (myParents.size > 0) {
                        // if i have a parent i will not be drawn
                        for (OutlinePolygon outlinePolygon : myParents) {
                                outlinePolygon.draw(batch);
                        }
                        return this;
                }

                drawInvocations++;

                if (myChildren.size == 0 || drawInvocations >= myChildren.size) {

                        queueDebugDrawIfEnabled(batch);

                        timeOfLastDrawCall = System.currentTimeMillis();
                        // if i don't have any children i will just attempt to draw right away
                        // also if all my children has had their draw method called i will attempt to draw

                        drawInvocations = 0;

                        if (halfWidth <= 0) return this;
                        if (color.a <= 0) return this;
                        if (weight <= 0) return this;
                        if (scale <= 0) return this;
                        if (!drawInside && !drawOutside) return this;

                        Rectangle frustum = this.frustum.set(batch.frustum);

                        tmpColor.set(color).a *= opacity;

                        updateStripAndCulling();


                        for (BoundingBox box : boundingBoxes) {

                                Rectangle cullingArea = getCullingArea(tmpRectangle, box.rectangle, angleRad, position, scale);

                                if (frustum.overlaps(cullingArea)) {
                                        // if we reached here we can draw the vertices within this particular BoundingBox

                                        if (drawInside) {
                                                batch.drawOutline(vertexDataArray, closedPolygon, true, box.begin, box.begin + box.count + 1,
                                                        tmpColor, scale, angleRad, position.x, position.y, weight);
                                        }

                                        if (drawOutside) {
                                                batch.drawOutline(vertexDataArray, closedPolygon, false, box.begin, box.begin + box.count + 1,
                                                        tmpColor, scale, angleRad, position.x, position.y, weight);
                                        }
                                }
                        }
                }
                return this;
        }

        private void queueDebugDrawIfEnabled(PrettyPolygonBatch batch) {
                if (drawDebugInfo)
                        if (!batch.debugRendererArray.contains(debugRenderer, true))
                                batch.debugRendererArray.add(debugRenderer);
        }

        /**
         * When you call methods like for example {@link #setHalfWidth(float)} or {@link #updateVertex(int, float, float)}
         * work is being queued up. Before each draw call this method is automatically called to do all this work.
         * <p>
         * It can be smart to call this method after you are done configuring your OutlinePolygon, but before you
         * exit your loading screen. This way you do more work during the loading screen and get less initial lag.
         */
        public void updateStripAndCulling() {
                if (!changesToStripOrCulling) return;

                updateAllBoxIndices();

                boolean clockwiseUpdated = false;

                for (int i = 0; i < needsUpdate.size; i++) {
                        boolean update = needsUpdate.items[i];
                        if (update) {
                                if (!clockwiseUpdated) {
                                        clockwiseUpdated = true;
                                        updateAuxVertexFinderClockwise();
                                }
                                if (drawInside) {
                                        updateVertex(i, true);
                                }
                                if (drawOutside) {
                                        updateVertex(i, false);
                                }

                                needsUpdate.items[i] = false;
                        }
                }

                for (BoundingBox box : boundingBoxes) {
                        if (box.needsCullingUpdate) {
                                updateBoxCulling(box);
                                box.needsCullingUpdate = false;
                        }
                }
        }


        /**
         * The aux vertex finder needs to know if the polygon is a clockwise one.
         * Instead of figuring out whether the polygon is clockwise each time it needs to know
         * this method is called at most once during {@link #updateStripAndCulling()}, and only if
         * the vertices has been changed.
         */
        private void updateAuxVertexFinderClockwise() {
                boolean clockwisePolygon = RenderUtil.clockwisePolygon(vertices);
                auxVertexFinder.setClockwise(clockwisePolygon);
        }

        /**  */
        private void updateAllBoxIndices() {
                while (boundingBoxes.size * verticesPerBox < vertices.size) {
                        if (boundingBoxes.size >= 1) {
                                boundingBoxes.items[boundingBoxes.size - 1].count = verticesPerBox;
                        }
                        boundingBoxes.add(new BoundingBox(boundingBoxes.size * verticesPerBox, verticesPerBox));
                }
                while (boundingBoxes.size * verticesPerBox >= vertices.size + verticesPerBox) {
                        boundingBoxes.pop();
                }
                if (boundingBoxes.size > 0) {

                        int n = vertices.size % verticesPerBox;
                        if (n == 0) n = verticesPerBox;
                        if (!closedPolygon) n--;
                        boundingBoxes.peek().count = n;

                }
        }

        // TODO Comment
        private void updateBoxCulling(BoundingBox box) {
                Rectangle rectangle = box.rectangle;

                boolean initialized = false;
                for (int n = box.begin; n < box.begin + box.count; n++) {

                        Array<Float> inside = vertexDataArray.items[n].insideVertexData;
                        Array<Float> outside = vertexDataArray.items[n].outsideVertexData;

                        if (!initialized) {
                                if (inside.size > 0 && drawInside) {
                                        rectangle.set(inside.items[0], inside.items[1], 0, 0);
                                } else if (outside.size > 0 && drawOutside) {
                                        rectangle.set(outside.items[0], outside.items[1], 0, 0);
                                }

                                if (closedPolygon || n > 0) {
                                        int k = (box.begin - 1 + vertices.size) % vertices.size;

                                        if (drawInside) {
                                                Array<Float> _inside = vertexDataArray.items[k].insideVertexData;
                                                for (int i = 0; i < _inside.size; i += 3) {
                                                        rectangle.merge(_inside.items[i], _inside.items[i + 1]);
                                                }
                                        }

                                        if (drawOutside) {
                                                Array<Float> _outside = vertexDataArray.items[k].outsideVertexData;
                                                for (int i = 0; i < _outside.size; i += 3) {
                                                        rectangle.merge(_outside.items[i], _outside.items[i + 1]);
                                                }
                                        }
                                }

                                if (closedPolygon || n < vertices.size) {
                                        int k = (box.begin + box.count + vertices.size) % vertices.size;

                                        if (drawInside) {
                                                Array<Float> _inside = vertexDataArray.items[k].insideVertexData;
                                                for (int i = 0; i < _inside.size; i += 3) {
                                                        rectangle.merge(_inside.items[i], _inside.items[i + 1]);
                                                }
                                        }

                                        if (drawOutside) {
                                                Array<Float> _outside = vertexDataArray.items[k].outsideVertexData;
                                                for (int i = 0; i < _outside.size; i += 3) {
                                                        rectangle.merge(_outside.items[i], _outside.items[i + 1]);
                                                }
                                        }
                                }

                                initialized = true;
                        }

                        if (drawInside)
                                for (int i = 0; i < inside.size; i += 3) {
                                        rectangle.merge(inside.items[i], inside.items[i + 1]);
                                }

                        if (drawOutside)
                                for (int i = 0; i < outside.size; i += 3) {
                                        rectangle.merge(outside.items[i], outside.items[i + 1]);
                                }
                }
        }

        // TODO Comment
        private void updateVertex(int index, boolean inside) {
                if (!closedPolygon && index == 0) {
                        updateVertexBeginning(inside);
                } else if (!closedPolygon && index == vertices.size - 1) {
                        updateVertexEnding(inside);
                } else {
                        updateVertexDefault(index, inside);
                }
        }

        // TODO Comment
        private void updateVertexDefault(int index, boolean inside) {

                BoundingBox box = boundingBoxes.items[index / verticesPerBox];
                box.needsCullingUpdate = true;

                int k = index % vertices.size;
                StripVertex stripVertex = vertexDataArray.items[k];
                AuxVertexFinder auxVertexFinder = this.auxVertexFinder;

                Array<Float> vertexData = inside ? stripVertex.insideVertexData : stripVertex.outsideVertexData;
                vertexData.clear();

                auxVertexFinder.setInsideStrip(inside);

                Vector2 currentVertex = vertices.items[k];
                Vector2 defaultAux = auxVertexFinder.getAux(vertices, k);

                boolean roundCorner = false;

                {       // within these brackets i figure out if i should round the corner.
                        // i hope to rewrite this code to something i can understand one day after writing it
                        Vector2 previous = tmp.set(vertices.items[(k - 1 + vertices.size) % vertices.size]);
                        Vector2 copyOfDefaultAux = tmp1.set(defaultAux);

                        previous.sub(currentVertex);
                        copyOfDefaultAux.sub(currentVertex);

                        float angle = previous.angleRad() - copyOfDefaultAux.angleRad();
                        angle = ((angle + MathUtils.PI2) % MathUtils.PI) * 2f;
                        boolean angleMoreThanPI;
                        if (inside) {
                                angleMoreThanPI = angle > MathUtils.PI * 1.1f;
                        } else {
                                angleMoreThanPI = angle < MathUtils.PI * 0.9f;
                        }

                        if (auxVertexFinder.clockWisePolygon) angleMoreThanPI = !angleMoreThanPI;


                        if (angleMoreThanPI) {
                                boolean sharpCorner = Math.abs(MathUtils.PI - angle) > Math.PI * 0.4f;
                                roundCorner = roundSharpCorners && sharpCorner;
                        }
                }

                if (roundCorner) {
                        Vector2 beginningAux = auxVertexFinder.getAuxEnding(vertices, k, 0);
                        Vector2 endingAux = auxVertexFinder.getAuxBeginning(vertices, k, 0);
                        Vector2 middleAux = tmp.set(defaultAux).sub(currentVertex).nor().scl(halfWidth).add(currentVertex);

                        add(currentVertex, VERTEX_TYPE_USER, vertexData);
                        add(beginningAux, VERTEX_TYPE_AUX, vertexData);

                        add(currentVertex, VERTEX_TYPE_USER, vertexData);
                        add(middleAux, VERTEX_TYPE_AUX, vertexData);

                        add(currentVertex, VERTEX_TYPE_USER, vertexData);
                        add(endingAux, VERTEX_TYPE_AUX, vertexData);
                } else {
                        add(currentVertex, VERTEX_TYPE_USER, vertexData);
                        add(defaultAux, VERTEX_TYPE_AUX, vertexData);
                }


        }


        /**
         * Updates a strip vertex with vertices shaping a rounded ending. This method
         * is called when the polygon is open (not {@link #isClosedPolygon()}).
         *
         * @param inside whether this is a <b>inside</b> triangle strip,
         *               as opposed to an outside triangle strip.
         */
        private void updateVertexEnding(boolean inside) {
                // if polygon is not closed the endings must be fixed
                int index = vertices.size - 1;

                BoundingBox box = boundingBoxes.items[index / verticesPerBox];
                box.needsCullingUpdate = true;

                StripVertex stripVertex = vertexDataArray.items[index];
                AuxVertexFinder auxVertexFinder = this.auxVertexFinder;

                Array<Float> vertexData = inside ? stripVertex.insideVertexData : stripVertex.outsideVertexData;
                vertexData.clear();

                auxVertexFinder.setInsideStrip(inside);

                Vector2 currentVertex = vertices.items[index];

                Vector2 currentAux = auxVertexFinder.getAuxEnding(vertices, index, 0);
                add(currentVertex, VERTEX_TYPE_USER, vertexData);
                add(currentAux, VERTEX_TYPE_AUX, vertexData);

                currentAux = auxVertexFinder.getAuxEnding(vertices, index, MathUtils.PI * 0.25f);
                add(currentVertex, VERTEX_TYPE_USER, vertexData);
                add(currentAux, VERTEX_TYPE_AUX, vertexData);

                currentAux = auxVertexFinder.getAuxEnding(vertices, index, MathUtils.PI * 0.5f);
                add(currentVertex, VERTEX_TYPE_USER, vertexData);
                add(currentAux, VERTEX_TYPE_AUX, vertexData);
        }

        /**
         * Updates a strip vertex with vertices shaping a rounded beginning. This method
         * is called when the polygon is open (not {@link #isClosedPolygon()}).
         *
         * @param inside whether this is a <b>inside</b> triangle strip,
         *               as opposed to an outside triangle strip.
         */
        private void updateVertexBeginning(boolean inside) {
                // if polygon is not closed the endings must be fixed
                int index = 0;

                BoundingBox box = boundingBoxes.items[0];
                box.needsCullingUpdate = true;

                StripVertex stripVertex = vertexDataArray.items[index];
                AuxVertexFinder auxVertexFinder = this.auxVertexFinder;

                Array<Float> vertexData = inside ? stripVertex.insideVertexData : stripVertex.outsideVertexData;
                vertexData.clear();

                auxVertexFinder.setInsideStrip(inside);

                Vector2 currentVertex = vertices.items[index];

                Vector2 currentAux = auxVertexFinder.getAuxBeginning(vertices, index, MathUtils.PI * 0.5f);
                add(currentVertex, VERTEX_TYPE_USER, vertexData);
                add(currentAux, VERTEX_TYPE_AUX, vertexData);

                currentAux = auxVertexFinder.getAuxBeginning(vertices, index, MathUtils.PI * 0.25f);
                add(currentVertex, VERTEX_TYPE_USER, vertexData);
                add(currentAux, VERTEX_TYPE_AUX, vertexData);

                currentAux = auxVertexFinder.getAuxBeginning(vertices, index, 0);
                add(currentVertex, VERTEX_TYPE_USER, vertexData);
                add(currentAux, VERTEX_TYPE_AUX, vertexData);
        }

        /**
         * There are the vertices set by the user and then there are the auxiliary vertices that
         * are needed to build a pretty triangle strip. This class can find these auxiliary vertices.
         */
        private class AuxVertexFinder {

                private final Vector2
                        m1 = new Vector2(), m2 = new Vector2(),
                        n1 = new Vector2(), n2 = new Vector2(),
                        nor1 = new Vector2(), nor2 = new Vector2();

                /** Whether to find auxVertices for inside or outside strips */
                boolean insideStrip = true;
                /** Whether to find auxVertices for a clockwise or anti clockwise polygon */
                boolean clockWisePolygon = true;

                /** The width of the inside or outside strip. */
                float halfWidth = 0.02f;

                /** Whether to find auxVertices for inside or outside strips */
                void setInsideStrip(boolean insideStrip) {
                        this.insideStrip = insideStrip;
                }

                /** Whether to find auxVertices for a clockwise or anti clockwise polygon */
                void setClockwise(boolean clockwise) {
                        this.clockWisePolygon = clockwise;
                }

                /** The width of the inside or outside strip. */
                void setHalfWidth(float halfWidth) {
                        this.halfWidth = halfWidth;

                }

                /**
                 * Finds the auxiliary vertices that are used to fill all the edges that are made. There is typically
                 * one of these for each vertex set by the user.
                 */
                private Vector2 getAux(Array<Vector2> vertices, int i) {
                        int dir = clockWisePolygon ? 1 : -1;

                        if (insideStrip) dir *= -1;
                        RenderUtil.getEdge(vertices, (i + vertices.size - 1) % vertices.size, m1, m2);

                        nor1.set(m2).sub(m1).nor().scl(halfWidth).rotate90(dir);
                        m1.add(nor1);
                        m2.add(nor1);

                        RenderUtil.getEdge(vertices, (i + vertices.size) % vertices.size, n1, n2);

                        nor2.set(n2).sub(n1).nor().scl(halfWidth).rotate90(dir);
                        n1.add(nor2);
                        n2.add(nor2);


                        Vector2 result = new Vector2();
                        Intersector.intersectLines(m1, m2, n1, n2, result);
                        return result;

                }

                /** When the polygon is not closed we need auxiliary vertices to round the ending. */
                private Vector2 getAuxBeginning(Array<Vector2> vertices, int i, float extraAngleRad) {
                        RenderUtil.getEdge(vertices, i, m1, m2);
                        nor1.set(m1).sub(m2).nor().scl(halfWidth);

                        int dir = clockWisePolygon ? -1 : 1;

                        if (insideStrip) dir *= -1;

                        nor1.rotate90(dir);
                        nor1.rotateRad(extraAngleRad * (-dir));


                        return new Vector2(m1).add(nor1);
                }

                /** When the polygon is not closed we need auxiliary vertices to round the beginning. */
                private Vector2 getAuxEnding(Array<Vector2> vertices, int i, float extraAngleRad) {
                        RenderUtil.getEdge(vertices, (i - 1 + vertices.size) % vertices.size, m1, m2);
                        nor1.set(m2).sub(m1).nor().scl(halfWidth);

                        int dir = clockWisePolygon ? 1 : -1;

                        if (insideStrip) dir *= -1;

                        nor1.rotate90(dir);
                        nor1.rotateRad(extraAngleRad * (-dir));

                        return new Vector2(m2).add(nor1);
                }
        }

        /** @return half the width of the edges. */
        public float getHalfWidth() {
                return halfWidth;
        }

        /**
         * @param halfWidth half the width of the edges.
         * @return this for chaining.
         */
        public OutlinePolygon setHalfWidth(float halfWidth) {
                this.halfWidth = halfWidth;
                auxVertexFinder.setHalfWidth(this.halfWidth);

                for (int i = 0; i < needsUpdate.size; i++) {
                        needsUpdate.items[i] = true;
                }
                changesToStripOrCulling = true;

                return this;
        }

        /**
         * Lines have an insideStrip and an outside part. Sometimes you only want to draw one of them. For example
         * if you want to draw a shadow you may only want to draw the outside part.
         *
         * @param drawInside whether to draw the insideStrip part of the lines.
         * @return this for chaining.
         */
        public OutlinePolygon setDrawInside(boolean drawInside) {
                this.drawInside = drawInside;
                return this;
        }

        /**
         * Lines have an insideStrip and an outside part. Sometimes you only want to draw one of them. For example
         * if you want to draw a shadow you may only want to draw the outside part.
         *
         * @return whether the insideStrip part of the lines are drawn.
         */
        public boolean isInsideDrawn() {
                return drawInside;
        }

        /**
         * Lines have an insideStrip and an outside part. Sometimes you only want to draw one of them. For example
         * if you want to draw a shadow you may only want to draw the outside part.
         *
         * @param drawOutside whether to draw the outside part of the lines.
         * @return this for chaining.
         */
        public OutlinePolygon setDrawOutside(boolean drawOutside) {
                this.drawOutside = drawOutside;
                return this;
        }

        /**
         * Lines have an insideStrip and an outside part. Sometimes you only want to draw one of them. For example
         * if you want to draw a shadow you may only want to draw the outside part.
         *
         * @return whether the outside part of the lines are drawn.
         */
        public boolean isOutsideDrawn() {
                return drawOutside;
        }


        @Override
        public Array<Vector2> getVerticesRotatedAndTranslated() {

                for (int i = 0; i < vertices.size; i++) {
                        Vector2 w = verticesRotatedAndTranslated.items[i];
                        w.set(vertices.items[i]);
                        w.rotateRad(angleRad);
                        w.add(position);
                }
                return verticesRotatedAndTranslated;
        }

        @Override
        public Array<Vector2> getVertices() {
                return vertices;
        }

        @Override
        public final OutlinePolygon setVertices(Array<Vector2> vertices) {

                this.boundingBoxes.clear();

                this.vertices.clear();
                for (Vector2 v : vertices) {
                        this.vertices.add(new Vector2(v));
                }

                this.needsUpdate.clear();
                for (Vector2 v : vertices) {
                        this.needsUpdate.add(true);
                }

                this.vertexDataArray.clear();
                for (Vector2 v : vertices) {
                        this.vertexDataArray.add(new StripVertex());
                }


                this.verticesRotatedAndTranslated.clear();
                for (Vector2 v : vertices) {
                        this.verticesRotatedAndTranslated.add(new Vector2(v));
                }

                changesToStripOrCulling = true;

                return this;
        }


        /** Set the data for one vertex. Also merges this vertex with the BoundingBox's rectangle. */
        private void add(Vector2 vertex, float alpha, Array<Float> vertexData) {
                vertexData.add(vertex.x);
                vertexData.add(vertex.y);
                vertexData.add(alpha);
        }

        @Override
        public float getAngleRad() {
                return angleRad;
        }

        @Override
        public OutlinePolygon setAngleRad(float angleRad) {
                this.angleRad = angleRad;
                return this;
        }

        @Override
        public Vector2 getPosition() {
                return position;
        }

        @Override
        public OutlinePolygon setPosition(float x, float y) {
                this.position.set(x, y);
                return this;
        }

        @Override
        public OutlinePolygon setPosition(Vector2 position) {
                this.position.set(position);
                return this;
        }

        /**
         * The weight of the "brush". Increase to make edges bolder.
         *
         * @return the weight of the outline.
         */
        public float getWeight() {
                return weight;
        }

        /**
         * The weight of the "brush". Increase to make edges bolder.
         *
         * @param weight the weight of the outline.
         * @return this for chaining.
         */
        public OutlinePolygon setWeight(float weight) {
                this.weight = weight;
                return this;
        }

        /**
         * The color gets more transparent the farther from the center of an edge.
         *
         * @return The color of the edges.
         */
        public Color getColor() {
                return color;
        }

        /**
         * The color gets more transparent the farther from the center of an edge.
         *
         * @param color the color of the edges.
         * @return this for chaining.
         */
        public OutlinePolygon setColor(Color color) {
                this.color = color;
                return this;
        }


        @Override
        public float getScale() {
                return scale;
        }


        @Override
        public OutlinePolygon setScale(float scale) {
                this.scale = scale;
                return this;
        }

        /**
         * Calculates the culling area of the bounding box after it has scaled, rotated and translates. This bounding box
         * contains a bunch of vertices. This way i don't have to merge hundreds of vertices to get a reasonable culling area,
         * just the four of the bounding box.
         */
        private Rectangle getCullingArea(Rectangle cullingArea, Rectangle boundingBox, float rotation, Vector2 translation, float scale) {

                tmp.set(boundingBox.x, boundingBox.y)
                        .scl(scale).rotateRad(rotation).add(translation);
                cullingArea.set(tmp.x, tmp.y, 0, 0);

                tmp.set(boundingBox.x + boundingBox.width, boundingBox.y)
                        .scl(scale).rotateRad(rotation).add(translation);
                cullingArea.merge(tmp);

                tmp.set(boundingBox.x + boundingBox.width, boundingBox.y + boundingBox.height)
                        .scl(scale).rotateRad(rotation).add(translation);
                cullingArea.merge(tmp);

                tmp.set(boundingBox.x, boundingBox.y + boundingBox.height)
                        .scl(scale).rotateRad(rotation).add(translation);
                cullingArea.merge(tmp);

                return cullingArea;
        }

        /**
         * When the polygon is closed an edge is drawn from the last vertex to the first.
         *
         * @return whether the polygon is closed.
         */
        public boolean isClosedPolygon() {
                return closedPolygon;
        }

        /**
         * When the polygon is closed an edge is drawn from the last vertex to the first.
         *
         * @param closedPolygon whether the polygon should be closed.
         * @return this for chaining.
         */
        public OutlinePolygon setClosedPolygon(boolean closedPolygon) {
                boolean change = this.closedPolygon != closedPolygon;

                this.closedPolygon = closedPolygon;

                if (change) {
                        if (needsUpdate.size > 0) {
                                needsUpdate.items[needsUpdate.size - 1] = true;
                                needsUpdate.items[0] = true;
                        }

                }

                changesToStripOrCulling = true;

                return this;
        }

        /** For debugging. */
        public OutlinePolygon setDrawDebugInfo(boolean debugDraw) {
                this.drawDebugInfo = debugDraw;
                return this;
        }

        /** For debugging. */
        public boolean isDrawingDebugInfo(PrettyPolygonBatch batch) {
                return drawDebugInfo;
        }

        /** For debugging. */
        public OutlinePolygon setDrawBoundingBoxesForDebugDraw(boolean drawBoundingBoxesForDebugDraw) {
                this.drawBoundingBoxesForDebugDraw = drawBoundingBoxesForDebugDraw;
                return this;
        }

        /** For debugging. */
        public boolean isDrawingBoundingBoxesForDebugDraw() {
                return drawBoundingBoxesForDebugDraw;
        }


        /** For debugging. */
        public OutlinePolygon setDrawTriangleStripsForDebugDraw(boolean drawTriangleStripsForDebugDraw) {
                this.drawTriangleStripsForDebugDraw = drawTriangleStripsForDebugDraw;
                return this;
        }

        /** For debugging. */
        public boolean isDrawingTriangleStripsForDebugDraw() {
                return drawTriangleStripsForDebugDraw;
        }

        /** For debugging. */
        public OutlinePolygon setDrawLineFromFirstToLastForDebugDraw(boolean drawLineFromFirstToLastForDebugDraw) {
                this.drawLineFromFirstToLastForDebugDraw = drawLineFromFirstToLastForDebugDraw;
                return this;
        }

        /** For debugging. */
        public boolean isDrawingLineFromFirstToLastForDebugDraw() {
                return drawLineFromFirstToLastForDebugDraw;
        }

        /**
         * Draw bounding rectangle(rotated) and culling area(never rotated,
         * contains the bounding rectangle) for each triangle.
         * <p>
         * Also draws a blue line from the beginning vertex
         * to the count vertex of each BoundingBox.
         */
        private void debugDraw(ShapeRenderer shapeRenderer) {
                if (myParents.size > 0) {
                        return;
                }

                if (drawBoundingBoxesForDebugDraw)
                        for (BoundingBox br : boundingBoxes) {

                                Rectangle r = br.rectangle;
                                Rectangle cullingArea = getCullingArea(tmpRectangle, r, angleRad, position, scale);

                                shapeRenderer.set(ShapeRenderer.ShapeType.Filled);
                                shapeRenderer.setColor(frustum.overlaps(cullingArea) ? debugFillGreen : debugFillRed);
                                shapeRenderer.rect(cullingArea.x, cullingArea.y, cullingArea.width, cullingArea.height);

                                tmp.set(r.x, r.y).rotateRad(angleRad).add(position);
                                tmp1.set(r.x + r.width, r.y).rotateRad(angleRad).add(position);
                                tmp2.set(r.x + r.width, r.y + r.height).rotateRad(angleRad).add(position);
                                tmp3.set(r.x, r.y + r.height).rotateRad(angleRad).add(position);

                                shapeRenderer.set(ShapeRenderer.ShapeType.Line);
                                shapeRenderer.setColor(frustum.overlaps(cullingArea) ? Color.GREEN : Color.RED);

                                shapeRenderer.line(tmp, tmp1);
                                shapeRenderer.line(tmp1, tmp2);
                                shapeRenderer.line(tmp2, tmp3);
                                shapeRenderer.line(tmp3, tmp);


                        }


                if (drawLineFromFirstToLastForDebugDraw) {
                        shapeRenderer.setColor(Color.BLUE);
                        for (BoundingBox br : boundingBoxes) {

                                tmp.set(vertices.items[br.begin]);
                                tmp.rotateRad(angleRad);
                                tmp.scl(scale);
                                tmp.add(position);


                                tmp1.set(vertices.items[(br.begin + br.count) % vertices.size]);
                                tmp1.rotateRad(angleRad);
                                tmp1.scl(scale);
                                tmp1.add(position);

                                shapeRenderer.line(tmp, tmp1);

                        }
                }


                if (drawTriangleStripsForDebugDraw) {


                        for (int i = 0; i < vertexDataArray.size; i++) {
                                StripVertex bb = vertexDataArray.items[i];

                                Array<Float> data = bb.insideVertexData;
                                for (int j = 0; j < data.size - 3; ) {

                                        shapeRenderer.setColor(j == 0 ? Color.ORANGE : Color.RED);

                                        tmp.x = data.items[j];
                                        tmp.y = data.items[j + 1];
                                        tmp.rotateRad(angleRad);
                                        tmp.scl(scale);
                                        tmp.add(position);

                                        tmp1.x = data.items[j + 3];
                                        tmp1.y = data.items[j + 4];
                                        tmp1.rotateRad(angleRad);
                                        tmp1.scl(scale);
                                        tmp1.add(position);
                                        j += 3;

                                        shapeRenderer.line(tmp, tmp1);
                                }
                                data = bb.outsideVertexData;
                                for (int j = 0; j < data.size - 3; ) {

                                        shapeRenderer.setColor(j == 0 ? Color.ORANGE : Color.RED);

                                        tmp.x = data.items[j];
                                        tmp.y = data.items[j + 1];
                                        tmp.rotateRad(angleRad);
                                        tmp.scl(scale);
                                        tmp.add(position);

                                        tmp1.x = data.items[j + 3];
                                        tmp1.y = data.items[j + 4];
                                        tmp1.rotateRad(angleRad);
                                        tmp1.scl(scale);
                                        tmp1.add(position);
                                        j += 3;

                                        shapeRenderer.line(tmp, tmp1);
                                }

                        }
                }
        }


        /**
         * The BoundingBox is for dividing outlines into different parts.
         * Each box can be rendered independently of the others,
         * it is therefore convenient for frustum culling.
         * <p>
         * This is a public class because GWT doesn't want to compile otherwise.
         */
        public static class BoundingBox {
                /** The actual bounding rectangle */
                Rectangle rectangle = new Rectangle();

                /** Index of the first vertex that this bounding rectangle include. */
                int begin;
                /** The number of vertices that this bounding rectangle include. */
                int count;

                /**
                 * This box needs a culling update when:
                 * - a new stripVertex is added to it
                 * - a stripVertex is removed from it
                 * - a stripVertex within it changes
                 */
                boolean needsCullingUpdate = true;


                BoundingBox(int begin, int count) {
                        this.begin = begin;
                        this.count = count;
                }

        }

        /**
         * Each vertex supplied by the user must be supplemented by other
         * vertices to make a pretty triangle strip. A strip vertex
         * contains the user vertex and all the other vertices associated with it.
         * It is ordered so that when passed to the gpu, together with the previous and next vertex, a
         * pretty triangle strip is formed.
         * <p>
         * This is a public class because GWT doesn't want to compile otherwise.
         */
        public static class StripVertex {

                Array<Float> insideVertexData = new Array<Float>(true, 9, Float.class);

                Array<Float> outsideVertexData = new Array<Float>(true, 9, Float.class);


        }

        @Override
        public OutlinePolygon setVisible(boolean visible) {
                this.visible = visible;
                return this;
        }

        @Override
        public boolean isVisible() {
                return visible;
        }
}
