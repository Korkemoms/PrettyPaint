/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.prettypaint;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.Array;

/**
 * Use together with {@link OutlinePolygon]s, {@link TexturePolygon }}s and {@link Background}s to draw
 * things. The batch accumulates data and flushes it to the gpu in large portions, this is faster than
 * sending smaller portions of data more often.
 */
public class PrettyPolygonBatch {

        /**
         * The frustum is set ever time one of the begin methods are called.
         * The Polygons then check if they overlap with this rectangle before drawing.
         */
        protected final Rectangle frustum = new Rectangle();

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
        private final int dataPerVertex = 2 + 1 + 2 + 2 + 1;

        /** Max data before we must flush. */
        private final int maxData = 512 * 64;

        /** Data from the draw calls are accumulated in this array. */
        private final float[] data = new float[maxData];

        private final Vector2 tempVector = new Vector2();
        private final Color tempColor = new Color();

        /** I save the worldview for debugging, */
        protected Matrix4 worldView = new Matrix4();

        private ShaderProgram shaderProgram;
        private boolean drawDebugInfo = false;

        /** How much data currently stored in {@link #data}. */
        private int dataCount = 0;

        private boolean isStarted = false;
        private Texture lastTexture = null;

        /** Draws lines and shapes used for debugging. */
        private ShapeRenderer debugRenderer;

        private boolean shrinkFrustumForDebugDraw;

        /**
         * When other classes want to draw something for debugging purposes they can add
         * a {@link DebugDrawer} to this array. These will be drawn when you call {@link #end()}.
         */
        protected Array<DebugDrawer> debugDrawingTasks = new Array<DebugDrawer>(true, 4, DebugDrawer.class);

        /**
         * For every frame: call one of the begin methods, then pass this batch to a {@link PrettyPolygon}s draw method. After you have done
         * that with all your Polygons you call {@link #end()};
         */
        public PrettyPolygonBatch() {
                shaderProgram = new ShaderProgram(Shader.vertexShader, Shader.fragmentShader);

                if (!shaderProgram.isCompiled())
                        System.out.println("PrettyPolygonBatch shader-program not compiled!");

                Mesh.VertexDataType vertexDataType = Mesh.VertexDataType.VertexBufferObject;
                if (Gdx.gl30 != null) {
                        vertexDataType = Mesh.VertexDataType.VertexBufferObjectWithVAO;
                }

                mesh = new Mesh(vertexDataType, true, maxData, 0,
                        Shader.Attribute.position.vertexAttribute, // position of vertex
                        Shader.Attribute.colorOrJustOpacity.vertexAttribute, // are packed into one float
                        Shader.Attribute.originInTexture.vertexAttribute, // texture translation, alpha values
                        Shader.Attribute.sourcePositionOrBoldness.vertexAttribute, // bottom left of textureRegion in texture, alpha values
                        Shader.Attribute.textureSizeAndShaderChooser.vertexAttribute // size of textureRegion(region must be square), alpha value. (<-0.5 when outline)
                );
        }

        /**
         * Dispose the resources associated with this batch. Must be called when the batch is no longer used.
         */
        public void dispose() {
                if (shaderProgram != null) shaderProgram.dispose();
                if (mesh != null) mesh.dispose();
                if (debugRenderer != null) debugRenderer.dispose();
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

                shaderProgram.begin();
                shaderProgram.setUniformMatrix("u_worldView", this.worldView);

                if (drawDebugInfo && shrinkFrustumForDebugDraw) {
                        float padding = (float) Math.sqrt(this.frustum.area()) * 0.1f;
                        this.frustum.x += padding;
                        this.frustum.y += padding;
                        this.frustum.width -= 2f * padding;
                        this.frustum.height -= 2f * padding;

                }
        }

        public void end() {
                isStarted = false;
                flush();
                shaderProgram.end();
                doAllDebugDrawing();

        }

        /**
         * This can be used to verify that the frustum culling is working.
         *
         * @return whether the frustum is being shrunk when debug drawing.
         */
        public boolean isShrinkingFrustumForDebugDraw() {
                return shrinkFrustumForDebugDraw;
        }

        /**
         * This can be used to verify that the frustum culling is working.
         *
         * @param shrinkFrustumForDebugDraw whether to shrink the frustum when debug drawing.
         */
        public void setShrinkFrustumForDebugDraw(boolean shrinkFrustumForDebugDraw) {
                this.shrinkFrustumForDebugDraw = shrinkFrustumForDebugDraw;
        }

        /**
         * This can be used to manually inspect some functionality.
         * For now it only shows information about the frustum.
         *
         * @param debugDraw whether to draw debug information.
         */
        public void setDrawDebugInfo(boolean debugDraw) {
                this.drawDebugInfo = debugDraw;
        }

        /**
         * This can be used to manually inspect some functionality.
         * For now it only shows information about the frustum.
         *
         * @return whether debug information is being drawn.
         */
        public boolean isDrawingDebugInfo() {
                return drawDebugInfo;
        }

        protected void drawTexture(PolygonRegion region, float pos_x, float pos_y, float width, float height,
                                   float scaleX, float scaleY, float rotation, float texture_pos_x, float texture_pos_y, float tex_trans_x,
                                   float tex_trans_y, float tex_width_and_height, float opacity) {
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
                        data[dataCount++] = opacity;

                        data[dataCount++] = textureCoordinates[i] + tex_trans_x;
                        data[dataCount++] = textureCoordinates[i + 1] + tex_trans_y;

                        data[dataCount++] = texture_pos_x;
                        data[dataCount++] = texture_pos_y;

                        data[dataCount++] = tex_width_and_height;
                }

