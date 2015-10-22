package org.prettypaint;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 * Convenience class for aligning the textures of many {@link PolygonTexture}s.
 */
public class TextureAligner {
        private final Vector2 extraTranslation = new Vector2();

        public void alignTextures(Array<PolygonTexture> polygonTextures) {
                alignTextures(polygonTextures, true);
        }

        /**
         * Align the given textures so that they can seamlessly overlap.
         * @param polygonTextures      the textures to align.
         * @param textureAlwaysUpright whether to set the textures upright when calling {@link #alignTextures(Array)}.
         *                             If you set this value false the textures may not look aligned because they can have different angles.
         */
        public void alignTextures(Array<PolygonTexture> polygonTextures, boolean textureAlwaysUpright) {
                for (PolygonTexture polygonTexture : polygonTextures) {

                        if (textureAlwaysUpright)
                                polygonTexture.setTextureUprightForCurrentAngle();


                        polygonTexture.alignTexture(extraTranslation);

                }
        }

        /**
         * Align the given textures so that they can seamlessly overlap.
         * @param polygonTextures the textures to align.
         * @param textureAngleRad the angle to rotate the textures before aligning them.
         */
        public void alignTextures(Array<PolygonTexture> polygonTextures, float textureAngleRad) {
                for (PolygonTexture polygonTexture : polygonTextures) {

                        polygonTexture.setTextureAngleRad(textureAngleRad);

                        polygonTexture.alignTexture(extraTranslation);
                }
        }

        public Vector2 getExtraTranslation() {
                return extraTranslation;
        }

        public void setExtraTranslation(Vector2 extraTranslation) {
                this.extraTranslation.set(extraTranslation);
        }

        /**
         * T
         */
        public void setExtraTranslation(float x, float y) {
                this.extraTranslation.set(x, y);
        }

}

