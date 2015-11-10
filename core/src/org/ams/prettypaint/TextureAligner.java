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

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 * Convenience class for aligning the textures of many {@link TexturePolygon}s.
 */
public class TextureAligner {
        private final Vector2 extraTranslation = new Vector2();

        public void alignTextures(Array<TexturePolygon> polygonTextures) {
                alignTextures(polygonTextures, true);
        }

        /**
         * Align the given textures so that they can seamlessly overlap.
         *
         * @param polygonTextures      the textures to align.
         * @param textureAlwaysUpright whether to set the textures upright when calling {@link #alignTextures(Array)}.
         *                             If you set this value false the textures may not look aligned because they can have different angles.
         */
        public void alignTextures(Array<TexturePolygon> polygonTextures, boolean textureAlwaysUpright) {
                for (TexturePolygon texturePolygon : polygonTextures) {

                        if (textureAlwaysUpright)
                                texturePolygon.setTextureUprightForCurrentAngle();


                        texturePolygon.alignTexture(extraTranslation);

                }
        }

        /**
         * Align the given textures so that they can seamlessly overlap.
         *
         * @param polygonTextures the textures to align.
         * @param textureAngleRad the angle to rotate the textures before aligning them.
         */
        public void alignTextures(Array<TexturePolygon> polygonTextures, float textureAngleRad) {
                for (TexturePolygon texturePolygon : polygonTextures) {

                        texturePolygon.setTextureAngle( textureAngleRad-texturePolygon.getAngle());

                        texturePolygon.alignTexture(extraTranslation);
                }
        }

        // TODO Comment
        public Vector2 getExtraTranslation() {
                return extraTranslation;
        }

        // TODO Comment
        public void setExtraTranslation(Vector2 extraTranslation) {
                this.extraTranslation.set(extraTranslation);
        }

        // TODO Comment
        public void setExtraTranslation(float x, float y) {
                this.extraTranslation.set(x, y);
        }

}

