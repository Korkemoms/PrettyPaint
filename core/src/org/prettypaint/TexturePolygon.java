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
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import org.core.Util;


/**
 * Made for drawing seamless textures on polygons.
 * Use together with {@link PrettyPolygonBatch} to draw things.
 *
 * @author Andreas
 */
public class TexturePolygon implements PrettyPolygon {

        private final Vector2 position = new Vector2();
        /** The value given by the user. Alpha values: [0,1] */
        private final Vector2 textureTranslation = new Vector2();
        /** Alpha values: [0,1] */
        private final Vector2 actualTextureTranslation = new Vector2();
        private final short[] fillingTriangle = new short[]{0, 1, 2};
        private final Rectangle tmpRectangle = new Rectangle();
        private final Color tmpColor = new Color(1, 1, 1, 1);
        private final Vector2 tmpVector = new Vector2();
        /** These are all triangles. */
        private final Array<PolygonRegion> polygonRegions;
        private final PrettyPolygonBatch.DebugRenderer debugRenderer;
        private float angleRad = 0f;
        private float scale = 1f;
        private float textureScale = 0.01f;
        private float opacity = 1f;
        private boolean visible = true;

        private TextureRegion textureRegion;
        /** Vertices defining the polygon being drawn. */
        private final Array<Vector2> vertices = new Array<Vector2>(true, 4, Vector2.class);

        private Array<Vector2> verticesRotatedAndTranslated = new Array<Vector2>(true, 4, Vector2.class);

        /** used for frustum culling. */
        private Rectangle regionBounds;
        /** Only the opacity value is used. */
        private float tmpColorAsFloatBits = tmpColor.toFloatBits();
        /** stored to avoid extra computation when setting new texture, client code can also make use of these triangles. */
        private float[] triangles;
        /** Rotation of the texture. */
        private float textureAngleRad = 0;
        /** The last time */
        public long timeOfLastRender;
        private boolean drawDebugInfo = false;

        /** Used by a {@link PrettyPolygonBatch} to determine if it should stop debug rendering this polygon. */
        private long timeOfLastDrawCall;


        /** Made for drawing seamless textures on polygons. */
        public TexturePolygon() {
                polygonRegions = new Array<PolygonRegion>(true, 4, PolygonRegion.class);
                debugRenderer = new PrettyPolygonBatch.DebugRenderer(this) {
                        @Override
                        public void draw(ShapeRenderer shapeRenderer) {
                                debugDraw(shapeRenderer);
                        }
                };
        }

        @Override
        public long getTimeOfLastDrawCall() {
                return timeOfLastDrawCall;
        }

        /**
         * With texture angle you can rotate the texture without rotating the edges of the polygon.
         *
         * @return the angle of the texture in radians.
         */
        public float getTextureAngleRad() {
                return textureAngleRad;
        }

        /**
         * With texture angle you can rotate the texture without rotating the edges of the polygon.
         *
         * @param textureAngleRad the angle of the texture in radians.
         */
        public void setTextureAngleRad(float textureAngleRad) {
                Vector2 v = new Vector2();
                for (int i = 0; i < polygonRegions.size; i++) {
                        PolygonRegion toReplace = polygonRegions.get(i);

                        float[] vertices = toReplace.getVertices();

                        for (int j = 0; j < vertices.length; ) {
                                v.set(vertices[j], vertices[j + 1]);
                                v.rotateRad(this.textureAngleRad - textureAngleRad);
                                vertices[j] = v.x;
                                vertices[j + 1] = v.y;
                                j += 2;
                        }

                        PolygonRegion replacement = new PolygonRegion(
                                toReplace.getRegion(),
                                vertices,
                                toReplace.getTriangles());

                        polygonRegions.set(i, replacement);


                }
                this.textureAngleRad = textureAngleRad;
        }

