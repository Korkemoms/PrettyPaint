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

import com.badlogic.gdx.utils.Array;
import org.ams.physics.things.def.ThingDef;
import org.ams.prettypaint.OutlinePolygon;
import org.ams.prettypaint.def.OutlinePolygonDef;
import org.ams.prettypaint.def.TexturePolygonDef;

/**
 * Created by Andreas on 06.11.2015.
 */
public class PPThingDef {

        public TexturePolygonDef texturePolygonDef;
        public Array<OutlinePolygonDef> outlinePolygonDefinitions;

        public ThingDef physicsThingDefinition;

        public String typeOfThing;

        public static PPThing definitionToThing(PPThingDef definition) {
                if (definition.typeOfThing.equals("PPCircle"))
                        return new PPCircle(definition);
                else if (definition.typeOfThing.equals("PPPolygon"))
                        return new PPPolygon(definition);

                throw new IllegalArgumentException("Unknown PPThingDef");
        }

        public PPThingDef(PPThing toCopy) {
                typeOfThing = toCopy.getType();

                for (OutlinePolygon outlinePolygon : toCopy.getOutlinePolygons()) {
                        OutlinePolygonDef outlinePolygonDef = new OutlinePolygonDef(outlinePolygon);
                        outlinePolygonDefinitions.add(outlinePolygonDef);
                }

                if (toCopy.getTexturePolygon() != null)
                        texturePolygonDef = new TexturePolygonDef(toCopy.getTexturePolygon());

                if (toCopy.getPhysicsThing() != null)
                        physicsThingDefinition = org.ams.physics.things.def.DefParser
                                .thingToDefinition(toCopy.getPhysicsThing());


        }

}
