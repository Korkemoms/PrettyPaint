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
 * Draws anti aliased polygon edges.
 *
 * @author Andreas
 */
public class PolygonOutline {

        private final Vector2 position = new Vector2();
        private final Array<Vector2> vertices = new Array<Vector2>(true, 1, Vector2.class);
        private final AuxVertexFinder auxVertexFinder = new AuxVertexFinder();
        private final Vector2 tmp = new Vector2(), tmp1 = new Vector2(), tmp2 = new Vector2(), tmp3 = new Vector2();
        private final Rectangle tmpRectangle = new Rectangle();
        private final Color debugFillGreen = new Color(0, 1, 0, 0.1f);
        private final Color debugFillRed = new Color(1, 0, 0, 0.1f);
        private final Rectangle frustum = new Rectangle();

        protected Array<PolygonOutline> myParents = new Array<PolygonOutline>();
        protected Array<PolygonOutline> myChildren = new Array<PolygonOutline>();

        private float rotation = 0;
        private float weight = 1.25f;
        private Color color = new Color(Color.BLACK);
        private float scale = 1f;

        private float halfWidth = 0.02f;
        private boolean needsVertexUpdateBeforeRendering = false;

        private Array<Vector2> verticesRotatedAndTranslated = new Array<Vector2>(true, 4, Vector2.class);
        private Array<BoundingBox> boundingBoxes = new Array<BoundingBox>();
        private DebugDrawer debugDrawer;
        private boolean drawInside = true;
        private boolean drawOutside = true;

        private int stripVerticesPerVertex = 2;
        private int dataPerStripVertex = 3;
        private int dataPerVertex = stripVerticesPerVertex * dataPerStripVertex;

        private int verticesPerBoundingRectangle = 10;

        private int drawInvocations = 0;
        private boolean closedPolygon = true;

        /** Draws anti aliased polygon edges. */
        public PolygonOutline() {
                debugDrawer = new DebugDrawer() {
                        @Override
                        public void draw(ShapeRenderer shapeRenderer) {
                                debugDraw(shapeRenderer);
                        }
                };
        }


        /**
         * Draw the edges defined by {@link #setVertices(Array)}.
         * If {@link OutlineMerger#mergeOutlines(Array, boolean)} has been used on this {@link PolygonOutline} then
         * the draw method may just be redirected to this polygonOutline's parents.
         *
         * @param batch Accumulates data and sends it in large portions to the gpu, instead of sending small portions more often.
         * @return this for chaining.
         */
        public PolygonOutline draw(PolygonBatch batch) {
                if (myParents.size > 0) {
                        // if i have a parent i will not be drawn
                        for (PolygonOutline polygonOutline : myParents) {
                                polygonOutline.draw(batch);
                        }
                        return this;
                }

                drawInvocations++;
                
                if (myChildren.size == 0 || drawInvocations >= myChildren.size) {
                        // if i don't have any children i will just attempt to draw right away
                        // also if all my children has had their draw method called i will attempt to draw

                        drawInvocations = 0;

                        Rectangle frustum = this.frustum.set(batch.frustum);
                        doHeavyWorkIfNeeded();

                        if (halfWidth <= 0) return this;
                        if (color.a <= 0) return this;
                        if (weight <= 0) return this;
                        if (scale <= 0) return this;
                        if (!drawInside && !drawOutside) return this;


                        for (BoundingBox br : boundingBoxes) {

                                Rectangle cullingArea = getCullingArea(tmpRectangle, br.rectangle, rotation, position, scale);

                                if (frustum.overlaps(cullingArea)) {
                                        // if we reached here we can draw

                                        if (drawInside) {
                                                batch.renderOutlines(br.insideVertexData, 0, br.insideVertexData.size, color, scale, rotation, position.x, position.y, weight);
                                        }
                                        if (drawOutside) {
                                                batch.renderOutlines(br.outsideVertexData, 0, br.outsideVertexData.size, color, scale, rotation, position.x, position.y, weight);
                                        }
                                }
                        }
                }
                return this;
        }