        /** Set the texture to be upright at the polygons current angle. */
        public void setTextureUprightForCurrentAngle() {
                Vector2 v = new Vector2();
                for (int i = 0; i < polygonRegions.size; i++) {
                        PolygonRegion toReplace = polygonRegions.get(i);

                        float[] vertices = toReplace.getVertices();

                        for (int j = 0; j < vertices.length; ) {
                                v.set(vertices[j], vertices[j + 1]);
                                v.rotateRad(textureAngleRad + angleRad);
                                vertices[j] = v.x;
                                vertices[j + 1] = v.y;
                                j += 2;
                        }

                        PolygonRegion replacement = new PolygonRegion(
                                toReplace.getRegion(),
                                vertices,
                                toReplace.getTriangles());

                        polygonRegions.set(i, replacement);


                }
                textureAngleRad = -angleRad;

        }


        /**
         * Draws the culling rectangles of the triangles.
         *
         * @param shapeRenderer can draw rectangles.
         */
        private void debugDraw(ShapeRenderer shapeRenderer) {
                shapeRenderer.set(ShapeRenderer.ShapeType.Line);
                shapeRenderer.setColor(Color.GREEN);

                float scale = this.textureScale * this.scale;

                for (PolygonRegion pr : polygonRegions) {
                        Rectangle cullingArea = this.tmpRectangle;
                        getCullingArea(cullingArea, pr, angleRad + textureAngleRad, position, scale);
                        shapeRenderer.rect(cullingArea.x, cullingArea.y, cullingArea.width, cullingArea.height);
                }
        }

        /**
         * Draw texture on the polygon defined by {@link #setVertices(Array)}.
         *
         * @param batch Accumulates data and sends it in large portions to the gpu, instead of sending small portions more often.
         * @return this for chaining.
         */
        @Override
        public TexturePolygon draw(PrettyPolygonBatch batch) {
                if (batch == null) throw new RuntimeException("You must supply a batch.");
                if (textureRegion == null) throw new RuntimeException("You must set a TextureRegion first.");
                if (vertices == null) throw new RuntimeException("You must set the vertices first.");

                if (!visible) return this;
                if (opacity <= 0) return this;


                Texture texture = textureRegion.getTexture();

                float textureWidth = texture.getWidth();
                float textureHeight = texture.getHeight();

                float srcWidth = textureRegion.getRegionWidth() / textureWidth;
                float srcHeight = textureRegion.getRegionHeight() / textureHeight;
                if (srcWidth != srcHeight)
                        throw new IllegalArgumentException("Texture width and height must be equal. :(");
                float tex_width_and_height = srcHeight;

                timeOfLastDrawCall = System.currentTimeMillis();

                Rectangle frustum = batch.frustum;

                float scale = this.textureScale * this.scale;

                long now = System.currentTimeMillis();

                queueDebugDrawIfEnabled(batch);

                for (int i = 0; i < polygonRegions.size; i++) {
                        PolygonRegion pr = polygonRegions.items[i];
                        Rectangle cullingArea = this.tmpRectangle;
                        getCullingArea(cullingArea, pr, angleRad + textureAngleRad, position, scale);


                        if (frustum.overlaps(cullingArea)) {
                                timeOfLastRender = now;
                                batch.drawTexture(pr,
                                        position.x,
                                        position.y,
                                        regionBounds.width,
                                        regionBounds.height,
                                        scale,
                                        scale,
                                        angleRad + textureAngleRad,
                                        regionBounds.x / textureWidth,
                                        regionBounds.y / textureHeight,
                                        actualTextureTranslation.x,
                                        actualTextureTranslation.y,
                                        tex_width_and_height,
                                        tmpColorAsFloatBits
                                );
                        }
                }
                return this;
        }

        /**
         * Do not modify. If you want to change, translate, rotate or scale the polygon use
         * {@link #setVertices(Array)}, {@link #setPosition(Vector2)}, {@link #setAngleRad(float)} or {@link #setScale(float)} respectively.
         * <p>
         * These vertices are not affected by scale.
         *
         * @return the vertices set by {@link #setVertices(Array)}. Do not modify.
         */
        @Override
        public Array<Vector2> getVertices() {
                return vertices;
        }

