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
import com.badlogic.gdx.physics.box2d.joints.WeldJoint;
import com.badlogic.gdx.physics.box2d.joints.WeldJointDef;
import org.box2dinside.things.def.WeldDef;
import org.box2dinside.world.BoxWorld;

/**
 * @author Andreas
 */
public class Weld extends AbstractJoint implements JointThing {

        public WeldJointDef weldJointDef;

        public Weld(BoxWorld boxWorld, WeldDef def) {
                super(boxWorld, def);

                // for welds
                weldJointDef = new WeldJointDef();


                if (thingA != null)
                        weldJointDef.bodyA = thingA.getBody();
                else
                        weldJointDef.bodyA = boxWorld.getJointAnchor().getBody();

                if (thingB != null)
                        weldJointDef.bodyB = thingB.getBody();
                else
                        weldJointDef.bodyB = boxWorld.getJointAnchor().getBody();


                weldJointDef.localAnchorA.set(def.localAnchorA);
                weldJointDef.localAnchorB.set(def.localAnchorB);

                weldJointDef.referenceAngle = def.referenceAngle;

                postInstantiation(def);

        }

        @Override
        public void thingHasBeenAddedToBoxWorld(BoxWorld boxWorld) {
                super.thingHasBeenAddedToBoxWorld(boxWorld);
                this.joint = (WeldJoint) thingA.getBody().getWorld().createJoint(weldJointDef);

                this.joint.setUserData(this);
        }

        @Override
        public String toString() {
                return "W" + getID() + " A=" + thingA.getID() + " B=" + thingB.getID();
        }

        @Override
        public WeldJoint getJoint() {
                return (WeldJoint) joint;
        }

        @Override
        public Vector2 getLocalAnchorA() {
                return weldJointDef.localAnchorA;
        }

        @Override
        public Vector2 getLocalAnchorB() {
                return weldJointDef.localAnchorB;
        }
}
