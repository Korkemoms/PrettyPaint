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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ams.prettypaint;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.Array;

/**
 * Use together with {@link OutlinePolygon]s and {@link TexturePolygon }}s.
 * The batch accumulates data and flushes it to the gpu in large portions,
 * this is faster than sending smaller portions of data more often.
 */
public class PrettyPolygonBatch {

        /**
         * The frustum is set every time one of the begin methods are called.
         * The Polygons then check if they overlap with this rectangle before drawing.
         */
        public final Rectangle frustum = new Rectangle();

        /**
         * When enough data is accumulated or another texture is given in a draw call
         * all the data is then handed over to this mesh. The mesh then sends the data
         * to the gpu for drawing.
         */
        private final Mesh mesh;

        /**
         * This number is currently very large, i hope to learn some tricks so that
         * i can get by with less data per vertex.
         */
        private final int dataPerVertex = 2 + 1 + 2 + 2 + 2;

        /** Max data before we must flush. */
        private final int maxData = 512 * 64;

        /** Data from the draw calls are accumulated in this array. */
        private final float[] data = new float[maxData];

        private final Vector2 tempVector = new Vector2();

        private final Color tempColor = new Color();

        /** I save the worldview for debugging, */
        protected Matrix4 worldView = new Matrix4();

        private ShaderProgram shaderProgram;
        private boolean drawFrustum = false;

        /** How much data currently stored in {@link #data}. */
        private int dataCount = 0;

        public boolean isStarted = false;
        private Texture lastTexture = null;

        /** Draws lines and shapes used for debugging. */
        private ShapeRenderer shapeRenderer;

        /** Font for debugging. */
        private BitmapFont debugFont;

        /** Draws text used for debugging. */
        private SpriteBatch debugSpriteBatch;

        /** Used to inspect the frustum culling. */
        private float frustumScale = 1f;

        private DebugRenderer debugRenderer;

        private Array<Color> debugColorsTaken = new Array<Color>();

        /**
         * When other classes want to draw something for debugging purposes they can add
         * a {@link DebugRenderer} to this array. These will be drawn when you call {@link #end()}.
         */
        protected Array<DebugRenderer> debugRendererArray = new Array<DebugRenderer>(true, 4, DebugRenderer.class);

        private OrthographicCamera debugCamera;


        /**
         * For every frame: call one of the begin methods, then pass this batch to a {@link PrettyPolygon}s draw method. After you have done
         * that with all your Polygons you call {@link #end()};
         */
        public PrettyPolygonBatch() {


                debugRenderer = new DebugRenderer() {


                        @Override
                        void draw(ShapeRenderer shapeRenderer) {
                                drawFrustum(shapeRenderer, debugColors.first().color);
                        }

                        @Override
                        void update() {
                                super.update();
                                boolean enabled = drawFrustum;
                                boolean change = enabled != this.enabled;
                                if (!change) return;
                                this.enabled = enabled;

                                debugColors.clear();
                                if (drawFrustum) {
                                        debugColors.add(new DebugColor(Color.CYAN, "Frustum"));
                                }

                                if (!enabled) {
                                        debugRendererArray.removeValue(this, true);
                                }
                        }
                };


                shaderProgram = new ShaderProgram(Shader.vertexShader, Shader.fragmentShader);

                if (!shaderProgram.isCompiled())
                        Gdx.app.log("PrettyPolygonBatch", "PrettyPolygonBatch shader-program not compiled!");

                Mesh.VertexDataType vertexDataType = Mesh.VertexDataType.VertexBufferObject;
                if (Gdx.gl30 != null) {
                        vertexDataType = Mesh.VertexDataType.VertexBufferObjectWithVAO;
                }

                mesh = new Mesh(vertexDataType, true, maxData, 0,
                        Shader.Attribute.position.vertexAttribute, // position of vertex
                        Shader.Attribute.colorOrJustOpacity.vertexAttribute, // are packed into one float
                        Shader.Attribute.positionInRegion.vertexAttribute, // texture translation, alpha values
                        Shader.Attribute.regionPositionOrBoldness.vertexAttribute, // bottom left of textureRegionName in texture, alpha values
                        Shader.Attribute.regionSizeAndShaderChooser.vertexAttribute // size of textureRegionName(region must be square), alpha value. (<-0.5 when outline)
                );
        }

        /**
         * Dispose the resources associated with this batch. Must be called when the batch is no longer used.
         */
        public void dispose() {
                if (shaderProgram != null) shaderProgram.dispose();
                if (mesh != null) mesh.dispose();
                if (shapeRenderer != null) shapeRenderer.dispose();
                if (debugFont != null) debugFont.dispose();
                if (debugSpriteBatch != null) debugSpriteBatch.dispose();
        }