                for (; i < regionVerticesLength; i += 2) {
                        fx = (regionVertices[i] * sX) * scaleX;
                        fy = (regionVertices[i + 1] * sY) * scaleY;

                        data[dataCount++] = cos * fx - sin * fy + worldOriginX;
                        data[dataCount++] = sin * fx + cos * fy + worldOriginY;
                        data[dataCount++] = opacity;

                        data[dataCount++] = textureCoordinates[i] + tex_trans_x;
                        data[dataCount++] = textureCoordinates[i + 1] + tex_trans_y;

                        data[dataCount++] = texture_pos_x;
                        data[dataCount++] = texture_pos_y;

                        data[dataCount++] = tex_width_and_height;

                }


                {
                        i -= 2;
                        fx = (regionVertices[i] * sX) * scaleX;
                        fy = (regionVertices[i + 1] * sY) * scaleY;

                        data[dataCount++] = cos * fx - sin * fy + worldOriginX;
                        data[dataCount++] = sin * fx + cos * fy + worldOriginY;
                        data[dataCount++] = opacity;

                        data[dataCount++] = textureCoordinates[i] + tex_trans_x;
                        data[dataCount++] = textureCoordinates[i + 1] + tex_trans_y;

                        data[dataCount++] = texture_pos_x;
                        data[dataCount++] = texture_pos_y;

                        data[dataCount++] = tex_width_and_height;
                }
        }

        /**
         * Used by OutlinePolygon to draw triangle strips. These triangle strips
         * form anti-aliased polygon outlines.
         *
         * @param vertexData    [x, y, VERTEX_TYPE, x, y, VERTEX_TYPE, ...]
         * @param begin         the index in {@code vertexData} to begin at(inclusive)
         * @param end           the index in {@code vertexData} to stop at(not inclusive)
         * @param color         the color of the outline
         * @param scale         how much to scale the outline
         * @param angleRad      how much to rotate before drawing
         * @param translation_x how much to translate the outline horizontally
         * @param translation_y how much to translate the outline vertically
         * @param weight        the weight of the outline(higher gives bolder edges)
         */
        protected void drawOutline(Array<Float> vertexData, int begin, int end, Color color, float scale, float angleRad,
                                   float translation_x, float translation_y, float weight) {
                if (!isStarted) throw new RuntimeException("You must call begin() before calling this method.");

                // this color is used for all the user vertices
                float colorAsFloatBits = color.toFloatBits();

                tempColor.set(color.r, color.g, color.b, 0f);
                // this color is used for all the aux vertices
                float colorInvisibleAsFloatBits = tempColor.toFloatBits();

                float vertexCount = (end - begin) / 3f;
                float degenerateVertexCount = 2;
                float totalData = dataPerVertex * (vertexCount + degenerateVertexCount);

                if (dataCount + totalData > maxData) {
                        flush();
                }

                Vector2 pos = tempVector;

                pos.x = vertexData.items[begin] * scale;
                pos.y = vertexData.items[begin + 1] * scale;
                pos.rotateRad(angleRad);
                pos.x += translation_x;
                pos.y += translation_y;

                // degenerate in order to travel from the previous vertex without drawing anything
                dataCount = setOutlineVertexData(pos.x, pos.y, colorInvisibleAsFloatBits, dataCount, weight);


                for (int i = begin; i < end; ) {

                        pos.x = vertexData.items[i++] * scale;
                        pos.y = vertexData.items[i++] * scale;
                        pos.rotateRad(angleRad);
                        pos.x += translation_x;
                        pos.y += translation_y;

                        // the vertex type is either VERTEX_TYPE_USER(1)
                        // or VERTEX_TYPE_AUX(0)
                        float VERTEX_TYPE = vertexData.items[i++];

                        // when its an aux vertex the opacity is set to 0
                        float _colorAsFloatBits = getColor(VERTEX_TYPE, colorAsFloatBits, colorInvisibleAsFloatBits);

                        dataCount = setOutlineVertexData(pos.x, pos.y, _colorAsFloatBits, dataCount, weight);

                }

                pos.x = vertexData.items[end - 3] * scale;
                pos.y = vertexData.items[end - 2] * scale;
                pos.rotateRad(angleRad);
                pos.x += translation_x;
                pos.y += translation_y;

                // degenerate in order to travel to the next vertex without drawing anything
                dataCount = setOutlineVertexData(pos.x, pos.y, colorInvisibleAsFloatBits, dataCount, weight);

        }

        /** Append the information about this vertex to the data array. */
        private int setOutlineVertexData(float x, float y, float colorAsFloatBits, int dataSet, float weight) {
                // so much overhead :(
                data[dataSet++] = x;
                data[dataSet++] = y;
                data[dataSet++] = colorAsFloatBits;
                data[dataSet++] = 0f;
                data[dataSet++] = 0f;
                data[dataSet++] = weight;
                data[dataSet++] = 0f;
                data[dataSet++] = -1f;

                return dataSet;
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
                if (debugDrawingTasks.size == 0 && !drawDebugInfo) return;

                if (debugRenderer == null) {
                        debugRenderer = new ShapeRenderer();
                        debugRenderer.setAutoShapeType(true);
                }


                debugRenderer.begin();
                debugRenderer.setProjectionMatrix(worldView);

                if (drawDebugInfo) {
                        debugDraw(debugRenderer);
                }


                for (int i = debugDrawingTasks.size - 1; i >= 0; i--) {
                        debugDrawingTasks.items[i].draw(debugRenderer);

                }


                debugRenderer.end();

        }

        private void debugDraw(ShapeRenderer shapeRenderer) {
                shapeRenderer.set(ShapeRenderer.ShapeType.Line);
                shapeRenderer.setColor(Color.CYAN);
                shapeRenderer.rect(frustum.x, frustum.y, frustum.width, frustum.height);
        }
}
