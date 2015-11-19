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

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;

/**
 * Created by Andreas on 18.11.2015.
 */
public class SceneUtil {
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

        public static void fillAndCenter(Stage stage, Table table) {
                table.setWidth(stage.getWidth());
                table.setHeight(stage.getHeight());

                float x = stage.getWidth() * 0.5f;
                float y = stage.getHeight() * 0.5f;

                table.setPosition(x, y, Align.center);
        }
}
