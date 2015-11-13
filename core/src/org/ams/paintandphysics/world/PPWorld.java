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

package org.ams.paintandphysics.world;

import com.badlogic.gdx.utils.Array;
import org.ams.paintandphysics.things.PPThing;
import org.ams.paintandphysics.things.PPThingDef;
import org.ams.physics.world.BoxWorld;
import org.ams.physics.world.def.BoxWorldDef;
import org.ams.prettypaint.PrettyPolygonBatch;

/**
 * Created by Andreas on 05.11.2015.
 */
public class PPWorld {

        /**
         * To add a thing you should use {@link #addThing(PPThing)}.
         */
        public Array<PPThing> things = new Array<PPThing>();

        public BoxWorld boxWorld;


        public PPWorld() {
                this(null);
        }

        public PPWorld(PPWorldDef definition) {
                BoxWorldDef def = new BoxWorldDef();
                boxWorld = new BoxWorld(def);


                if (definition != null)
                        for (PPThingDef thingDef : definition.thingDefinitions) {
                                addThing(PPThingDef.definitionToThing(thingDef));
                        }


        }

        public PPWorld step(float deltaTime) {
                boxWorld.fixedStep(deltaTime);
                return this;
        }

        public PPWorld draw(PrettyPolygonBatch batch) {
                for (PPThing thing : things) {
                        thing.draw(batch);
                }
                return this;
        }

        public PPWorld addThing(PPThing thing) {
                things.add(thing);
                if (thing.getPhysicsThing() != null) {
                        boxWorld.safelyAddThing(thing.getPhysicsThing());
                }
                return this;
        }

        /**
         * The index is ignored for the
         *
         * @param index
         * @param thing
         * @return
         */
        public PPWorld insertThing(int index, PPThing thing) {
                things.insert(index, thing);
                if (thing.getPhysicsThing() != null) {
                        boxWorld.safelyInsertThing(index, thing.getPhysicsThing());
                }
                return this;
        }


}
