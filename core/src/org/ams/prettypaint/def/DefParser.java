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

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import org.ams.prettypaint.OutlinePolygon;
import org.ams.prettypaint.PrettyPolygon;
import org.ams.prettypaint.TexturePolygon;

/**
 * The main purpose of these definitions is to make it easy so save and load things.
 *
 * @author Andreas
 */
public class DefParser {

        private static final Json json = new Json();


        public static Array<PrettyPolygonDef> prettyPolygonsToDefinitions(Array<PrettyPolygon> polygons) {
                Array<PrettyPolygonDef> definitions = new Array<PrettyPolygonDef>();
                for (PrettyPolygon polygon : polygons) {
                        definitions.add(prettyPolygonToDefinition(polygon));
                }
                return definitions;
        }

        public static Array<PrettyPolygon> definitionsToPrettyPolygons(Array<PrettyPolygonDef> defs, TextureAtlas atlas) {
                Array<PrettyPolygon> prettyPolygons = new Array<PrettyPolygon>();
                for (PrettyPolygonDef def : defs) {
                        prettyPolygons.add(definitionToPrettyPolygon(def, atlas));
                }
                return prettyPolygons;
        }

        public static PrettyPolygon definitionToPrettyPolygon(PrettyPolygonDef prettyPolygonDef, TextureAtlas textureAtlas) {
                if (prettyPolygonDef instanceof OutlinePolygonDef) {
                        return new OutlinePolygon((OutlinePolygonDef) prettyPolygonDef);
                } else if (prettyPolygonDef instanceof TexturePolygonDef) {
                        return new TexturePolygon((TexturePolygonDef) prettyPolygonDef, textureAtlas);
                }
                throw new IllegalArgumentException("Unknown definition.");
        }

        public static PrettyPolygonDef prettyPolygonToDefinition(PrettyPolygon prettyPolygon) {
                if (prettyPolygon instanceof OutlinePolygon) {
                        return new OutlinePolygonDef((OutlinePolygon) prettyPolygon);
                } else if (prettyPolygon instanceof TexturePolygon) {
                        return new TexturePolygonDef((TexturePolygon) prettyPolygon);
                }
                throw new IllegalArgumentException("Unknown polygon.");
        }


}
