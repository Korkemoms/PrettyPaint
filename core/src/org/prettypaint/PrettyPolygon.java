package org.prettypaint;

import com.badlogic.gdx.graphics.Color;
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
         * {@link #setVertices(Array)}, {@link #setPosition(Vector2)}, {@link #setAngleRad(float)} or {@link #setScale(float)} respectively.
         * <p>
         * These vertices are not affected by scale.
         *
         * @return vertices rotated and translated. Do not modify.
         */
        Array<Vector2> getVerticesRotatedAndTranslated();

        /**
         * Do not modify. If you want to change, translate, rotate or scale the polygon use
         * {@link #setVertices(Array)}, {@link #setPosition(Vector2)}, {@link #setAngleRad(float)} or {@link #setScale(float)} respectively.
         * <p>
         * These vertices are not affected by scale.
         *
         * @return the vertices add by {@link #setVertices(Array)}. Do not modify.
         */
        Array<Vector2> getVertices();

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
        PrettyPolygon setVertices(Array<Vector2> vertices);

        /**
         * When true draws debug information.
         *
         * @param batch     The batch you are using to draw.
         * @param debugDraw Whether to draw debug information.
         * @return this for chaining.
         */
        PrettyPolygon setDrawDebugInfo(PrettyPolygonBatch batch, boolean debugDraw);

        /**
         * When true draws debug information.
         *
         * @param batch the batch you are using to draw.
         * @return whether debug information is being drawn.
         */
        boolean isDrawingDebugInfo(PrettyPolygonBatch batch);

        /**
         * @return the angle of the polygon in radians.
         */
        float getAngleRad();

        /**
         * @param angleRad the angle of the polygon in radians.
         * @return this for chaining.
         */
        PrettyPolygon setAngleRad(float angleRad);

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

        PrettyPolygon setOpacity(float opacity);

        float getOpacity();
}