        /**
         * All the {@link BoundingBox}s created here contain {@code verticesPerRectangle}+2
         * vertices, except the last one that may contain more or less.
         * <p>
         * There is some overlap: the first vertex in BoundingBox
         * n is the same as the last vertex in BoundingBox n-1.
         */
        private Array<BoundingBox> createBoundingRectangles(Array<Vector2> vertices, int verticesPerRectangle) {

                Array<BoundingBox> boundingRectangles = new Array<BoundingBox>(true, 4, BoundingBox.class);

                // first create all the bounding rectangles with no overlapping
                for (int i = 0; i < vertices.size; i++) {
                        Vector2 v = vertices.items[i];

                        boolean addNewRect = i % verticesPerRectangle == 0 && i <= vertices.size - verticesPerRectangle;
                        addNewRect |= boundingRectangles.size == 0;

                        if (addNewRect) {
                                if (i > 0) {
                                        boundingRectangles.peek().count = i - boundingRectangles.peek().begin + 1;
                                }
                                boundingRectangles.add(new BoundingBox(new Rectangle(v.x, v.y, 0, 0), i));
                        } else {
                                Rectangle current = boundingRectangles.peek().rectangle;
                                current.merge(v);
                        }
                }
                boundingRectangles.peek().count = vertices.size - boundingRectangles.peek().begin + (closedPolygon ? 1 : 0);


                // make the bounding rectangles overlap
                // (if they do not overlap then edges are not drawn properly when close to the frustum bounds)
                for (BoundingBox br : boundingRectangles) {
                        int begin = br.begin;
                        int end = br.begin + br.count;
                        Rectangle r = br.rectangle;

                        int mergeWithVertex = (begin - 1 + vertices.size) % vertices.size;
                        r.merge(vertices.items[mergeWithVertex]);

                        mergeWithVertex = (end + 1 + vertices.size) % vertices.size;
                        r.merge(vertices.items[mergeWithVertex]);
                }

                return boundingRectangles;
        }

        /** Builds the entire triangle strip (insideStrip, outside or both). */
        private void buildTriangleStrips() {

                Array<Vector2> vertices = this.vertices;
                boolean drawInside = this.drawInside;
                boolean drawOutside = this.drawOutside;
                int dataPerVertex = this.dataPerVertex;
                AuxVertexFinder auxVertexFinder = this.auxVertexFinder;


                boolean clockwisePolygon = RenderUtil.clockwisePolygon(vertices);
                auxVertexFinder.setClockwise(clockwisePolygon);
                auxVertexFinder.setHalfWidth(this.halfWidth);

                if (drawInside) { // setup insideStrip triangle strip
                        auxVertexFinder.setInsideStrip(true);
                        buildStrip(auxVertexFinder);
                }

                if (drawOutside) { // setup outside triangle strip
                        auxVertexFinder.setInsideStrip(false);
                        buildStrip(auxVertexFinder);
                }
        }