        /**
         * You must call begin before you can use this batch to draw stuff.
         *
         * @param camera the camera tells me where to draw things, it also defines
         *               the frustum, everything that is outside the frustum is not drawn.
         */
        public void begin(OrthographicCamera camera) {

                this.worldView.set(camera.combined);

                frustum.x = camera.position.x - camera.zoom * camera.viewportWidth / 2f;
                frustum.y = camera.position.y - camera.zoom * camera.viewportHeight / 2f;
                frustum.width = camera.viewportWidth * camera.zoom;
                frustum.height = camera.viewportHeight * camera.zoom;


                begin();

        }

        /**
         * You must call begin before you can use this batch to draw stuff.
         *
         * @param worldView this matrix tells me where to draw things.
         * @param frustum   everything that is outside the frustum is not drawn.
         */
        public void begin(Matrix4 worldView, Rectangle frustum) {
                this.frustum.set(frustum);
                this.worldView.set(worldView);

                begin();

        }

        private void begin() {
                isStarted = true;

                debugRenderer.queueIfEnabled(this);

                shaderProgram.begin();
                shaderProgram.setUniformMatrix("u_worldView", this.worldView);

                if (frustumScale != 1f) {
                        float addX = this.frustum.width * (frustumScale - 1);
                        float addY = this.frustum.height * (frustumScale - 1);

                        this.frustum.x -= addX * 0.5f;
                        this.frustum.y -= addY * 0.5f;
                        this.frustum.width += addX;
                        this.frustum.height += addY;

                }
        }

        // TODO Comment
        public void end() {
                isStarted = false;
                flush();
                shaderProgram.end();
                doAllDebugDrawing();

        }

        /**
         * For debugging.
         * This can be used to verify that the frustum culling is working.
         *
         * @return the frustum scaling factor.
         */
        public float getFrustumScale() {
                return frustumScale;
        }

        /**
         * For debugging.
         * This can be used to verify that the frustum culling is working.
         *
         * @param frustumScale the new frustum scaling factor.
         */
        public void setFrustumScale(float frustumScale) {
                this.frustumScale = frustumScale;
        }

        /**
         * For debugging.
         * This can be used to verify that the frustum culling is working.
         * See also {@link #setFrustumScale(float)}.
         *
         * @param drawFrustum whether to draw an outline showing the frustum.
         */
        public void setDrawFrustum(boolean drawFrustum) {
                this.drawFrustum = drawFrustum;
                debugRenderer.update();
        }

        /**
         * For debugging.
         * This can be used to verify that the frustum culling is working.
         *
         * @return whether the frustum is drawn.
         */
        public boolean isDrawingFrustum() {
                return drawFrustum;
        }

        // TODO Comment
        protected void drawTexture(PolygonRegion region, float pos_x, float pos_y, float width, float height,
                                   float scaleX, float scaleY, float rotation, float texture_pos_x, float texture_pos_y, float tex_trans_x,
                                   float tex_trans_y, float region_width, float region_height, float packedColor) {
                if (!isStarted) throw new RuntimeException("You must call begin() before calling this method.");

                final float[] regionVertices = region.getVertices();
                final int regionVerticesLength = regionVertices.length;
                final TextureRegion textureRegion = region.getRegion();

                Texture texture = textureRegion.getTexture();

                float vertexCount = 3f;

                float degenerateVertexCount = 2f;

                float totalData = dataPerVertex * (vertexCount + degenerateVertexCount);

                if (texture != lastTexture) {
                        flush();
                        lastTexture = texture;
                } else if (dataCount + totalData > maxData) {
                        flush();
                }

                final float[] textureCoordinates = region.getTextureCoords();

                final float worldOriginX = pos_x;
                final float worldOriginY = pos_y;
                final float sX = width / textureRegion.getRegionWidth();
                final float sY = height / textureRegion.getRegionHeight();
                final float cos = MathUtils.cos(rotation);
                final float sin = MathUtils.sin(rotation);

                float fx, fy;

                int i = 0;
                {
                        fx = (regionVertices[i] * sX) * scaleX;
                        fy = (regionVertices[i + 1] * sY) * scaleY;

                        data[dataCount++] = cos * fx - sin * fy + worldOriginX;
                        data[dataCount++] = sin * fx + cos * fy + worldOriginY;
                        data[dataCount++] = packedColor;

                        data[dataCount++] = textureCoordinates[i] + tex_trans_x;
                        data[dataCount++] = textureCoordinates[i + 1] + tex_trans_y;

                        data[dataCount++] = texture_pos_x;
                        data[dataCount++] = texture_pos_y;

                        data[dataCount++] = region_width;
                        data[dataCount++] = region_height;

                }

                for (; i < regionVerticesLength; i += 2) {
                        fx = (regionVertices[i] * sX) * scaleX;
                        fy = (regionVertices[i + 1] * sY) * scaleY;

                        data[dataCount++] = cos * fx - sin * fy + worldOriginX;
                        data[dataCount++] = sin * fx + cos * fy + worldOriginY;
                        data[dataCount++] = packedColor;

                        data[dataCount++] = textureCoordinates[i] + tex_trans_x;
                        data[dataCount++] = textureCoordinates[i + 1] + tex_trans_y;

                        data[dataCount++] = texture_pos_x;
                        data[dataCount++] = texture_pos_y;

                        data[dataCount++] = region_width;
                        data[dataCount++] = region_height;


                }


                {
                        i -= 2;
                        fx = (regionVertices[i] * sX) * scaleX;
                        fy = (regionVertices[i + 1] * sY) * scaleY;

                        data[dataCount++] = cos * fx - sin * fy + worldOriginX;
                        data[dataCount++] = sin * fx + cos * fy + worldOriginY;
                        data[dataCount++] = packedColor;

                        data[dataCount++] = textureCoordinates[i] + tex_trans_x;
                        data[dataCount++] = textureCoordinates[i + 1] + tex_trans_y;

                        data[dataCount++] = texture_pos_x;
                        data[dataCount++] = texture_pos_y;

                        data[dataCount++] = region_width;
                        data[dataCount++] = region_height;
                }
        }

