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

package org.ams.testapps.paintandphysics.cardhouse;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import org.ams.core.CoordinateHelper;
import org.ams.core.SceneUtil;
import org.ams.core.Timer;
import org.ams.prettypaint.OutlinePolygon;
import org.ams.prettypaint.PrettyPolygonBatch;

public class Tips {

        private boolean debug = false;

        // for drawing arrows
        private OrthographicCamera arrowCamera;
        private PrettyPolygonBatch polygonBatch;
        private Array<OutlinePolygon> arrows = new Array<OutlinePolygon>();

        private Stage stage;
        private Skin skin;

        // for remembering completed tips
        private Preferences preferences;

        private Array<Runnable> queuedTips = new Array<Runnable>();
        private boolean anyTipVisible = false;

        private long delay = 2000;
        private Timer timer = new Timer();


        private Runnable onResize; // used to lay out actors again

        private Window window;


        public Tips(String key, Stage stage, Skin skin) {
                if (debug) Gdx.app.setLogLevel(Application.LOG_DEBUG);
                if (debug) debug("Creating a new Tips instance with key" + key + ".");

                this.stage = stage;
                this.skin = skin;

                // for remembering completed tips
                preferences = Gdx.app.getPreferences(key + "Tips");

                // for drawing arrows
                polygonBatch = new PrettyPolygonBatch();

                // camera
                float w = Gdx.graphics.getWidth();
                float h = Gdx.graphics.getHeight();
                arrowCamera = new OrthographicCamera(10, 10 * (h / w));
                updateArrowCamera();
        }

        private void debug(String text) {
                if (debug) Gdx.app.log("Tips", text);
        }

        /** Uncomplete all tips. Removes any visible tip. */
        public void reset() {
                if (debug) debug("Resetting.");

                queuedTips.clear();
                timer.clear();
                anyTipVisible = false;
                arrows.clear();
                stage.getActors().removeValue(window, true);

                onResize = null;

                preferences.clear();
                preferences.flush();

        }

        /** Call whenever screen size changes. */
        public void resize(int width, int height) {
                updateArrowCamera();
                if (onResize != null)
                        onResize.run();

        }

        /**
         * Dispose all resources and nullify references.
         * Must be called when this object is no longer used.
         */
        public void dispose() {
                if (debug) debug("Disposing Tips instance.");

                arrowCamera = null;

                if (polygonBatch != null) polygonBatch.dispose();
                polygonBatch = null;

                arrows = null;
                stage = null;
                skin = null;
                queuedTips = null;
                onResize = null;

                preferences = null;

        }

        /** The buttons and text is drawn with the rest of the stage. */
        public void drawArrows() {
                polygonBatch.begin(arrowCamera);
                for (OutlinePolygon outlinePolygon : arrows) {
                        outlinePolygon.draw(polygonBatch);
                }
                polygonBatch.end();

                timer.step();
        }

        /** Show the tip first in the queue. */
        private void showNextTip() {
                if (queuedTips.size == 0) {
                        anyTipVisible = false;
                        onResize = null;
                        return;
                }
                anyTipVisible = true;


                queuedTips.first().run();
                queuedTips.removeIndex(0);
        }

        /**
         * When a tip becomes first in the queue
         * it is displayed after {@link #getDelay()} milliseconds.
         */
        public void setDelay(long delay) {
                this.delay = delay;
        }

        /**
         * When a tip becomes first in the queue
         * it is displayed after {@link #getDelay()} milliseconds.
         */
        public long getDelay() {
                return delay;
        }

        /** Whether a tip is currently displayed on the screen. */
        public boolean isAnyTipVisible() {
                return anyTipVisible;
        }

        /** The number of tips waiting to be shown. */
        public int getQueueSize() {
                return queuedTips.size;
        }

        /**
         * If a user clicks "Got it" the tip is forever completed. To uncomplete all tips
         * call {@link #reset()}.
         */
        public boolean isTipCompleted(String key) {
                return preferences.getBoolean(key, false);
        }


        /**
         * If this tip is not completed it is queued for showing. When it becomes first in the queue
         * it is displayed after {@link #getDelay()} milliseconds.
         * <p/>
         * See {@link #setDelay(long)}.
         *
         * @param key     String for remembering whether to display this tips in the feature.
         * @param text    The tips.
         * @param arrowTo If not null an arrow will point to this location.
         */
        public void queueTip(final String key, final String text, final ChangingVector arrowTo) {
                boolean tipComleted = isTipCompleted(key);
                if (debug) {
                        if (tipComleted) {
                                debug("Not Queueing tooltip with key " + key + ".");
                        } else {
                                debug("Queueing tooltip with key " + key + ".");
                        }
                }
                if (tipComleted) return;


                Runnable tip = new Runnable() {
                        @Override
                        public void run() {

                                timer.runOnRender(new Runnable() {
                                        long begin = System.currentTimeMillis();

                                        @Override
                                        public void run() {
                                                long now = System.currentTimeMillis();
                                                if (now > begin + delay) {
                                                        timer.remove(this);
                                                        showTip(key, text, arrowTo);
                                                }
                                        }
                                });


                        }
                };

                queuedTips.add(tip);

                if (!anyTipVisible)
                        showNextTip();

        }