        /**
         * Set the vertices of the polygon. The polygon can be self intersecting.
         * <p>
         * It is recommended that the centroid of these vertices is (0,0).
         * <p>
         * <p>
         * Given array is copied.
         *
         * @param vertices Vertices defining the polygon.
         * @return this for chaining.
         */
        @Override
        public TexturePolygon setVertices(Array<Vector2> vertices) {
                this.vertices.clear();
                for (Vector2 v : vertices) {
                        this.vertices.add(new Vector2(v));
                }


                float[] triangles = Util.simplifyAndMakeTriangles(vertices);

                setTriangles(triangles);

                return this;
        }

        /**
         * Do not modify. If you want to change the TextureRegion use {@link #setTextureRegion(TextureRegion)}.
         *
         * @return the {@link TextureRegion} set by {@link #setTextureRegion(TextureRegion)}. Do not modify.
         */
        public TextureRegion getTextureRegion() {
                return textureRegion;
        }

        /**
         * The given region should be square, otherwise it will not look seamless. I hope to fix
         * this soon.
         * The texture region should contain a seamless texture.
         * <p>
         * Use a {@link com.badlogic.gdx.graphics.g2d.TextureAtlas} to manage your texture regions.
         *
         * @param textureRegion the region you wish to draw on the polygon defined by {@link #setVertices(Array)}.
         * @return this for chaining.
         */
        public TexturePolygon setTextureRegion(TextureRegion textureRegion) {
                if (textureRegion == null) throw new RuntimeException("TextureRegion can not be null. ");


                boolean change = this.textureRegion == null || !this.textureRegion.equals(textureRegion);

                if (change) {

                        this.textureRegion = textureRegion;
                        regionBounds = new Rectangle(textureRegion.getRegionX(),
                                textureRegion.getRegionY(),
                                textureRegion.getRegionWidth(),
                                textureRegion.getRegionHeight());

                        Array<PolygonRegion> newRegions = new Array<PolygonRegion>(true, 4, PolygonRegion.class);
                        for (int i = 0; i < polygonRegions.size; i++) {
                                PolygonRegion pr = polygonRegions.get(i);
                                // reuse the triangles and vertices
                                newRegions.add(new PolygonRegion(textureRegion, pr.getVertices(), pr.getTriangles()));
                        }
                        polygonRegions.clear();
                        polygonRegions.addAll(newRegions);

                        setTextureTranslation(new Vector2(
                                (float) Math.random() * MathUtils.random() * Util.getMaximumTranslationX(textureRegion),
                                (float) Math.random() * MathUtils.random() * Util.getMaximumTranslationY(textureRegion)));
                }
                return this;
        }

        private void queueDebugDrawIfEnabled(PrettyPolygonBatch batch) {
                if (drawDebugInfo)
                        if (!batch.debugRendererArray.contains(debugRenderer, true))
                                batch.debugRendererArray.add(debugRenderer);
        }

        /**
         * When true draws the culling rectangles of the triangles.
         *
         * @param debugDraw Whether to draw debug information.
         * @return this for chaining.
         */
        @Override
        public TexturePolygon setDrawDebugInfo(boolean debugDraw) {
                this.drawDebugInfo = debugDraw;
                return this;
        }

        /**
         * Whether debug information is being drawn.
         *
         * @param batch the batch you are using to draw.
         * @return this for chaining.
         */
        @Override
        public boolean isDrawingDebugInfo(PrettyPolygonBatch batch) {
                return drawDebugInfo;
        }

        /**
         * Source translation allows you move the texture around within the polygon.
         * <p>
         * Do not modify. If you wish to change SourceTranslation use one of these methods:
         * -{@link #setSourceTranslation(float, float)}
         * -{@link #setTextureTranslation(Vector2)}
         * -{@link #alignTexture(float, float)}
         * -{@link #alignTexture(Vector2)}
         *
         * @return the source translation. Do not modify.
         */
        public Vector2 getTextureTranslation() {
                return textureTranslation;
        }

