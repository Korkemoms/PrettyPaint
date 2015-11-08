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

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;

/**
 * A {@link PrettyPolygon} add a DebugRenderer to {@link #debugRendererArray}.
 * It will then have its {@link #draw(ShapeRenderer)} method called after the
 * batch is done drawing its normal things.
 * <p>
 * This is a public class because GWT doesn't want to compile otherwise.
 */
public class DebugRenderer {
        final Array<DebugColor> debugColors = new Array<DebugColor>();

        public PrettyPolygon owner;

        public boolean enabled;
        private boolean thereHasBeenAnUpdate = false;

        public DebugRenderer() {

        }

        public DebugRenderer(PrettyPolygon owner) {
                this.owner = owner;
        }

        void draw(ShapeRenderer shapeRenderer) {

        }

        final Array<DebugColor> getDebugColors() {
                return debugColors;
        }

        final void queueIfEnabled(PrettyPolygonBatch batch) {
                if (!thereHasBeenAnUpdate) return;
                thereHasBeenAnUpdate = false;

                if (enabled) {
                        if (!batch.debugRendererArray.contains(this, true))
                                batch.debugRendererArray.add(this);
                } else {
                        batch.debugRendererArray.removeValue(this, true);
                }
        }

        /**
         * Remember to call super.update() when you override.
         */
        void update() {
                thereHasBeenAnUpdate = true;
        }

        public static class DebugColor {
                public final Color color;
                public CharSequence charSequence;

                public DebugColor(Color color, CharSequence charSequence) {
                        this.color = new Color(color);
                        this.charSequence = charSequence;
                }
        }
}
