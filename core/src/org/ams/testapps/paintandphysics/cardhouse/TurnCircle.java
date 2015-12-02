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

package org.ams.testapps.paintandphysics.cardhouse;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import org.ams.core.Util;

/** A circle that is made to visualize the area one can touch to turn an object. */
public class TurnCircle {

        private boolean debug = false;
        private boolean visible = false;

        private final Vector2 position = new Vector2();

        // circle
        private final float radius;
        private final float width;
        private final Array<Util.Triangle> triangulatedCircle;


        // camera is used to update zoom
        private final OrthographicCamera camera;
        private float zoom = 1;


        // color
        private final Color color = new Color();
        private final Color moreOpaqueColor = new Color();

        // tiny square in the middle
        private float angleSquareAngle = 0;
        private boolean drawAngleSquare = true;

        // markers
        private float markerSpacing = 5;
        private boolean drawMarkers = true;


        // helper variables for rendering
        private final short[] fillingTriangle = new short[]{0, 1, 2};
        private final float[] tmp_triangle = new float[6];

        private final float[] t = new float[6];
        private final int[] triangle = new int[6];

        private final Vector2 v = new Vector2();
        private final Vector2 v1 = new Vector2();

        /**
         * A circle that is made to visualize the area one can touch to turn an object.
         *
         * @param camera The camera you draw the world with.
         * @param radius distance from center of circle to the inner part of the colored outline.
         * @param width  the width of the colored outline.
         */
        public TurnCircle(OrthographicCamera camera, float radius, float width) {
                if (debug) Gdx.app.setLogLevel(Application.LOG_DEBUG);

                this.camera = camera;

                setColor(Color.PINK);

                // reasonable size for all densities
                this.radius = radius * (float) Math.pow(Gdx.graphics.getDensity(), 0.25);
                this.width = width * (float) Math.pow(Gdx.graphics.getDensity(), 0.25);

                // create triangles that together form a circle
                Array<Vector2> outer = makeCircle(new Vector2(), this.radius + this.width);
                Array<Vector2> inner = makeCircle(new Vector2(), this.radius);
                Array<Array<Vector2>> holes = new Array<Array<Vector2>>();
                holes.add(inner);
                triangulatedCircle = Util.makeTriangles(outer, holes, false);
        }

        private void debug(String text) {
                if (debug) Gdx.app.log("Turn Circle", text);

        }

        /** The opacity value is not used. */
        public void setColor(Color color) {
                this.color.set(color);
                this.color.a = 0.4f;
                this.moreOpaqueColor.set(color);
                this.moreOpaqueColor.a = 0.8f;
        }

        /** The opacity value is not used. */
        public Color getColor() {
                return color;
        }

        /** Markers for angle. See {@link #setDrawMarkers(boolean)}. */
        public void setMarkerSpacing(float markerSpacing) {
                this.markerSpacing = markerSpacing;
        }

        /** Markers for angle. See {@link #setDrawMarkers(boolean)}. */
        public float getMarkerSpacing() {
                return markerSpacing;
        }

        /** Markers for angle. See {@link #setMarkerSpacing(float)}. */
        public void setDrawMarkers(boolean drawMarkers) {
                this.drawMarkers = drawMarkers;
        }

        /** Markers for angle. See {@link #setMarkerSpacing(float)}. */
        public boolean isDrawingMarkers() {
                return drawMarkers;
        }

        /** A tiny square in the middle that can be turned. See {@link #setAngleSquareAngle(float)}. */
        public void setDrawAngleSquare(boolean drawAngleSquare) {
                this.drawAngleSquare = drawAngleSquare;
        }

        /** A tiny square in the middle that can be turned. See {@link #setAngleSquareAngle(float)}. */
        public boolean isDrawingAngleSquare() {
                return drawAngleSquare;
        }

        /** Angle of the tiny square in the middle. Measured in radians. See {@link #setDrawAngleSquare(boolean)}. */
        public void setAngleSquareAngle(float angleSquareAngle) {
                this.angleSquareAngle = angleSquareAngle;
        }

        /** Angle of the tiny square in the middle. Measured in radians. */
        public float getAngleSquareAngle() {
                return angleSquareAngle;
        }

        public void setVisible(boolean visible) {
                this.visible = visible;
        }

        public boolean isVisible() {
                return visible;
        }

        /** Set position of the center of the circle. */
        public void setPosition(Vector2 position) {
                this.position.set(position);
        }