        /** Builds either the insideStrip or outside triangle strip. Depending on which AuxVertexFinder is given. */
        private void buildStrip(AuxVertexFinder auxVertexFinder) {

                Vector2 v1 = new Vector2();

                float invisibleAlpha = getAlphaValue(false);
                Array<Vector2> vertices = this.vertices;
                boolean closedPolygon = this.closedPolygon;
                Array<BoundingBox> boundingRectangles = this.boundingBoxes;
                boolean inside = auxVertexFinder.insideStrip;

                // build the triangle strip
                for (int i = 0; i < boundingRectangles.size; i++) {
                        // the triangle strips are split into BoundingRectangles
                        // to allow for easy frustum culling
                        BoundingBox br = boundingRectangles.get(i);

                        Array<Float> vertexData = inside ? br.insideVertexData : br.outsideVertexData;

                        for (int j = br.begin; j < br.begin + br.count; j++) {
                                int k = j % vertices.size;

                                Vector2 currentVertex = vertices.items[k];
                                Vector2 currentAux = auxVertexFinder.getAux(vertices, k);
                                float edgeAlpha = getAlphaValue(true);

                                add(currentVertex, edgeAlpha, vertexData);
                                add(currentAux, invisibleAlpha, vertexData);
                        }
                }

                if (!closedPolygon) {
                        // if polygon is not closed the endings must be fixed
                        {
                                // round the ending
                                BoundingBox br = boundingRectangles.peek();
                                Array<Float> vertexData = inside ? br.insideVertexData : br.outsideVertexData;

                                if (vertexData.size >= 6) {
                                        vertexData.removeRange(vertexData.size - 6, vertexData.size - 1);
                                }

                                int i = vertices.size - 1;
                                Vector2 currentVertex = vertices.items[i];
                                float edgeAlpha = getAlphaValue(true);

                                Vector2 currentAux = auxVertexFinder.getEndingAux(vertices, tmp, 0);
                                add(currentVertex, edgeAlpha, vertexData);
                                add(currentAux, invisibleAlpha, vertexData);

                                currentAux = auxVertexFinder.getEndingAux(vertices, tmp, MathUtils.PI * 0.25f);
                                add(currentVertex, edgeAlpha, vertexData);
                                add(currentAux, invisibleAlpha, vertexData);

                                currentAux = auxVertexFinder.getEndingAux(vertices, tmp, MathUtils.PI * 0.5f);
                                add(currentVertex, edgeAlpha, vertexData);
                                add(currentAux, invisibleAlpha, vertexData);

                        }

                        {
                                // round the beginning
                                BoundingBox br = boundingRectangles.first();
                                Array<Float> vertexData = inside ? br.insideVertexData : br.outsideVertexData;

                                if (vertexData.size >= 6) {
                                        vertexData.removeRange(0, 5);
                                }

                                int i = 0;
                                Vector2 currentVertex = vertices.items[i];
                                float edgeAlpha = getAlphaValue(true);

                                Vector2 currentAux = auxVertexFinder.getBeginningAux(vertices, tmp, 0);
                                insert(currentAux, invisibleAlpha, vertexData);
                                insert(currentVertex, edgeAlpha, vertexData);

                                currentAux = auxVertexFinder.getBeginningAux(vertices, tmp, MathUtils.PI * 0.25f);
                                insert(currentAux, invisibleAlpha, vertexData);
                                insert(currentVertex, edgeAlpha, vertexData);

                                currentAux = auxVertexFinder.getBeginningAux(vertices, tmp, MathUtils.PI * 0.5f);
                                insert(currentAux, invisibleAlpha, vertexData);
                                insert(currentVertex, edgeAlpha, vertexData);

                        }
                }
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

                private Vector2 getAuxBasic(Array<Vector2> vertices, int i) {
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


                /**
                 * Finds the auxiliary vertices that are used to fill all the edges that are made. There is typically
                 * one of these for each vertex set by the user.
                 */
                private Vector2 getAux(Array<Vector2> vertices, int i) {

                        Vector2 basic = getAuxBasic(vertices, i);

                        return basic;

                }

                /** When the polygon is not closed we need auxiliary vertices to round the ending. */
                private Vector2 getEndingAux(Array<Vector2> vertices, Vector2 result, float extraAngleRad) {
                        RenderUtil.getEdge(vertices, vertices.size - 2, m1, m2);
                        nor1.set(m2).sub(m1).nor().scl(halfWidth);

                        int dir = clockWisePolygon ? 1 : -1;

                        if (insideStrip) dir *= -1;

                        nor1.rotate90(dir);
                        nor1.rotateRad(extraAngleRad * (-dir));
                        result.set(m2).add(nor1);

                        return result;
                }

                /** When the polygon is not closed we need auxiliary vertices to round the beginning. */
                private Vector2 getBeginningAux(Array<Vector2> vertices, Vector2 result, float extraAngleRad) {
                        RenderUtil.getEdge(vertices, 0, m1, m2);
                        nor1.set(m1).sub(m2).nor().scl(halfWidth);

                        int dir = clockWisePolygon ? -1 : 1;

                        if (insideStrip) dir *= -1;

                        nor1.rotate90(dir);
                        nor1.rotateRad(extraAngleRad * (-dir));
                        result.set(m1).add(nor1);

                        return result;
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
        public PolygonOutline setHalfWidth(float halfWidth) {
                this.halfWidth = halfWidth;
                needsVertexUpdateBeforeRendering = true;
                return this;
        }

        /**
         * Lines have an insideStrip and an outside part. Sometimes you only want to draw one of them. For example
         * if you want to draw a shadow you may only want to draw the outside part.
         *
         * @param drawInside whether to draw the insideStrip part of the lines.
         * @return this for chaining.
         */
        public PolygonOutline setDrawInside(boolean drawInside) {
                needsVertexUpdateBeforeRendering = true;
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
        public PolygonOutline setDrawOutside(boolean drawOutside) {
                needsVertexUpdateBeforeRendering = true;
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

        /**
         * When instantiating or changing settings some tasks may be scheduled to be
         * done before the next {@link #draw(PolygonBatch)} call. If you wish you can call this
         * method to do these tasks right away. It can be a good idea to call this method after you are done configuring
         * this object, but before you exit your loading screen.
         *
         * @return this for chaining.
         */
        public PolygonOutline doHeavyWorkIfNeeded() {
                if (needsVertexUpdateBeforeRendering) {
                        needsVertexUpdateBeforeRendering = false;

                        this.boundingBoxes = createBoundingRectangles(vertices, verticesPerBoundingRectangle);
                        buildTriangleStrips();
                }

                return this;
        }

        /**
         * Do not modify. If you want to change, translate, rotate or scale the polygon use
         * {@link #setVertices(Array)}, {@link #setPosition(Vector2)}, {@link #setAngleRad(float)} or {@link #setScale(float)} respectively.
         * <p>
         * These vertices are not affected by scale.
         *
         * @return vertices rotated and translated. Do not modify.
         */
        public Array<Vector2> getVerticesRotatedAndTranslated() {

                for (int i = 0; i < vertices.size; i++) {
                        Vector2 w = verticesRotatedAndTranslated.items[i];
                        w.set(vertices.items[i]);
                        w.rotateRad(rotation);
                        w.add(position);
                }
                return verticesRotatedAndTranslated;
        }

        /**
         * Do not modify. If you want to change, translate, rotate or scale the polygon use
         * {@link #setVertices(Array)}, {@link #setPosition(Vector2)}, {@link #setAngleRad(float)} or {@link #setScale(float)} respectively.
         * <p>
         * These vertices are not affected by scale.
         *
         * @return the vertices add by {@link #setVertices(Array)}. Do not modify.
         */
        public Array<Vector2> getVertices() {
                return vertices;
        }


        /**
         * Set the vertices of the polygon. The polygon can be self intersecting.
         * <p>
         * It is recommended that the centroid of these vertices is (0,0).
         * <p>
         * Given array is copied.
         *
         * @param vertices Vertices defining the polygon.
         * @return this for chaining.
         */
        public final PolygonOutline setVertices(Array<Vector2> vertices) {

                needsVertexUpdateBeforeRendering = true;


                this.vertices.clear();
                for (Vector2 v : vertices) {
                        this.vertices.add(new Vector2(v));
                }

                this.verticesRotatedAndTranslated.clear();
                for (Vector2 v : vertices) {
                        this.verticesRotatedAndTranslated.add(new Vector2(v));
                }

                return this;
        }

        /**
         * When true draws the culling rectangles of the triangles.
         *
         * @param batch     The batch you are using to draw.
         * @param debugDraw Whether to draw debug information.
         * @return this for chaining.
         */
        public PolygonOutline setDrawDebugInfo(PolygonBatch batch, boolean debugDraw) {
                if (debugDraw) {
                        if (!batch.debugDrawingTasks.contains(debugDrawer, true))
                                batch.debugDrawingTasks.add(debugDrawer);
                } else {
                        batch.debugDrawingTasks.removeValue(debugDrawer, true);
                }

                return this;
        }

        /**
         * Whether debug information is being drawn.
         *
         * @param batch the batch you are using to draw.
         * @return this for chaining.
         */
        public boolean isDrawingDebugInfo(PolygonBatch batch) {
                return batch.debugDrawingTasks.contains(debugDrawer, true);
        }

        private float getAlphaValue(boolean visible) {
                if (visible) return 1;
                else return 0;
        }

        /** Set the data for one stripVertex. */
        private void add(Vector2 vertex, float alpha, Array<Float> vertexData) {
                vertexData.add(vertex.x);
                vertexData.add(vertex.y);
                vertexData.add(alpha);
        }

        /** Set the data for one stripVertex. */
        private void insert(Vector2 vertex, float alpha, Array<Float> vertexData) {
                vertexData.insert(0, alpha);
                vertexData.insert(0, vertex.y);
                vertexData.insert(0, vertex.x);
        }

        /**
         * @return the angle of the polygon in radians.
         */
        public float getAngleRad() {
                return rotation;
        }

        /**
         * @param angleRad the angle of the polygon in radians.
         * @return this for chaining.
         */
        public PolygonOutline setAngleRad(float angleRad) {
                this.rotation = angleRad;
                return this;
        }

        /**
         * @return the position of the polygon.
         */
        public Vector2 getPosition() {
                return position;
        }

        /**
         * @param position the position of the polygon.
         * @return this for chaining.
         */
        public PolygonOutline setPosition(Vector2 position) {
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
        public PolygonOutline setWeight(float weight) {
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
        public PolygonOutline setColor(Color color) {
                this.color = color;
                return this;
        }

        /**
         * The scale scales everything.
         *
         * @return the scale of the polygon.
         */
        public float getScale() {
                return scale;
        }

        /**
         * The scale scales everything.
         *
         * @param scale the scale of the polygon.
         * @return this for chaining.
         */
        public PolygonOutline setScale(float scale) {
                this.scale = scale;
                return this;
        }

        /**
         * Calculates the culling area of the bounding box after it has scaled, rotated and translates. This bounding box
         * contains a bunch of vertices. This way i don't have to merge hundreds of vertices to get a reasonable culling area,
         * just the four of the bounding box.
         */
        private Rectangle getCullingArea(Rectangle cullingArea, Rectangle boundingBox, float rotation, Vector2 translation, float scale) {

                tmp.set(boundingBox.x, boundingBox.y).scl(scale).rotateRad(rotation).add(translation);
                cullingArea.set(tmp.x, tmp.y, 0, 0);

                tmp.set(boundingBox.x + boundingBox.width, boundingBox.y).scl(scale).rotateRad(rotation).add(translation);
                cullingArea.merge(tmp);

                tmp.set(boundingBox.x + boundingBox.width, boundingBox.y + boundingBox.height).scl(scale).rotateRad(rotation).add(translation);
                cullingArea.merge(tmp);

                tmp.set(boundingBox.x, boundingBox.y + boundingBox.height).scl(scale).rotateRad(rotation).add(translation);
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
        public PolygonOutline setClosedPolygon(boolean closedPolygon) {
                needsVertexUpdateBeforeRendering = true;
                this.closedPolygon = closedPolygon;
                return this;
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

                for (BoundingBox br : boundingBoxes) {

                        Rectangle r = br.rectangle;
                        Rectangle cullingArea = getCullingArea(tmpRectangle, r, rotation, position, scale);

                        shapeRenderer.set(ShapeRenderer.ShapeType.Filled);
                        shapeRenderer.setColor(frustum.overlaps(cullingArea) ? debugFillGreen : debugFillRed);
                        shapeRenderer.rect(cullingArea.x, cullingArea.y, cullingArea.width, cullingArea.height);

                        tmp.set(r.x, r.y).rotateRad(rotation).add(position);
                        tmp1.set(r.x + r.width, r.y).rotateRad(rotation).add(position);
                        tmp2.set(r.x + r.width, r.y + r.height).rotateRad(rotation).add(position);
                        tmp3.set(r.x, r.y + r.height).rotateRad(rotation).add(position);

                        shapeRenderer.set(ShapeRenderer.ShapeType.Line);
                        shapeRenderer.setColor(frustum.overlaps(cullingArea) ? Color.GREEN : Color.RED);

                        shapeRenderer.line(tmp, tmp1);
                        shapeRenderer.line(tmp1, tmp2);
                        shapeRenderer.line(tmp2, tmp3);
                        shapeRenderer.line(tmp3, tmp);


                }

                shapeRenderer.setColor(Color.BLUE);
                for (BoundingBox br : boundingBoxes) {
                        tmp.set(vertices.items[br.begin]);
                        tmp.rotateRad(rotation);
                        tmp.add(position);


                        tmp1.set(vertices.items[(br.begin + br.count) % vertices.size]);
                        tmp1.rotateRad(rotation);
                        tmp1.add(position);

                        shapeRenderer.line(tmp, tmp1);

                }

                for (BoundingBox bb : boundingBoxes) {
                        shapeRenderer.setColor(Color.RED);
                        Array<Float> data = bb.insideVertexData;
                        for (int i = 0; i < data.size - 6; ) {
                                float x1 = data.items[i];
                                float y1 = data.items[i + 1];

                                float x2 = data.items[i + 3];
                                float y2 = data.items[i + 4];
                                i += 3;

                                shapeRenderer.line(x1, y1, x2, y2);
                        }
                        data = bb.outsideVertexData;
                        for (int i = 0; i < data.size - 6; ) {
                                float x1 = data.items[i];
                                float y1 = data.items[i + 1];

                                float x2 = data.items[i + 3];
                                float y2 = data.items[i + 4];
                                i += 3;

                                shapeRenderer.line(x1, y1, x2, y2);
                        }

                }
        }


}
