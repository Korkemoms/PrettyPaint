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
 *
 */
public class PolygonBatch {

        protected final Rectangle frustum = new Rectangle();
        private final Mesh mesh;
        private final int dataPerVertex = 2 + 1 + 2 + 2 + 1;
        private final int maxData = 512 * 64;
        private final float[] commonVertices = new float[maxData];
        private final Vector2 tempVector = new Vector2();
        private final Color tempColor = new Color();
        protected Matrix4 worldView = new Matrix4();
        private ShaderProgram shaderProgram;
        private boolean drawDebugInfo = false;
        private int dataSet = 0;
        private boolean isStarted = false;
        private Texture lastTexture = null;
        private ShapeRenderer debugRenderer;
        protected Array<DebugDrawer> debugDrawingTasks = new Array<DebugDrawer>(true, 4, DebugDrawer.class);

        public PolygonBatch() {
                shaderProgram = new ShaderProgram(Shader.vertexShader, Shader.fragmentShader);

                if (!shaderProgram.isCompiled())
                        System.out.println("PolygonBatch shader-program not compiled!");

                Mesh.VertexDataType vertexDataType = Mesh.VertexDataType.VertexBufferObject;
                if (Gdx.gl30 != null) {
                        vertexDataType = Mesh.VertexDataType.VertexBufferObjectWithVAO;
                }

                mesh = new Mesh(vertexDataType, true, maxData, 0,
                        Shader.Attribute.position.vertexAttribute, // position of vertex
                        Shader.Attribute.colorOrScale.vertexAttribute, // are packed into one float
                        Shader.Attribute.textureTranslation.vertexAttribute, // texture translation, alpha values
                        Shader.Attribute.sourcePositionOrBoldness.vertexAttribute, // bottom left of textureRegion in texture, alpha values
                        Shader.Attribute.textureSizeAndShaderChooser.vertexAttribute // size of textureRegion(region must be square), alpha value. (<-0.5 when outline)
                );


        }

        public void dispose() {
                if (shaderProgram != null) shaderProgram.dispose();
                if (mesh != null) mesh.dispose();
                if (debugRenderer != null) debugRenderer.dispose();
        }

        public void begin(OrthographicCamera camera) {

                this.worldView.set(camera.combined);

                frustum.x = camera.position.x - camera.zoom * camera.viewportWidth / 2f;
                frustum.y = camera.position.y - camera.zoom * camera.viewportHeight / 2f;
                frustum.width = camera.viewportWidth * camera.zoom;
                frustum.height = camera.viewportHeight * camera.zoom;


                begin();

        }

        public void begin(Matrix4 worldView, Rectangle frustum) {
                this.frustum.set(frustum);
                this.worldView.set(worldView);
                begin();

        }