        /**
         * Source translation allows you move the texture around within the polygon.
         *
         * @param textureTranslation the source translation.
         * @return this for chaining.
         */
        public TexturePolygon setTextureTranslation(Vector2 textureTranslation) {
                return setSourceTranslation(textureTranslation.x, textureTranslation.y);
        }

        /**
         * Source translation allows you move the texture around within the polygon.
         *
         * @param x x translation of source.
         * @param y y translation of source.
         * @return this for chaining.
         */
        public TexturePolygon setSourceTranslation(float x, float y) {
                if (textureRegion == null)
                        throw new RuntimeException("You must set the textureRegion before using this method.");

                this.textureTranslation.set(x, y);

                actualTextureTranslation.set(x, y);
                actualTextureTranslation.scl(scale);

                actualTextureTranslation.x *= Util.getTextureAlignmentConstantX(textureRegion);
                actualTextureTranslation.y *= Util.getTextureAlignmentConstantY(textureRegion);

                actualTextureTranslation.x %= Util.getMaximumTranslationX(textureRegion);
                actualTextureTranslation.y %= Util.getMaximumTranslationY(textureRegion);

                return this;
        }

        /**
         * This method lets you align the texture so that if two or more different {@link #TexturePolygon}'s are overlapping
         * the texture still looks seamless. They must have the same scale and textureScale for this to work.
         *
         * @param extraTranslation Extra translation allows you to move the texture around within the polygon.
         *                         Use the same value for all the {@link #TexturePolygon}'s you wish to align.
         * @return this for chaining.
         */
        public TexturePolygon alignTexture(Vector2 extraTranslation) {
                return alignTexture(extraTranslation.x, extraTranslation.y);
        }

        /**
         * This method lets you align the texture so that if two or more different {@link #TexturePolygon}'s are overlapping
         * the texture still looks seamless. They must have the same scale and textureScale for this to work.
         *
         * @param extraTranslationX Extra translation allows you to move the texture around within the polygon.
         *                          Use the same value for all the {@link #TexturePolygon}'s  you wish to align.
         * @param extraTranslationY Extra translation allows you to move the texture around within the polygon.
         *                          Use the same value for all the {@link #TexturePolygon}'s you wish to align.
         * @return this for chaining.
         */
        public TexturePolygon alignTexture(float extraTranslationX, float extraTranslationY) {
                Vector2 v = new Vector2(position);
                v.rotateRad(-angleRad - textureAngleRad);

                v.add(extraTranslationX, extraTranslationY);

                setTextureTranslation(v);
                return this;
        }

        /**
         * Source scale lets you zoom in and out on the texture without changing the size of the polygon.
         *
         * @return the source scale.
         */
        public float getTextureScale() {
                return textureScale;
        }

        /**
         * Source scale lets you zoom in and out on the texture without changing the size of the polygon.
         *
         * @param textureScale the source scale.
         * @return this for chaining.
         */
        public TexturePolygon setTextureScale(float textureScale) {
                this.textureScale = textureScale;
                setTriangles(triangles);
                return this;
        }

        /** Computes the culling area of the polygon after scaling, rotating and translating. */
        private void getCullingArea(Rectangle cullingArea, PolygonRegion pr, float rotation, Vector2 translation, float scale) {

                float[] vertices = pr.getVertices();


                tmpVector.set(vertices[0], vertices[1]);
                tmpVector.scl(scale);
                tmpVector.rotateRad(rotation);
                tmpVector.add(translation);
                cullingArea.set(tmpVector.x, tmpVector.y, 0, 0);

                tmpVector.set(vertices[2], vertices[3]);
                tmpVector.scl(scale);
                tmpVector.rotateRad(rotation);
                tmpVector.add(translation);
                cullingArea.merge(tmpVector);

                tmpVector.set(vertices[4], vertices[5]);
                tmpVector.scl(scale);
                tmpVector.rotateRad(rotation);
                tmpVector.add(translation);
                cullingArea.merge(tmpVector);


        }

        /** @return the opacity. In range 0 to 1. 0 is invisible, 1 is full visible. */
        @Override
        public float getOpacity() {
                return opacity;
        }

