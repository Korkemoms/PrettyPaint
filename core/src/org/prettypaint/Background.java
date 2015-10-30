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



// TODO Clean and comment
public class Background {

        /**
         * You can modify this to set rotation. For some draw methods this value is ignored.
         */
        public float rotation = 0f;
        /**
         * You can modify this to set translation. For some draw methods this value is
         * ignored.
         */
        public final Vector2 translation = new Vector2();
        /**
         * You can modify this to set opacity. For some draw methods this value is
         * ignored.
         */
        public float opacity = 1f;
        /**
         * You can modify this to set texture translation.
         */
        public final Vector2 textureTranslation = new Vector2();


        public Array<PolygonRegion> polygonRegions = new Array<PolygonRegion>(true, 4, PolygonRegion.class);

        public final float baseScale;

        private TextureRegion textureRegion;

        private Array<Vector2> vertices;
        private Rectangle regionBounds;

        private final short[] fillingTriangle = new short[]{0, 1, 2};
        private float lastScale = 1f;
        private boolean debugDraw = false;
        private final Rectangle tmpRectangle = new Rectangle();

        private final Color tmpColor = new Color(1, 1, 1, 1);
        private float lastOpacity = 1f;
        private float tmpColorAsFloatBits = tmpColor.toFloatBits();


        private PrettyPolygonBatch.DebugRenderer debugRenderer = new PrettyPolygonBatch.DebugRenderer(null) {
                @Override
                public void draw(ShapeRenderer shapeRenderer) {
                        shapeRenderer.set(ShapeRenderer.ShapeType.Line);
                        shapeRenderer.setColor(Color.GREEN);
                        for (PolygonRegion pr : polygonRegions) {
                                getCullingArea(tmpRectangle, pr, rotation, translation, lastScale);
                                shapeRenderer.rect(tmpRectangle.x, tmpRectangle.y, tmpRectangle.width, tmpRectangle.height);
                        }
                }
        };

        public void alignTexture(float baseX, float baseY) {

                textureTranslation.set(baseX, baseY);

                textureTranslation.x *= RenderUtil.getTextureAlignmentConstantX(textureRegion);
                textureTranslation.y *= RenderUtil.getTextureAlignmentConstantY(textureRegion);

                textureTranslation.x %= RenderUtil.getMaximumTranslationX(textureRegion);
                textureTranslation.y %= RenderUtil.getMaximumTranslationY(textureRegion);

        }

        public Background(TextureRegion textureRegion) {
                this(textureRegion, 0.01f);
        }

        public Background(TextureRegion textureRegion, float baseScale) {

                this.baseScale = baseScale;
                setTextureRegion(textureRegion);

        }


        public void draw(PrettyPolygonBatch batch) {

                draw(batch, 1f, opacity);
        }


        public void draw(PrettyPolygonBatch batch, float opacity) {
                draw(batch, 1f, opacity);
        }


        public void draw(PrettyPolygonBatch batch, float scale, float opacity) {
                if (textureRegion == null) return;


                scale *= baseScale;
                lastScale = scale;


                Texture texture = textureRegion.getTexture();

                float textureWidth = texture.getWidth();
                float textureHeight = texture.getHeight();

                float srcWidth = textureRegion.getRegionWidth() / textureWidth;
                float srcHeight = textureRegion.getRegionHeight() / textureHeight;
                if (srcWidth != srcHeight)
                        throw new IllegalArgumentException("Texture width and height must be equal. :(");
                float tex_width_and_height = srcHeight;

                Rectangle frustum = batch.frustum;

                if (opacity != lastOpacity) {
                        lastOpacity = opacity;
                        tmpColor.a = opacity;
                        tmpColorAsFloatBits = tmpColor.toFloatBits();
                }


                for (int i = 0; i < polygonRegions.size; i++) {
                        PolygonRegion pr = polygonRegions.items[i];
                        getCullingArea(tmpRectangle, pr, rotation, translation, scale);

                        if (frustum.overlaps(tmpRectangle))
                                batch.drawTexture(pr,
                                        translation.x,
                                        translation.y,
                                        regionBounds.width,
                                        regionBounds.height,
                                        scale,
                                        scale,
                                        rotation,
                                        regionBounds.x / textureWidth,
                                        regionBounds.y / textureHeight,
                                        textureTranslation.x,
                                        textureTranslation.y,
                                        tex_width_and_height,
                                        tmpColorAsFloatBits
                                );
                }

        }


        public void setVertices(Array<Vector2> vertices, TRIANGULATION_TYPE triangulationType) {
                this.vertices = vertices;

                float[] triangles;
                if (triangulationType == TRIANGULATION_TYPE.PIZZA) {
                        Vector2 centroid = RenderUtil.polygonCentroid(vertices);

                        triangles = RenderUtil.makePizzaTriangles(vertices, centroid);
                } else {

                        triangles = RenderUtil.makeTriangles(vertices);
                }
                setTriangles(triangles);


        }

        public Array<Vector2> getVertices() {
                return vertices;
        }

        @Deprecated
        public void setTriangles(float[] triangles) {
                polygonRegions.clear();
                if (textureRegion == null) return;

                for (int i = 0; i < triangles.length; ) {


                        float[] triangle = new float[6];
                        for (int j = 0; j < triangle.length; j++) {
                                triangle[j] = triangles[i++];
                        }

                        for (int j = 0; j < triangle.length; j++) {
                                triangle[j] /= baseScale;
                        }

                        PolygonRegion polygonRegion = new PolygonRegion(
                                textureRegion,
                                triangle, fillingTriangle);

                        polygonRegions.add(polygonRegion);
                }
        }

        public final void setTextureRegion(TextureRegion textureRegion) {
                if (textureRegion == null) return;


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

                        textureTranslation.set(
                                (float) Math.random() * MathUtils.random() * RenderUtil.getMaximumTranslationX(textureRegion),
                                (float) Math.random() * MathUtils.random() * RenderUtil.getMaximumTranslationY(textureRegion));
                }
        }

        public TextureRegion getTextureRegion() {
                return textureRegion;
        }


        private final Vector2 tmp = new Vector2();
        private final Vector2 tmp1 = new Vector2();
        private final Vector2 tmp2 = new Vector2();

        private void getCullingArea(Rectangle cullingArea, PolygonRegion pr, float rotation, Vector2 translation, float scale) {

                float[] vertices = pr.getVertices();

                tmp.set(vertices[0] * scale, vertices[1] * scale);
                tmp.rotateRad(rotation);
                tmp.add(translation);
                cullingArea.set(tmp.x, tmp.y, 0, 0);

                tmp1.set(vertices[2] * scale, vertices[3] * scale);
                tmp1.rotateRad(rotation);
                tmp1.add(translation);
                cullingArea.merge(tmp1);

                tmp2.set(vertices[4] * scale, vertices[5] * scale);
                tmp2.rotateRad(rotation);
                tmp2.add(translation);
                cullingArea.merge(tmp2);


        }

        public void setDebugDraw(boolean debugDraw) {
                this.debugDraw = debugDraw;
        }

        public boolean isDebugDraw() {
                return debugDraw;
        }


        public enum TRIANGULATION_TYPE {
                PIZZA,
                POLY2TRI;
        }


}