        private void begin(){
                isStarted = true;

                shaderProgram.begin();
                shaderProgram.setUniformMatrix("u_worldView", this.worldView);

                if (drawDebugInfo) {
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


        public void setDrawDebugInfo(boolean debugDraw) {
                this.drawDebugInfo = debugDraw;
        }

        public boolean isDrawingDebugInfo() {
                return drawDebugInfo;
        }

        protected void drawTexture(PolygonRegion region, float x, float y, float width, float height,
                                   float scaleX, float scaleY, float rotation, float pos_x, float pos_y, float trans_x, float trans_y, float srcWidth, float srcHeight, float opacityAndScale) {
                if (!isStarted) throw new RuntimeException("You must call begin() before calling this method.");


                if (srcWidth != srcHeight)
                        throw new IllegalArgumentException("srcWidth and srcHeight must be equal(only square textureRegioons allowed)");


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
                } else if (dataSet + totalData > maxData) {
                        flush();
                }

                final float[] textureCoords = region.getTextureCoords();

                final float worldOriginX = x;
                final float worldOriginY = y;
                final float sX = width / textureRegion.getRegionWidth();
                final float sY = height / textureRegion.getRegionHeight();
                final float cos = MathUtils.cos(rotation);
                final float sin = MathUtils.sin(rotation);

                float fx, fy;

                int i = 0;
                {
                        fx = (regionVertices[i] * sX) * scaleX;
                        fy = (regionVertices[i + 1] * sY) * scaleY;

                        commonVertices[dataSet++] = cos * fx - sin * fy + worldOriginX;
                        commonVertices[dataSet++] = sin * fx + cos * fy + worldOriginY;
                        commonVertices[dataSet++] = opacityAndScale;

                        float _x = textureCoords[i] + trans_x;
                        float _y = textureCoords[i + 1] + trans_y;

                        commonVertices[dataSet++] = _x;
                        commonVertices[dataSet++] = _y;

                        commonVertices[dataSet++] = pos_x;
                        commonVertices[dataSet++] = pos_y;

                        commonVertices[dataSet++] = srcWidth;
                }

                for (; i < regionVerticesLength; i += 2) {
                        fx = (regionVertices[i] * sX) * scaleX;
                        fy = (regionVertices[i + 1] * sY) * scaleY;

                        commonVertices[dataSet++] = cos * fx - sin * fy + worldOriginX;
                        commonVertices[dataSet++] = sin * fx + cos * fy + worldOriginY;
                        commonVertices[dataSet++] = opacityAndScale;

                        float _x = textureCoords[i] + trans_x;
                        float _y = textureCoords[i + 1] + trans_y;

                        commonVertices[dataSet++] = _x;
                        commonVertices[dataSet++] = _y;

                        commonVertices[dataSet++] = pos_x;
                        commonVertices[dataSet++] = pos_y;

                        commonVertices[dataSet++] = srcWidth;

                }
                {
                        i -= 2;
                        fx = (regionVertices[i] * sX) * scaleX;
                        fy = (regionVertices[i + 1] * sY) * scaleY;

                        commonVertices[dataSet++] = cos * fx - sin * fy + worldOriginX;
                        commonVertices[dataSet++] = sin * fx + cos * fy + worldOriginY;
                        commonVertices[dataSet++] = opacityAndScale;

                        float _x = textureCoords[i] + trans_x;
                        float _y = textureCoords[i + 1] + trans_y;

                        commonVertices[dataSet++] = _x;
                        commonVertices[dataSet++] = _y;

                        commonVertices[dataSet++] = pos_x;
                        commonVertices[dataSet++] = pos_y;

                        commonVertices[dataSet++] = srcWidth;
                }
        }


        protected void renderOutlines(Array<Float> vertexData, int begin, int end, Color c, float scale, float rotation, float translation_x, float translation_y, float weight) {
                if (!isStarted) throw new RuntimeException("You must call begin() before calling this method.");

                float colorAsFloatBits = c.toFloatBits();
                tempColor.set(c.r, c.g, c.b, 0f);
                float colorInvisibleAsFloatBits = tempColor.toFloatBits();

                float vertexCount = (end - begin) / 3f;

                float degenerateVertexCount = 2;

                float totalData = dataPerVertex * (vertexCount + degenerateVertexCount);

                if (dataSet + totalData > maxData) {
                        flush();
                }

                Vector2 pos = tempVector;

                pos.x = vertexData.items[begin] * scale;
                pos.y = vertexData.items[begin + 1] * scale;
                pos.rotateRad(rotation);
                pos.x += translation_x;
                pos.y += translation_y;


                dataSet = setOutlineVertexData(pos.x, pos.y, getColor(0, colorAsFloatBits, colorInvisibleAsFloatBits), dataSet, weight);


                for (int i = begin; i < end; ) {

                        pos.x = vertexData.items[i++] * scale;
                        pos.y = vertexData.items[i++] * scale;
                        pos.rotateRad(rotation);
                        pos.x += translation_x;
                        pos.y += translation_y;

                        dataSet = setOutlineVertexData(pos.x, pos.y, getColor(vertexData.items[i++], colorAsFloatBits, colorInvisibleAsFloatBits), dataSet, weight);

                }

                pos.x = vertexData.items[end - 3] * scale;
                pos.y = vertexData.items[end - 2] * scale;
                pos.rotateRad(rotation);
                pos.x += translation_x;
                pos.y += translation_y;


                dataSet = setOutlineVertexData(pos.x, pos.y, getColor(0, colorAsFloatBits, colorInvisibleAsFloatBits), dataSet, weight);

        }


        private int setOutlineVertexData(float x, float y, float colorAsFloatBits, int dataSet, float weight) {
                commonVertices[dataSet++] = x;
                commonVertices[dataSet++] = y;
                commonVertices[dataSet++] = colorAsFloatBits;
                commonVertices[dataSet++] = 0f;
                commonVertices[dataSet++] = 0f;
                commonVertices[dataSet++] = weight;
                commonVertices[dataSet++] = 0f;
                commonVertices[dataSet++] = -1f;

                return dataSet;
        }

        private float getColor(float alpha, float colorAsFloatBits, float colorInvisibleAsFloatBits) {
                if (alpha > 0) return colorAsFloatBits;
                return colorInvisibleAsFloatBits;
        }


        private void flush() {

                mesh.setVertices(commonVertices, 0, dataSet);



                Gdx.gl.glEnable(GL20.GL_BLEND);
                if (lastTexture != null) lastTexture.bind();
                mesh.render(shaderProgram, GL20.GL_TRIANGLE_STRIP, 0, dataSet / dataPerVertex);

                dataSet = 0;

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


                for (int i = debugDrawingTasks.size-1;i>=0;i--) {
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
