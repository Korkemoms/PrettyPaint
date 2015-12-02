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

package org.ams.prettypaint.def;

import com.badlogic.gdx.graphics.Color;
import org.ams.prettypaint.OutlinePolygon;

/**
 * The main purpose of these definitions is to make it easy so save and load things.
 *
 * @author Andreas
 */
public class OutlinePolygonDef extends PrettyPolygonDef {
        public boolean roundSharpCorners = true;
        public float halfWidth = 0.075f;
        public boolean drawInside = true;
        public boolean drawOutside = true;
        public float weight = 1;

        public boolean closedPolygon = true;
        public boolean drawBoundingBoxes = false;
        public boolean drawTriangleStrips = false;
        public boolean drawLineFromFirstToLast = false;

        public OutlinePolygonDef() {
                color.set(Color.BLACK);
        }

        public OutlinePolygonDef(OutlinePolygon outlinePolygon) {

                closedPolygon = outlinePolygon.isClosedPolygon();
                color.set(outlinePolygon.getColor());
                drawBoundingBoxes = outlinePolygon.isDrawingCullingRectangles();
                drawLineFromFirstToLast = outlinePolygon.isDrawingLineFromFirstToLast();
                drawTriangleStrips = outlinePolygon.isDrawingTriangleStrips();
                drawInside = outlinePolygon.isInsideDrawn();
                drawOutside = outlinePolygon.isOutsideDrawn();
                halfWidth = outlinePolygon.getHalfWidth();
                roundSharpCorners = outlinePolygon.isRoundingSharpCorners();
                weight = outlinePolygon.getWeight();

                angle = outlinePolygon.getAngle();
                opacity = outlinePolygon.getOpacity();
                position.set(outlinePolygon.getPosition());
                scale = outlinePolygon.getScale();
                vertices.addAll(outlinePolygon.getVertices());
                visible = outlinePolygon.isVisible();
        }


}