        /**
         * Used by OutlinePolygon to draw triangle strips. These triangle strips
         * form anti-aliased polygon outlines.
         *
         * @param stripVertices
         * @param inside
         * @param begin         the index in {@code vertexData} to begin at(inclusive)
         * @param end           the index in {@code vertexData} to stop at(not inclusive)
         * @param color         the color of the outline
         * @param scale         how much to scale the outline
         * @param angleRad      how much to rotate before drawing
         * @param translation_x how much to translate the outline horizontally
         * @param translation_y how much to translate the outline vertically
         * @param weight        the weight of the outline(higher gives bolder edges)
         */
        protected void drawOutline(Array<OutlinePolygon.StripVertex> stripVertices, boolean closed, boolean inside, int begin, int end, int total, Color color, float scale, float angleRad,
                                   float translation_x, float translation_y, float weight) {
                if (!isStarted) throw new RuntimeException("You must call begin() before calling this method.");

                // this color is used for all the user vertices
                float colorAsFloatBits = color.toFloatBits();

                tempColor.set(color.r, color.g, color.b, 0f);
                // this color is used for all the aux vertices
                float colorInvisibleAsFloatBits = tempColor.toFloatBits();

                Vector2 pos = tempVector;

                // fixing a problem i don't understand:
                if (!closed && end >= stripVertices.size) {
                        end = stripVertices.size;
                }

                int amountOfDataThisTime = (2 + total) * dataPerVertex;
                if (dataCount + amountOfDataThisTime > maxData) {
                        flush();
                }


                {

                        OutlinePolygon.StripVertex stripVertex = stripVertices.items[begin];
                        Array<Float> vertexData = inside ? stripVertex.insideVertexData : stripVertex.outsideVertexData;


                        pos.x = vertexData.items[0] * scale;
                        pos.y = vertexData.items[1] * scale;
                        pos.rotateRad(angleRad);
                        pos.x += translation_x;
                        pos.y += translation_y;

                        dataCount = setOutlineVertexData(pos.x, pos.y, colorInvisibleAsFloatBits, dataCount, weight);
                }


                for (int i = begin; i < end; i++) {

                        int k = i % stripVertices.size;

                        OutlinePolygon.StripVertex stripVertex = stripVertices.items[k];
                        Array<Float> vertexData = inside ? stripVertex.insideVertexData : stripVertex.outsideVertexData;

                        // fixing a problem i don't understand:
                        int n = i >= end - 1 ? 4 : vertexData.size;
                        if (!closed && i >= stripVertices.size - 1 && (end - begin) > 1) n = vertexData.size;

                        for (int j = 0; j < n; ) {
                                pos.x = vertexData.items[j++] * scale;
                                pos.y = vertexData.items[j++] * scale;
                                pos.rotateRad(angleRad);
                                pos.x += translation_x;
                                pos.y += translation_y;

                                float _colorAsFloatBits = getColor(vertexData.items[j++], colorAsFloatBits, colorInvisibleAsFloatBits);

                                dataCount = setOutlineVertexData(pos.x, pos.y, _colorAsFloatBits, dataCount, weight);
                        }
                }

                { // degenerate in order to travel from the previous vertex without drawing anything

                        dataCount = setOutlineVertexData(pos.x, pos.y, colorInvisibleAsFloatBits, dataCount, weight);
                }


        }

