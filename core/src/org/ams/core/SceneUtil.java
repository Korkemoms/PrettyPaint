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
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.SnapshotArray;

/**
 * Created by Andreas on 18.11.2015.
 */
public class SceneUtil {

        /** Compute reasonable button width, for all densities and screen sizes. */
        public static float getPreferredButtonWidth(Stage stage) {
                float preferred = Math.min(stage.getWidth(), stage.getHeight()) * 0.3f;

                float minimum = 250f;
                if (preferred < minimum)
                        preferred = minimum;

                return preferred;
        }

        /** Compute reasonable padding for buttons and such, for all densities and screen sizes. */
        public static float getPreferredPadding(Stage stage) {
                float preferred = Math.min(stage.getWidth(), stage.getHeight()) * 0.005f;

                float minimum = (float) Math.sqrt(Gdx.graphics.getDensity()) * 2f;
                if (preferred < minimum) preferred = minimum;

                return preferred;
        }

        /** Swap actors inside cells. */
        public static void swapActors(Cell cell, Cell cell1) {
                Actor actor = cell.getActor();
                Actor actor1 = cell1.getActor();

                cell.clearActor();
                cell1.clearActor();

                cell.setActor(actor1);
                cell1.setActor(actor);
        }


        public static Rectangle getActorBounds(Actor actor, Rectangle result) {

                result.width = actor.getWidth();
                result.height = actor.getHeight();

                result.x = getX(0, actor);
                result.y = getY(0, actor);
                return result;
        }

        public static float getX(float accumulated, Actor actor) {
                accumulated += actor.getX();
                Actor parent = actor.getParent();
                if (parent == null) return accumulated;
                return getX(accumulated, parent);
        }

        public static float getY(float accumulated, Actor actor) {
                accumulated += actor.getY();
                Actor parent = actor.getParent();
                if (parent == null) return accumulated;
                return getY(accumulated, parent);
        }

        public static void fillAndCenter(Stage stage, Actor actor) {
                actor.setWidth(stage.getWidth());
                actor.setHeight(stage.getHeight());

                float x = stage.getWidth() * 0.5f;
                float y = stage.getHeight() * 0.5f;

                actor.setPosition(x, y, Align.center);

        }

        public static void confirm(final Stage stage, Skin skin, String text, String okText, String cancelText, final ConfirmCallback callback) {

                final Dialog dialog = new Dialog("", skin);
                dialog.setBackground(new TextureRegionDrawable(skin.getRegion("gray")));
                dialog.setModal(true);


                Table mainTable = new Table();

                // text
                Label label = new Label(text, skin);
                label.setColor(Color.BLACK);
                mainTable.add(label).row();

                // buttons
                Table buttonTable = new Table();

                TextButton okButton = new TextButton(okText, skin);
                buttonTable.add(okButton);
                okButton.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                                stage.getActors().removeValue(dialog, true);
                                if (callback != null) callback.confirm(true);
                        }
                });

                TextButton cancelButton = new TextButton(cancelText, skin);
                buttonTable.add(cancelButton);
                cancelButton.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                                stage.getActors().removeValue(dialog, true);
                                if (callback != null) callback.confirm(false);
                        }
                });

                mainTable.add(buttonTable);


                fillAndCenter(stage, dialog);
                dialog.add(mainTable).width(stage.getWidth()).height(stage.getHeight());

                stage.addActor(dialog);
        }

        public interface ConfirmCallback {
                void confirm(boolean confirmed);
        }

        public static boolean traverseChildren(Actor actor, TraverseTask task) {
                boolean _continue = task.run(actor);
                if (!_continue) return false;

                if (!(actor instanceof WidgetGroup)) return true;

                WidgetGroup group = (WidgetGroup) actor;
                SnapshotArray<Actor> children = group.getChildren();

                for (Actor child : children) {
                        _continue = traverseChildren(child, task);
                        if (!_continue) return false;
                }
                return true;
        }

        public interface TraverseTask {
                boolean run(Actor actor);
        }


}
