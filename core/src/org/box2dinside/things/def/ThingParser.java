/*
 The MIT License (MIT)

 Copyright (c) <2015> <Andreas Modahl>

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 */
package org.box2dinside.things.def;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ObjectMap;
import org.box2dinside.things.*;

/**
 *
 * @author Andreas
 */
public class ThingParser {

        private static final Json json = new Json();

        public static String hingeToJSon(Hinge thing) {

                ObjectMap<String, String> thingAsMap = new ObjectMap<String, String>();
                thingAsMap.put("TypeOfThing", "Hinge");

                HingeDef weldDef = DefParser.HingeDefParser.hingeToHingeDef(thing);
                thingAsMap.put("Def", json.toJson(weldDef));

                return json.toJson(thingAsMap);

        }

        public static String weldToJSon(Weld thing) {

                ObjectMap<String, String> thingAsMap = new ObjectMap<String, String>();
                thingAsMap.put("TypeOfThing", "Weld");

                WeldDef weldDef = DefParser.WeldDefParser.weldToWeldDef(thing);
                thingAsMap.put("Def", json.toJson(weldDef));

                return json.toJson(thingAsMap);

        }

        public static String ropeToJSon(Rope thing) {
                ObjectMap<String, String> thingAsMap = new ObjectMap<String, String>();
                thingAsMap.put("TypeOfThing", "Rope");

                RopeDef weldDef = DefParser.RopeDefParser.ropeToRopeDef(thing);
                thingAsMap.put("Def", json.toJson(weldDef));

                return json.toJson(thingAsMap);
        }

        public static String circleToJSon(Circle thing) {

                ObjectMap<String, String> thingAsMap = new ObjectMap<String, String>();
                thingAsMap.put("TypeOfThing", "Circle");

                CircleDef weldDef = DefParser.CircleDefParser.circleToDefinition(thing);
                thingAsMap.put("Def", json.toJson(weldDef));

                return json.toJson(thingAsMap);
        }

        public static String polygonToJSon(Polygon thing) {

                ObjectMap<String, String> thingAsMap = new ObjectMap<String, String>();
                thingAsMap.put("TypeOfThing", "Polygon");

                PolygonDef weldDef = DefParser.PolygonDefParser.polygonToPolygonDef(thing);
                thingAsMap.put("Def", json.toJson(weldDef));

                return json.toJson(thingAsMap);

        }


        public static String thingsToJSon(Array<Thing> things) {
                Array<String> thingsAsJson = new Array(things.size);
                for (Thing thing : things) {
                        String s = thingToJSon(thing);
                        if (s != null) {
                                thingsAsJson.add(s);
                        }
                }
                return json.toJson(thingsAsJson);
        }

        public static String thingToJSon(Thing thing) {
                if (thing instanceof Polygon) {
                        return polygonToJSon((Polygon) thing);
                } else if (thing instanceof Circle) {
                        return circleToJSon((Circle) thing);
                } else if (thing instanceof Hinge) {
                        return hingeToJSon((Hinge) thing);
                } else if (thing instanceof Rope) {
                        return ropeToJSon((Rope) thing);
                } else if (thing instanceof Weld) {
                        return weldToJSon((Weld) thing);
                }

                return null;
        }
}
