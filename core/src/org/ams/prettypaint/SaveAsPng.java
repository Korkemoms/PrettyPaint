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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import org.ams.core.CoordinateHelper;

import java.nio.ByteBuffer;

/**
 * Very experimental.
 */
public class SaveAsPng {

        /**
         * Very experimental.
         * Save texture and outlines as png.
         *
         * @param texturePolygon  is drawn behind the outlines.
         * @param outlinePolygons outlines to save.
         * @param batch           batch for drawing.
         * @param fileName        name of the png, will be saved in assets folder.
         * @param quality         higher means larger resolution.
         */
        public static void saveAsPng(TexturePolygon texturePolygon, Array<OutlinePolygon> outlinePolygons, PrettyPolygonBatch batch, String fileName, float quality) {


                OrthographicCamera camera = new OrthographicCamera();


                Rectangle boundingRectangle = new Rectangle();
                boolean initialized = false;

                if (outlinePolygons != null) {
                        for (OutlinePolygon outlinePolygon : outlinePolygons) {
                                outlinePolygon.setPosition(0, 0);
                                outlinePolygon.setAngle(0);

                                if (!initialized) {
                                        initialized = true;
                                        boundingRectangle.set(outlinePolygon.getBoundingRectangle());
                                } else {
                                        boundingRectangle.merge(outlinePolygon.getBoundingRectangle());
                                }
                        }
                }

                if (texturePolygon != null) {
                        texturePolygon.setPosition(0, 0);
                        texturePolygon.setAngle(0);

                        if (!initialized) {
                                initialized = true;
                                boundingRectangle.set(texturePolygon.getBoundingRectangle());
                        } else
                                boundingRectangle.merge(texturePolygon.getBoundingRectangle());

                }
                if (!initialized) return;


                float w = Gdx.graphics.getWidth();
                float h = Gdx.graphics.getHeight();

                float dim = Math.max(boundingRectangle.width, boundingRectangle.height) * 2;

                camera.setToOrtho(false, 2, 2 * (h / w));
                camera.zoom = 0.5f + 1 / quality;

                camera.position.set(0, 0, camera.position.z);

                camera.update();

                Gdx.gl20.glClearColor(1, 1, 1, 0);
                Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
                batch.begin(camera);

                texturePolygon.draw(batch);
                batch.end();
                Pixmap one = getScreenshot(batch, camera, boundingRectangle);


                for (OutlinePolygon outlinePolygon : outlinePolygons) {
                        Gdx.gl20.glClearColor(1, 1, 1, 0);
                        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
                        batch.begin(camera);
                        outlinePolygon.draw(batch);
                        batch.end();
                        Pixmap two = getScreenshot(batch, camera, boundingRectangle);

                        one.drawPixmap(two, 0, 0);
                }

                int counter = 1;
                FileHandle fh;
                do {
                        fh = new FileHandle("PPImages/" + fileName + counter++ + ".png");
                } while (fh.exists());

                PixmapIO.writePNG(fh, one);

        }

        public static Pixmap getScreenshot(PrettyPolygonBatch batch, OrthographicCamera camera, Rectangle boundingRectangle) {

                Vector2 center = CoordinateHelper.getScreenCoordinates(
                        camera,
                        boundingRectangle.x + boundingRectangle.getWidth() * 0.5f,
                        boundingRectangle.y + boundingRectangle.getHeight() * 0.5f,
                        new Vector2());

                Vector2 upperRight = CoordinateHelper.getScreenCoordinates(
                        camera,
                        boundingRectangle.x + boundingRectangle.getWidth(),
                        boundingRectangle.y + boundingRectangle.getHeight(),
                        new Vector2());


                int halfWidth = (int) (upperRight.x - center.x);
                int halfHeight = (int) (upperRight.y - center.y);


                Pixmap pixmap = getScreenshot((int) (center.x - halfWidth), (int) (center.y - halfHeight), halfWidth * 2, halfHeight * 2, true);

                return pixmap;
        }

        public static Pixmap getScreenshot(int x, int y, int w, int h, boolean yDown) {

                Gdx.gl.glPixelStorei(GL20.GL_PACK_ALIGNMENT, 1);

                final Pixmap pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);
                ByteBuffer pixels = pixmap.getPixels();
                Gdx.gl.glReadPixels(x, y, w, h, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, pixels);

                if (yDown) {
                        // Flip the pixmap upside down
                        int numBytes = w * h * 4;
                        byte[] lines = new byte[numBytes];
                        int numBytesPerLine = w * 4;
                        for (int i = 0; i < h; i++) {
                                pixels.position((h - i - 1) * numBytesPerLine);
                                pixels.get(lines, i * numBytesPerLine, numBytesPerLine);
                        }
                        pixels.clear();
                        pixels.put(lines);
                        pixels.clear();
                }

                return pixmap;
        }
}