        /** Append the information about this vertex to the data array. */
        private int setOutlineVertexData(float x, float y, float colorAsFloatBits, int dataCount, float weight) {
                // so much overhead :(
                data[dataCount++] = x;
                data[dataCount++] = y;
                data[dataCount++] = colorAsFloatBits;
                data[dataCount++] = 0f;
                data[dataCount++] = 0f;
                data[dataCount++] = weight;
                data[dataCount++] = 0f;
                data[dataCount++] = -1f;
                data[dataCount++] = 0f;


                return dataCount;
        }

        /**
         * When drawing aux vertices the color should be the same as the color of the user vertex it belongs to, except
         * for the opacity which should be 0.
         */
        private float getColor(float VERTEX_TYPE, float colorAsFloatBits, float colorInvisibleAsFloatBits) {
                if (VERTEX_TYPE == OutlinePolygon.VERTEX_TYPE_USER) return colorAsFloatBits;
                return colorInvisibleAsFloatBits;
        }


        /** Send all the accumulated data to the gpu. */
        private void flush() {
                mesh.setVertices(data, 0, dataCount);

                Gdx.gl.glEnable(GL20.GL_BLEND);

                if (lastTexture != null) lastTexture.bind();
                mesh.render(shaderProgram, GL20.GL_TRIANGLE_STRIP, 0, dataCount / dataPerVertex);

                dataCount = 0;

        }


        private void doAllDebugDrawing() {
                if (debugRendererArray.size == 0) return;

                if (shapeRenderer == null) {
                        shapeRenderer = new ShapeRenderer();
                        shapeRenderer.setAutoShapeType(true);
                }

                if (debugSpriteBatch == null) {
                        debugSpriteBatch = new SpriteBatch();
                }

                if (debugFont == null) {
                        debugFont = new BitmapFont();
                }

                if (debugCamera == null) {
                        debugCamera = new OrthographicCamera();
                }


                shapeRenderer.begin();

                // draw the debug stuff
                shapeRenderer.setProjectionMatrix(worldView);
                long now = System.currentTimeMillis();
                for (int i = debugRendererArray.size - 1; i >= 0; i--) {
                        DebugRenderer debugRenderer = debugRendererArray.items[i];

                        debugRenderer.draw(shapeRenderer);

                        // if it is more than one second since the last time
                        // the owner of this debugRenderer did any normal drawing
                        // then we stop the debug rendering of it

                        if (debugRenderer.owner != null && debugRenderer.owner.getTimeOfLastDrawCall() + 1000 < now) {
                                debugRendererArray.removeIndex(i);
                        }
                }

                // draw short colored lines that will be beside some text explaining what the color of the line means
                debugCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                debugCamera.position.set(
                        Gdx.graphics.getWidth() * 0.5f,
                        Gdx.graphics.getHeight() * -0.5f + 10,
                        debugCamera.position.z);

                debugCamera.update();
                shapeRenderer.setProjectionMatrix(debugCamera.combined);

                debugColorsTaken.clear();

                float verticalSpacing = 20;

                int n = 0;
                for (DebugRenderer debugRenderer : debugRendererArray) {
                        Array<DebugRenderer.DebugColor> colors = debugRenderer.getDebugColors();

                        for (DebugRenderer.DebugColor debugColor : colors) {
                                if (debugColorsTaken.contains(debugColor.color, false)) continue;
                                debugColorsTaken.add(debugColor.color);

                                float y = n++ * -verticalSpacing - 5;
                                shapeRenderer.setColor(debugColor.color);
                                shapeRenderer.line(10, y, 25, y);
                        }
                }

                shapeRenderer.end();

                debugSpriteBatch.begin();
                debugSpriteBatch.setProjectionMatrix(debugCamera.combined);

                // draw explanatory text
                n = 0;
                for (DebugRenderer debugRenderer : debugRendererArray) {
                        Array<DebugRenderer.DebugColor> colors = debugRenderer.getDebugColors();

                        for (DebugRenderer.DebugColor debugColor : colors) {
                                if (!debugColorsTaken.contains(debugColor.color, true)) continue;

                                drawText(debugColor.charSequence, 30, n++ * -verticalSpacing);
                        }
                }

                debugSpriteBatch.end();

        }

        private void drawText(CharSequence text, float x, float y) {
                debugFont.draw(debugSpriteBatch, text, x, y);
        }

        private void drawFrustum(ShapeRenderer shapeRenderer, Color color) {
                shapeRenderer.set(ShapeRenderer.ShapeType.Line);
                shapeRenderer.setColor(color);
                shapeRenderer.rect(frustum.x, frustum.y, frustum.width, frustum.height);
        }
}
