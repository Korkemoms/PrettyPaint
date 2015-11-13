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

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 * See {@link OutlinePolygon} and {@link TexturePolygon}.
 */
public interface PrettyPolygon {

        /**
         * Draw this polygon.
         *
         * @param batch accumulates data and sends it in large portions to the gpu, instead of sending small portions more often.
         * @return this for chaining.
         */
        PrettyPolygon draw(PrettyPolygonBatch batch);

        /**
         * Do not modify. If you want to change, translate, rotate or scale the polygon use
         * {@link #setVertices(Array)}, {@link #setPosition(Vector2)}, {@link #setAngle(float)} or {@link #setScale(float)} respectively.
         * <p/>
         * These vertices are not affected by scale.
         *
         * @return vertices rotated and translated. Do not modify.
         */
        Array<Vector2> getVerticesRotatedScaledAndTranslated(float rotation, float scale, float transX, float transY);

        /**
         * Do not modify. If you want to change, translate, rotate or scale the polygon use
         * {@link #setVertices(Array)}, {@link #setPosition(Vector2)}, {@link #setAngle(float)} or {@link #setScale(float)} respectively.
         * <p/>
         * These vertices are not affected by scale.
         *
         * @return the vertices add by {@link #setVertices(Array)}. Do not modify.
         */
        Array<Vector2> getVertices();

        /**
         * Set the vertices of the polygon. The polygon can be self intersecting.
         * <p/>
         * It is recommended that the centroid of these vertices is (0,0).
         * <p/>
         * Given array is copied.
         *
         * @param vertices Vertices defining the polygon.
         * @return this for chaining.
         */
        PrettyPolygon setVertices(Array<Vector2> vertices);

        /**
         * @return the angle of the polygon in radians.
         */
        float getAngle();

        /**
         * @param angle the angle of the polygon in radians.
         * @return this for chaining.
         */
        PrettyPolygon setAngle(float angle);

        /**
         * @return the position of the polygon.
         */
        Vector2 getPosition();

        /**
         * @param position the position of the polygon.
         * @return this for chaining.
         */
        PrettyPolygon setPosition(Vector2 position);

        /**
         * @param x decides how much to horizontally translate the polygon(defined by {@link #setVertices(Array)}) before drawing it.
         * @param y decides how much to vertically translate the polygon(defined by {@link #setVertices(Array)}) before drawing it.
         * @return this for chaining.
         */
        PrettyPolygon setPosition(float x, float y);

        /**
         * The scale scales everything.
         *
         * @return the scale of the polygon.
         */
        float getScale();

        /**
         * The scale scales everything.
         *
         * @param scale the scale of the polygon.
         * @return this for chaining.
         */
        PrettyPolygon setScale(float scale);

        /**
         * Used by a {@link PrettyPolygonBatch} to determine if it should stop debug rendering this
         * polygon.
         *
         * @return the time of last draw call.
         */
        long getTimeOfLastDrawCall();

        /**
         * @param opacity the opacity for this polygon.
         * @return this for chaining.
         */
        PrettyPolygon setOpacity(float opacity);

        /**
         * @return the opacity of this polygon.
         */
        float getOpacity();

        /**
         * An invisible polygon uses almost no cpu or gpu.
         *
         * @param visible whether this polygon should be visible.
         * @return this for chaining.
         */
        PrettyPolygon setVisible(boolean visible);

        /**
         * An invisible polygon uses almost no cpu or gpu.
         *
         * @return whether this polygon is visible.
         */
        boolean isVisible();

        PrettyPolygon setUserData(Object userData);

        Object getUserData();
}
