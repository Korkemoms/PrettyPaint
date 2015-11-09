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

package org.ams.paintandphysics.things;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import org.ams.core.CoordinateHelper;
import org.ams.physics.things.JointThing;
import org.ams.physics.things.Polygon;
import org.ams.physics.things.Thing;
import org.ams.physics.things.ThingWithBody;
import org.ams.physics.things.def.DefParser;
import org.ams.prettypaint.OutlinePolygon;
import org.ams.prettypaint.PrettyPolygonBatch;
import org.ams.prettypaint.TexturePolygon;
import org.ams.prettypaint.def.OutlinePolygonDef;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * The PPBasic combines a {@link Thing} with a {@link TexturePolygon} and {@link OutlinePolygon}'s.
 * It does not need to have any or all of these members, you can choose which you want to use.
 * It can have several {@link OutlinePolygon}'s.
 * <p/>
 * There are convenient methods for updating properties of all the things at once, like
 * {@link #setOpacity(float)}.
 * <p/>
 * <p/>
 * There are no methods for getting the properties however, as they may be different
 * for each polygon.
 */
class PPBasic implements PPThing {

        protected Thing physicsThing;

        protected TexturePolygon texturePolygon;

        protected Array<OutlinePolygon> outlinePolygons = new Array<OutlinePolygon>();

        /**
         * When using this constructor you must manually set the {@link TexturePolygon}, {@link Polygon}
         * and {@link OutlinePolygon}'s you wish to use. All are optional.
         */
        public PPBasic() {
                this(null, null);
        }

        /**
         * Initialize from definition, no {@link com.badlogic.gdx.graphics.g2d.TextureRegion} is set
         * for the {@link TexturePolygon} when using this constructor.
         *
         * @param def the definition that sets this polygons properties.
         */
        public PPBasic(PPThingDef def) {
                this(def, null);
        }

        /**
         * Initialize from definition. The atlas is needed to find the {@link com.badlogic.gdx.graphics.g2d.TextureRegion}
         * specified in the {@link org.ams.prettypaint.def.TexturePolygonDef}.
         * <p/>
         * All the definition attributes in the {@link PPThingDef} are optional.
         *
         * @param def   the definition that sets this polygons properties.
         * @param atlas the atlas that contains the texture specified.
         */
        public PPBasic(PPThingDef def, TextureAtlas atlas) {
                if (def != null) {
                        if (def.texturePolygonDef != null)
                                texturePolygon = new TexturePolygon(def.texturePolygonDef, atlas);

                        if (def.outlinePolygonDefinitions != null) {
                                for (OutlinePolygonDef outlinePolygonDef : def.outlinePolygonDefinitions) {
                                        outlinePolygons.add(new OutlinePolygon(outlinePolygonDef));
                                }
                        }
                        if (def.physicsThingDefinition != null) {

                                physicsThing = DefParser.definitionToThing(def.physicsThingDefinition);
                        }
                }
        }

        /**
         * The {@link TexturePolygon} is drawn first.
         *
         * @param batch batch for drawing.
         * @return this for chaining.
         */
        public PPBasic draw(PrettyPolygonBatch batch) {
                Vector2 newPosition = null;
                float newAngle = 0;

                if (physicsThing != null) {
                        if (physicsThing instanceof ThingWithBody) {
                                ThingWithBody twb = (ThingWithBody) physicsThing;
                                newPosition = twb.getInterpolatedPosition();
                                newAngle = twb.getInterpolatedAngle();
                        } else if (physicsThing instanceof JointThing) {
                                JointThing jt = (JointThing) physicsThing;
                        }
                }


                if (texturePolygon != null) {
                        if (newPosition != null) {
                                texturePolygon.setPosition(newPosition);
                                texturePolygon.setAngle(newAngle);
                        }
                        texturePolygon.draw(batch);
                }

                for (OutlinePolygon outlinePolygon : outlinePolygons) {
                        if (newPosition != null) {
                                outlinePolygon.setPosition(newPosition);
                                outlinePolygon.setAngle(newAngle);
                        }
                        outlinePolygon.draw(batch);
                }

                return this;
        }

        /**
         * @param batch    batch for drawing.
         * @param fileName name of the png, will be saved in assets folder.
         * @param quality  higher means larger resolution.
         * @return this for chaining.
         */
        public PPBasic saveAsPng(PrettyPolygonBatch batch, String fileName, float quality) {
                texturePolygon.setPosition(0, 0);
                for (OutlinePolygon outlinePolygon : outlinePolygons) {
                        outlinePolygon.setPosition(0, 0);
                }


                OrthographicCamera camera = new OrthographicCamera();

                Rectangle boundingRectangle = outlinePolygons.first().getBoundingRectangle();

                float w = Gdx.graphics.getWidth();
                float h = Gdx.graphics.getHeight();

                float dim = Math.max(boundingRectangle.width, boundingRectangle.height) * 2;

                camera.setToOrtho(false, dim, dim * (h / w));
                camera.zoom = 0.5f + 1 / quality;

                camera.position.set(0, 0,
                        camera.position.z);

                camera.update();

                Gdx.gl20.glClearColor(1, 1, 1, 0);
                Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
                batch.begin(camera);

                texturePolygon.draw(batch);
                batch.end();
                Pixmap one = okok(batch, camera, boundingRectangle);


                for (OutlinePolygon outlinePolygon : outlinePolygons) {
                        Gdx.gl20.glClearColor(1, 1, 1, 0);
                        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
                        batch.begin(camera);
                        outlinePolygon.draw(batch);
                        batch.end();
                        Pixmap two = okok(batch, camera, boundingRectangle);

                        one.drawPixmap(two, 0, 0);
                }

                FileHandle fh;
                do {
                        fh = new FileHandle("PPTextures/" + "PPTexture" + counter++ + ".png");
                } while (fh.exists());

                PixmapIO.writePNG(fh, one);

                return this;
        }



        private Pixmap okok(PrettyPolygonBatch batch, OrthographicCamera camera, Rectangle boundingRectangle) {

                Vector2 center = CoordinateHelper.getScreenCoordinates(
                        camera,
                        boundingRectangle.x + boundingRectangle.getWidth() * 0.5f,
                        boundingRectangle.y + boundingRectangle.getHeight() * 0.5f,
                        new Vector2());

                Vector2 upperRight = CoordinateHelper.getScreenCoordinates(
                        camera,
                        boundingRectangle.x + boundingRectangle.getWidth(),
                        boundingRectangle.y + boundingRectangle.getHeight(),
                        new Vector2());


                int half = (int) (upperRight.x - center.x);

                Pixmap pixmap = getScreenshot((int) (center.x - half), (int) (center.y - half), half * 2, half * 2, true);

                return pixmap;
        }


        private static int counter = 1;

        private static Pixmap getScreenshot(int x, int y, int w, int h, boolean yDown) {

                Gdx.gl.glPixelStorei(GL20.GL_PACK_ALIGNMENT, 1);

                final Pixmap pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);
                ByteBuffer pixels = pixmap.getPixels();
                Gdx.gl.glReadPixels(x, y, w, h, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, pixels);

                if (yDown) {
                        // Flip the pixmap upside down
                        int numBytes = w * h * 4;
                        byte[] lines = new byte[numBytes];
                        int numBytesPerLine = w * 4;
                        for (int i = 0; i < h; i++) {
                                pixels.position((h - i - 1) * numBytesPerLine);
                                pixels.get(lines, i * numBytesPerLine, numBytesPerLine);
                        }
                        pixels.clear();
                        pixels.put(lines);
                        pixels.clear();
                }

                return pixmap;
        }

        /**
         * Set the position of the painting polygons and the physics thing if it has a body.
         *
         * @param x coordinate.
         * @param y coordinate.
         * @return this for chaining.
         */
        public PPBasic setPosition(float x, float y) {
                if (physicsThing != null && physicsThing.hasBody()) {
                        ThingWithBody twb = (ThingWithBody) physicsThing;

                        twb.setTransform(x, y, twb.getAngle());
                }
                if (texturePolygon != null) {
                        texturePolygon.setPosition(x, y);
                }
                for (OutlinePolygon outlinePolygon : outlinePolygons) {
                        outlinePolygon.setPosition(x, y);
                }
                return this;
        }


        /**
         * Set the angle of the painting polygons and the physics thing if it has a body.
         *
         * @param radians angle in radians.
         * @return this for chaining.
         */
        public PPBasic setAngle(float radians) {
                if (physicsThing != null && physicsThing.hasBody()) {
                        ThingWithBody twb = (ThingWithBody) physicsThing;

                        Vector2 bodyPos = twb.getPosition();
                        twb.setTransform(bodyPos.x, bodyPos.y, radians);
                }
                if (texturePolygon != null) {
                        texturePolygon.setAngle(radians);
                }
                for (OutlinePolygon outlinePolygon : outlinePolygons) {
                        outlinePolygon.setAngle(radians);
                }

                return this;
        }

        /**
         * Set the scale of the {@link OutlinePolygon}'s and the {@link TexturePolygon}.
         *
         * @param scale the painting scale.
         * @return this for chaining.
         */
        public PPBasic setScale(float scale) {
                if (texturePolygon != null)
                        texturePolygon.setScale(scale);
                for (OutlinePolygon outlinePolygon : outlinePolygons) {
                        outlinePolygon.setScale(scale);
                }
                return this;
        }

        /**
         * Set the opacity of the {@link OutlinePolygon}'s and the {@link TexturePolygon}.
         *
         * @param opacity the painting opacity.
         * @return this for chaining.
         */
        public PPBasic setOpacity(float opacity) {
                if (texturePolygon != null)
                        texturePolygon.setOpacity(opacity);
                for (OutlinePolygon outlinePolygon : outlinePolygons) {
                        outlinePolygon.setOpacity(opacity);
                }
                return this;
        }

        /**
         * Set the visibility of the {@link OutlinePolygon}'s and the {@link TexturePolygon}.
         *
         * @param visible whether to draw the polygons.
         * @return this for chaining.
         */
        public PPBasic setVisible(boolean visible) {
                if (texturePolygon != null)
                        texturePolygon.setVisible(visible);
                for (OutlinePolygon outlinePolygon : outlinePolygons) {
                        outlinePolygon.setVisible(visible);
                }
                return this;
        }

        @Override
        public Array<OutlinePolygon> getOutlinePolygons() {
                return outlinePolygons;
        }

        @Override
        public TexturePolygon getTexturePolygon() {
                return texturePolygon;
        }

        @Override
        public Thing getPhysicsThing() {
                return physicsThing;
        }

        @Override
        public PPBasic setTexturePolygon(TexturePolygon texturePolygon) {
                this.texturePolygon = texturePolygon;
                return this;
        }

        @Override
        public PPBasic setPhysicsThing(Thing physicsThing) {
                this.physicsThing = physicsThing;
                return this;
        }
}
