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
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.Vector2;

/**
 * Created by Andreas on 09.11.2015.
 */
public class CameraNavigator extends InputAdapter implements GestureDetector.GestureListener {

        private OrthographicCamera camera;

        // some variables used for controlling the camera
        private Vector2 lastWorldTouchDown;
        private Vector2 cameraPositionAtLastWorldTouch;
        private boolean active = true;

        private GestureDetector gestureDetector;

        // variables used for pinch zooming
        private float originalZoom;
        private Vector2 _initialPointer1 = new Vector2();
        private Vector2 _initialPointer2 = new Vector2();
        private Vector2 v2 = new Vector2();


        public CameraNavigator(OrthographicCamera camera) {
                this.camera = camera;
                gestureDetector = new GestureDetector(this);
        }

        public void setActive(boolean active) {
                this.active = active;
        }

        public boolean isActive() {
                return active;
        }

        @Override
        public boolean keyDown(int keycode) {
                gestureDetector.keyDown(keycode);
                return false;
        }

        @Override
        public boolean keyUp(int keycode) {
                gestureDetector.keyUp(keycode);
                return false;
        }

        @Override
        public boolean keyTyped(char character) {
                gestureDetector.keyTyped(character);
                return false;
        }

        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                gestureDetector.touchDown(screenX, screenY, pointer, button);

                if (!active) return false;
                lastWorldTouchDown = getPositionOfTouch(screenX, screenY);
                cameraPositionAtLastWorldTouch = new Vector2(camera.position.x, camera.position.y);
                return true;
        }

        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                gestureDetector.touchUp(screenX, screenY, pointer, button);

                boolean b = lastWorldTouchDown != null;
                lastWorldTouchDown = null;
                return active && b;
        }

        @Override
        public boolean touchDragged(int screenX, int screenY, int pointer) {
                gestureDetector.touchDragged(screenX, screenY, pointer);

                if (cameraPositionAtLastWorldTouch == null)
                        return false;

                if (lastWorldTouchDown == null) return false;

                if (!active) return false;

                Vector2 worldTouchDrag = getPositionOfTouch(screenX, screenY);

                Vector2 pos = new Vector2(cameraPositionAtLastWorldTouch).sub(worldTouchDrag).add(lastWorldTouchDown);

                camera.position.set(pos.x, pos.y, camera.position.z);
                camera.update();

                return true;
        }

        @Override
        public boolean scrolled(int amount) {
                gestureDetector.scrolled(amount);

                if (!active) return false;
                setZoom(camera.zoom * (1 + amount * 0.1f), Gdx.input.getX(), Gdx.input.getY());

                return true;
        }

        public void setZoom(float zoom, Vector2 pos) {
                setZoom(zoom, pos.x, pos.y);
        }

        /**
         * @param zoom    the cameras zoom will be set to this value.
         * @param screenX the screen x-coordinate you want to zoom in on(or away from).
         * @param screenY the screen y-coordinate you want to zoom in on(or away from).
         */
        public void setZoom(float zoom, float screenX, float screenY) {
                Vector2 touchWorldBefore = getPositionOfTouch(screenX, screenY);
                touchWorldBefore.add(camera.position.x, camera.position.y);

                camera.zoom = zoom;
                camera.update();

                Vector2 touchWorldAfter = getPositionOfTouch(screenX, screenY);
                touchWorldAfter.add(camera.position.x, camera.position.y);

                camera.translate(touchWorldBefore.sub(touchWorldAfter));
                camera.update();
        }

        /**
         * @param screenX pixel.
         * @param screenY pixel.
         * @return position of touch in "world units", with center of screen as origin.
         */
        public Vector2 getPositionOfTouch(float screenX, float screenY) {

                float halfWidth = Gdx.graphics.getWidth() * 0.5f;
                float halfHeight = Gdx.graphics.getHeight() * 0.5f;

                Vector2 pos = new Vector2(screenX - halfWidth, halfHeight - screenY);
                pos.scl(1f / halfWidth, 1f / halfHeight);

                pos.scl(camera.zoom);

                // convert to world units relative to center of screen
                pos.scl(camera.viewportWidth * 0.5f, camera.viewportHeight * 0.5f);

                return pos;
        }

        @Override
        public boolean mouseMoved(int screenX, int screenY) {
                gestureDetector.mouseMoved(screenX, screenY);

                return false;
        }

        @Override
        public boolean touchDown(float x, float y, int pointer, int button) {

                return false;
        }

        @Override
        public boolean tap(float x, float y, int count, int button) {
                return false;
        }

        @Override
        public boolean longPress(float x, float y) {
                return false;
        }

        @Override
        public boolean fling(float velocityX, float velocityY, int button) {
                return false;
        }

        @Override
        public boolean pan(float x, float y, float deltaX, float deltaY) {
                return false;
        }

        @Override
        public boolean panStop(float x, float y, int pointer, int button) {
                return false;
        }

        @Override
        public boolean zoom(float initialDistance, float distance) {
                return false;
        }

        @Override
        public boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2) {

                if (!_initialPointer1.equals(initialPointer1)) {
                        _initialPointer1.set(initialPointer1);
                        originalZoom = camera.zoom;
                }
                if (!_initialPointer2.equals(initialPointer2)) {
                        _initialPointer2.set(initialPointer2);
                        originalZoom = camera.zoom;
                }
                setZoom(originalZoom * (initialPointer1.dst(initialPointer2)) / (pointer1.dst(pointer2)),
                        v2.set(pointer1).add(pointer2).scl(0.5f));
                return true;
        }
}
