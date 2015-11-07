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
package org.ams.physics.things;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Joint;
import org.ams.physics.things.def.JointThingDef;
import org.ams.physics.world.BoxWorld;

/**
 * @author Andreas
 */
public abstract class AbstractJoint extends AbstractThing implements JointThing {

        protected ThingWithBody thingA;
        protected ThingWithBody thingB;

        protected Joint joint;

        protected final Vector2 interpolatedPositionA = new Vector2();
        protected final Vector2 interpolatedPositionB = new Vector2();

        protected final Vector2 lastPhysicsPosA = new Vector2();
        protected final Vector2 lastPhysicsPosB = new Vector2();


        public AbstractJoint(JointThingDef def) {
                super(def);

        }

        @Override
        public void thingHasBeenAddedToBoxWorld(BoxWorld boxWorld) {
                super.thingHasBeenAddedToBoxWorld(boxWorld);

                JointThingDef def = (JointThingDef) definitionFromInitialization;

                ThingWithBody thingA = null, thingB = null;
                for (Thing thing : boxWorld.things) {

                        if (thing.getID() == def.idThingA) {
                                thingA = (ThingWithBody) thing;
                        }
                        if (thing.getID() == def.idThingB) {
                                thingB = (ThingWithBody) thing;
                        }
                }

                this.thingA = thingA;
                this.thingB = thingB;

        }

        @Override
        public void storeCurrentState() {

                lastPhysicsPosA.set(joint.getAnchorA());
                lastPhysicsPosB.set(joint.getAnchorB());
        }

        @Override
        public void interpolate(float f) {
                interpolatedPositionA.x = lastPhysicsPosA.x * f;
                interpolatedPositionA.x += joint.getAnchorA().x * (1 - f);

                interpolatedPositionA.y = lastPhysicsPosA.y * f;
                interpolatedPositionA.y += joint.getAnchorA().y * (1 - f);


                interpolatedPositionB.x = lastPhysicsPosB.x * f;
                interpolatedPositionB.x += joint.getAnchorB().x * (1 - f);

                interpolatedPositionB.y = lastPhysicsPosB.y * f;
                interpolatedPositionB.y += joint.getAnchorB().y * (1 - f);

        }

        @Override
        public Vector2 getInterpolatedPosA() {
                return interpolatedPositionA;
        }

        @Override
        public Vector2 getInterpolatedPosB() {
                return interpolatedPositionB;
        }

        @Override
        public ThingWithBody getThingA() {
                return thingA;
        }

        @Override
        public ThingWithBody getThingB() {
                return thingB;
        }

        @Override
        public Vector2 getAnchorA() {
                return joint.getAnchorA();
        }

        @Override
        public Vector2 getAnchorB() {
                return joint.getAnchorB();
        }

        @Override
        public Joint getJoint() {
                return joint;
        }

        @Override
        public void dispose() {
                super.dispose();
                joint = null;
        }

        @Override
        public boolean hasBody() {
                return false;
        }


}