        /**
         * @param opacity value in range 0 to 1. 0 is invisible, 1 is full visible.
         * @return this for chaining.
         */
        @Override
        public TexturePolygon setOpacity(float opacity) {
                if (opacity < 0 || opacity > 1)
                        throw new IllegalArgumentException(opacity + " is an invalid value for opacity. Set opacity in range 0 to 1.");
                this.opacity = opacity;

                tmpColor.a = opacity;
                tmpColorAsFloatBits = tmpColor.toFloatBits();
                return this;

        }

        /**
         * Do not modify. Use {@link #setPosition(Vector2)} to set position.
         * The position decides how much to translate the polygon(defined by {@link #setVertices(Array)}) before drawing it.
         *
         * @return The position. Do not modify. Use {@link #setPosition(Vector2)} to set position.
         */
        @Override
        public Vector2 getPosition() {
                return position;
        }

        /**
         * @param position the position decides how much to translate the polygon(defined by {@link #setVertices(Array)}) before drawing it.
         * @return this for chaining.
         */
        @Override
        public TexturePolygon setPosition(Vector2 position) {
                this.position.set(position);
                return this;
        }

        /**
         * @param x decides how much to horizontally translate the polygon(defined by {@link #setVertices(Array)}) before drawing it.
         * @param y decides how much to vertically translate the polygon(defined by {@link #setVertices(Array)}) before drawing it.
         * @return this for chaining.
         */
        @Override
        public TexturePolygon setPosition(float x, float y) {
                this.position.set(x, y);
                return this;
        }

        /**
         * The polygon is rotated by angleRad radians before it is translated and drawn.
         *
         * @return how many radians the polygon is rotated.
         */
        @Override
        public float getAngleRad() {
                return angleRad;
        }

        /**
         * The polygon is rotated by angleRad radians before it is translated and drawn.
         *
         * @param angleRad how many radians to rotate the polygon.
         * @return this for chaining.
         */
        @Override
        public TexturePolygon setAngleRad(float angleRad) {
                this.angleRad = angleRad;
                return this;
        }

        /**
         * Scale decides how much to scale the polygon before drawing it. This also changes the
         * scale of the texture, such that if you increase the scale to make the polygon look larger the texture
         * also look like it is closer.
         *
         * @return how much the polygon is scaled.
         */
        @Override
        public float getScale() {
                return scale;
        }

        /**
         * Scale decides how much to scale the polygon before drawing it. This also changes the
         * scale of the texture, such that if you increase the scale to make the polygon look larger the texture
         * also look like it is closer.
         *
         * @param scale how much you want the polygon to be scaled.
         * @return this for chaining.
         */
        @Override
        public TexturePolygon setScale(float scale) {
                this.scale = scale;
                return this;
        }

        /**
         * When {@link #setVertices(Array)} is called it uses {@link Util#simplifyAndMakeTriangles(Array)} to calculate
         * the triangles that are used for drawing. If you need these triangles for anything (for example for box2d fixtures)
         * you can get them here instead of calculating them all over again.
         *
         * @return triangles whose union equals the polygon given by your last call to {@link #setVertices(Array)}.
         */
        public float[] getTriangles() {
                return triangles;
        }

        /**
         * Make new polygon regions.
         *
         * @param triangles one triangle for each polygon region.
         */
        private void setTriangles(float[] triangles) {
                if (textureRegion == null)
                        throw new RuntimeException("You must set the texture region before using this method. ");

                this.triangles = triangles;
                polygonRegions.clear();

                for (int i = 0; i < triangles.length; ) {


                        float[] triangle = new float[6];
                        for (int j = 0; j < triangle.length; j++) {
                                triangle[j] = (triangles[i++] / textureScale) * scale;
                        }

                        PolygonRegion polygonRegion = new PolygonRegion(
                                textureRegion,
                                triangle, fillingTriangle);


                        polygonRegions.add(polygonRegion);
                }
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
        public TexturePolygon setVisible(boolean visible) {
                this.visible = visible;
                return this;
        }

        @Override
        public boolean isVisible() {
                return visible;
        }
}
