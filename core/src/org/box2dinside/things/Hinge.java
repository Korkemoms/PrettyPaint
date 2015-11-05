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
package org.box2dinside.things;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJoint;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef;

import org.box2dinside.things.def.HingeDef;
import org.box2dinside.world.BoxWorld;

/**
 * @author Andreas
 */
public class Hinge extends AbstractJoint implements JointThing {

        public RevoluteJointDef hingeJointDef;

        public Hinge(BoxWorld boxWorld, HingeDef def) {
                super(boxWorld, def);

                // for hinges
                hingeJointDef = new RevoluteJointDef();


                if (thingA != null)
                        hingeJointDef.bodyA = thingA.getBody();
                else
                        hingeJointDef.bodyA = boxWorld.getJointAnchor().getBody();

                if (thingB != null)
                        hingeJointDef.bodyB = thingB.getBody();
                else
                        hingeJointDef.bodyB = boxWorld.getJointAnchor().getBody();


                hingeJointDef.localAnchorA.set(def.localAnchorA);
                hingeJointDef.localAnchorB.set(def.localAnchorB);

                hingeJointDef.enableLimit = def.enableLimit;
                hingeJointDef.enableMotor = def.enableMotor;
                hingeJointDef.lowerAngle = def.lowerLimit;
                hingeJointDef.upperAngle = def.upperLimit;
                hingeJointDef.maxMotorTorque = def.maxMotorTorque;
                hingeJointDef.motorSpeed = def.motorSpeed;

                postInstantiation(def);
        }

        @Override
        public void thingHasBeenAddedToBoxWorld(BoxWorld boxWorld) {
                super.thingHasBeenAddedToBoxWorld(boxWorld);
                this.joint = (RevoluteJoint) thingA.getBody().getWorld().createJoint(hingeJointDef);

                this.joint.setUserData(this);
        }

        @Override
        public String toString() {
                return "H" + getID() + " A=" + thingA.getID() + " B=" + thingB.getID();
        }

        @Override
        public RevoluteJoint getJoint() {
                return (RevoluteJoint) joint;
        }

        @Override
        public Vector2 getLocalAnchorA() {
                return hingeJointDef.localAnchorA;
        }

        @Override
        public Vector2 getLocalAnchorB() {
                return hingeJointDef.localAnchorB;
        }

}
