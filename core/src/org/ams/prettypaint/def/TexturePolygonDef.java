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

import com.badlogic.gdx.math.Vector2;
import org.ams.prettypaint.TexturePolygon;

/**
 * The main purpose of these definitions is to make it easy so save and load things.
 *
 * @author Andreas
 */
public class TexturePolygonDef extends PrettyPolygonDef{
        public final Vector2 textureTranslation = new Vector2();
        public float textureAngle = 0;
        public String textureRegionName;
        public float textureScale = 0.01f;
        public boolean drawCullingRectangles = false;

        public TexturePolygonDef(){

        }

        public TexturePolygonDef(TexturePolygon texturePolygon){

                textureAngle = texturePolygon.getAngle();
                textureRegionName = texturePolygon.getTextureRegionName();
                textureScale = texturePolygon.getTextureScale();
                textureTranslation.set(texturePolygon.getTextureTranslation());

                angle = texturePolygon.getAngle();
                opacity = texturePolygon.getOpacity();
                position.set(texturePolygon.getPosition());
                scale = texturePolygon.getScale();
                vertices.addAll(texturePolygon.getVertices());
                visible = texturePolygon.isVisible();

        }

}