        /**
         * Instantly show a tip on the screen.
         *
         * @param key     String for remembering whether to display this tips in the feature.
         * @param text    The tips.
         * @param arrowTo If not null an arrow will point to this location.
         */
        private void showTip(final String key, final String text, final ChangingVector arrowTo) {
                if (debug) debug("Showing tip with key " + key + ".");

                final Array<OutlinePolygon> arrow = new Array<OutlinePolygon>();

                stage.getActors().removeValue(window, true);


                // update this tip on resize
                Tips.this.onResize = new Runnable() {
                        @Override
                        public void run() {

                                arrows.removeAll(arrow, true);
                                stage.getActors().removeValue(window, true);
                                showTip(key, text, arrowTo);
                        }
                };


                // prepare text table
                Label label = new Label(text, skin);
                label.setColor(Color.BLACK);

                Table textTable = new Table();
                textTable.add(label);


                // prepare button table
                TextButton hide = new TextButton("Hide", skin);
                hide.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                                stage.getActors().removeValue(window, true);
                                arrows.removeAll(arrow, true);
                                showNextTip();
                        }
                });

                TextButton ok = new TextButton("Got It", skin);
                ok.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                                stage.getActors().removeValue(window, true);
                                arrows.removeAll(arrow, true);
                                preferences.putBoolean(key, true);
                                preferences.flush();
                                showNextTip();
                        }
                });

                Table buttonTable = new Table();
                buttonTable.add(hide).pad(SceneUtil.getPreferredPadding(stage));
                buttonTable.add(ok).pad(SceneUtil.getPreferredPadding(stage));


                // put it all in table
                Table mainTable = new Table();

                mainTable.add(textTable).row();
                mainTable.add(buttonTable);

                // prepare arrow
                if (arrowTo != null) {
                        float sw = stage.getWidth();
                        float sh = stage.getHeight();

                        // figure out where to start the arrow
                        Vector2 arrowFrom = new Vector2(mainTable.getPrefWidth() * 0.5f, 0);
                        Vector2 v = new Vector2(arrowTo.get()).sub(sw * 0.5f, sh * 0.5f);
                        arrowFrom.rotateRad(v.angleRad());

                        arrowFrom.add(sw * 0.5f, sh * 0.5f);
                        arrowFrom.scl(1, -1).add(0, sh);

                        // convert to world coordinates
                        Vector2 screenCoordinates = stage.stageToScreenCoordinates(arrowFrom);
                        Vector2 from = CoordinateHelper.getWorldCoordinates(arrowCamera, screenCoordinates.x, screenCoordinates.y);


                        // we know where to end the arrow, just convert to world coordinates
                        screenCoordinates = stage.stageToScreenCoordinates(new Vector2(arrowTo.get()).scl(1, -1).add(0, sh));
                        Vector2 to = CoordinateHelper.getWorldCoordinates(arrowCamera, screenCoordinates.x, screenCoordinates.y);

                        // make and save arrow
                        arrow.addAll(makeArrow(from, to));
                        arrows.addAll(arrow);
                }


                window = new Window("", skin);
                window.setBackground(new TextureRegionDrawable(skin.getRegion("gray")));
                window.add(mainTable).center();

                window.setSize(mainTable.getPrefWidth(),mainTable.getPrefHeight());


                // center on stage
                float x = stage.getWidth() * 0.5f;
                float y = stage.getHeight() * 0.5f;
                window.setPosition(x, y, Align.center);

                stage.addActor(window);
        }

        /** Update the camera used to draw the arrows. */
        private void updateArrowCamera() {
                float width = Gdx.graphics.getWidth();
                float height = Gdx.graphics.getHeight();

                arrowCamera.setToOrtho(true, 10, 10 * (height / width));
                arrowCamera.zoom = MathUtils.clamp(width / height, 0.25f, 1);
                arrowCamera.update();
        }

        /** Make some OutlinePolygons that form an arrow. */
        private Array<OutlinePolygon> makeArrow(Vector2 from, Vector2 to) {
                if (debug) debug("Creating arrow from " + from + " to " + to + ".");

                Array<OutlinePolygon> arrow = new Array<OutlinePolygon>();


                { // make the shaft of the arrow
                        Array<Vector2> vertices = new Array<Vector2>();
                        vertices.add(from);
                        vertices.add(to);
                        OutlinePolygon outlinePolygon = new OutlinePolygon();
                        outlinePolygon.setVertices(vertices);
                        outlinePolygon.setClosedPolygon(false);
                        arrow.add(outlinePolygon);
                }
                { // make the tip of the arrow
                        Array<Vector2> vertices = new Array<Vector2>();

                        Vector2 walk = new Vector2(to).sub(from).nor().scl(0.4f);

                        Vector2 one = new Vector2(walk).rotate(150).add(to);
                        Vector2 two = new Vector2(walk).scl(0.05f).add(to);
                        Vector2 three = new Vector2(walk).rotate(-150).add(to);


                        vertices.add(one);
                        vertices.add(two);
                        vertices.add(three);


                        OutlinePolygon outlinePolygon = new OutlinePolygon();
                        outlinePolygon.setVertices(vertices);
                        outlinePolygon.setClosedPolygon(false);
                        arrow.add(outlinePolygon);
                }

                return arrow;
        }


        public interface ChangingVector {
                Vector2 get();
        }
}
