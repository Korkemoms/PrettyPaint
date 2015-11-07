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

package org.ams.physics.world.def;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import org.ams.physics.things.def.ThingDef;
import org.ams.physics.world.BoxWorld;

/**
 * The main purpose of these definitions is to make it easy so save and load things.
 *
 * @author Andreas
 */
public class BoxWorldDef {
        public Array<ThingDef> things = new Array<ThingDef>();
        public final Vector2 gravity = new Vector2(0, -9.81f);

        public BoxWorldDef() {

        }

        public BoxWorldDef(BoxWorld boxWorld) {
                Array<ThingDef> thingDefArray = org.ams.physics.things.def.DefParser
                        .thingsToDefinitions(boxWorld.things);

                things.addAll(thingDefArray);

                gravity.set(boxWorld.world.getGravity());
        }

}
