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

package org.ams.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;

/**
 * @author Andreas
 */
public class CoordinateHelper {

        /**
         * @param screenX pixel.
         * @param screenY pixel.
         * @return position of touch in "world units", with center of screen as origin.
         */
        public Vector2 getCameraCoordinates(OrthographicCamera camera, int screenX, int screenY) {

                float halfWidth = Gdx.graphics.getWidth() * 0.5f;
                float halfHeight = Gdx.graphics.getHeight() * 0.5f;

                Vector2 pos = new Vector2(screenX - halfWidth, halfHeight - screenY);
                pos.scl(1f / halfWidth, 1f / halfHeight);

                pos.scl(camera.zoom);

                // convert to world units relative to center of screen
                pos.scl(camera.viewportWidth * 0.5f, camera.viewportHeight * 0.5f);

                return pos;
        }

        public static Vector2 getScreenCoordinates(OrthographicCamera camera, float cameraX, float cameraY, Vector2 result) {
                return getScreenCoordinates(camera, cameraX, cameraY, camera.zoom, result);
        }

        public static Vector2 getScreenCoordinates(OrthographicCamera camera, float cameraX, float cameraY, float zoom, Vector2 result) {


                float mouseXRelativeToScreenCenter = cameraX - camera.position.x;
                float mouseYRelativeToScreenCenter = cameraY - camera.position.y;

                mouseXRelativeToScreenCenter /= zoom;
                mouseYRelativeToScreenCenter /= zoom;

                mouseXRelativeToScreenCenter = mouseXRelativeToScreenCenter * 2 / camera.viewportWidth;
                mouseYRelativeToScreenCenter = mouseYRelativeToScreenCenter * 2 / camera.viewportHeight;

                float screenX = mouseXRelativeToScreenCenter * (Gdx.graphics.getWidth() / 2) + (float) Gdx.graphics.getWidth() / 2;
                float screenY = mouseYRelativeToScreenCenter * (Gdx.graphics.getHeight() / 2) + (float) Gdx.graphics.getHeight() / 2;

                result.set(screenX, screenY);
                return result;
        }


}