        /** Set position of the center of the circle. */
        public Vector2 getPosition() {
                return position;
        }

        /** Create vertices forming the outline of a circle. */
        private Array<Vector2> makeCircle(Vector2 center, float radius) {
                Array<Vector2> vertices = new Array<Vector2>();

                int n = 100;

                for (int i = 0; i < n; i++) {
                        Vector2 vertex = new Vector2(radius, 0);
                        vertex.rotateRad(i * MathUtils.PI2 / n);
                        vertex.add(center);
                        vertices.add(vertex);
                }
                return vertices;
        }

        /** Whether v is outside the colored outline. */
        public boolean isOutsideTurnCircle(Vector2 v) {
                if (!visible) return false;

                float dst = v.dst(position);
                boolean outside = dst > (radius + width) * zoom;

                if (debug) debug(outside ? v + " is outside circle." : v + " is not outside circle.");
                return outside;

        }

        /** Whether v is on the colored outline. */
        public boolean isOnTurnCircle(Vector2 v) {
                if (!visible) return false;

                float dst = v.dst(position);
                boolean isOn = radius * zoom < dst
                        && dst < (radius + width) * zoom;

                if (debug) debug(isOn ? v + " is on circle." : v + " is not on circle.");
                return isOn;

        }

        /** Whether v is inside the colored outline. */
        public boolean isInsideTurnCircle(Vector2 v) {
                if (!visible) return false;

                float dst = v.dst(position);
                boolean inside = dst < radius * zoom;

                if (debug) debug(inside ? v + " is inside circle." : v + " is not inside circle.");
                return inside;

        }

        /** Radius of the inside. */
        public float getRadius() {
                return radius;
        }

        /** Width of the color outline. */
        public float getWidth() {
                return width;
        }

        /** Draw the turn circle. */
        public void draw(ShapeRenderer worldRenderer) {
                if (!visible) return;

                zoom = camera.zoom * (float) Math.sqrt(camera.viewportHeight / 10);

                worldRenderer.set(ShapeRenderer.ShapeType.Filled);
                worldRenderer.setColor(color);

                // draw circle
                for (Util.Triangle t : triangulatedCircle) {
                        tmp_triangle[0] = t.a.x * zoom + position.x;
                        tmp_triangle[1] = t.a.y * zoom + position.y;
                        tmp_triangle[2] = t.b.x * zoom + position.x;
                        tmp_triangle[3] = t.b.y * zoom + position.y;
                        tmp_triangle[4] = t.c.x * zoom + position.x;
                        tmp_triangle[5] = t.c.y * zoom + position.y;

                        renderPolygonFilling(worldRenderer, tmp_triangle, fillingTriangle);
                }

                if (drawAngleSquare) {
                        // draw square in middle
                        float half = 0.1f * zoom;


                        worldRenderer.rect(
                                position.x - half,
                                position.y - half,
                                half,
                                half,
                                half * 2,
                                half * 2,
                                1,
                                1,
                                angleSquareAngle * MathUtils.radiansToDegrees);
                }

                if (drawMarkers) {
                        // draw angle markers
                        worldRenderer.set(ShapeRenderer.ShapeType.Line);
                        worldRenderer.setColor(moreOpaqueColor);

                        int n = (int) (360 / markerSpacing);

                        float inner = radius * zoom;
                        float outer = (radius + width * 0.1f) * zoom;

                        float rotate = MathUtils.PI2 / n;
                        for (int i = 0; i < n; i++) {
                                v.set(inner, 0).rotateRad(rotate * i).add(position);
                                v1.set(outer, 0).rotateRad(rotate * i).add(position);
                                worldRenderer.line(v.x, v.y, v1.x, v1.y);
                        }
                }

        }

        /** Fill polygon with color. */
        private void renderPolygonFilling(ShapeRenderer renderer, float[] polygon, short[] fillingTriangles) {

                int ntriangles = fillingTriangles.length / 3;
                for (int i = 0; i < ntriangles; i++) {
                        for (int j = 0; j < 3; j++) {
                                triangle[j] = fillingTriangles[j + i * 3];
                        }
                        for (int j = 0; j < 3; j++) {
                                t[2 * j] = polygon[triangle[j] * 2];
                                t[2 * j + 1] = polygon[triangle[j] * 2 + 1];
                        }

                        renderer.triangle(t[0], t[1], t[2], t[3], t[4], t[5]);
                }
        }
}
