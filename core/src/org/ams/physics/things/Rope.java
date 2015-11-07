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
import com.badlogic.gdx.physics.box2d.joints.RopeJoint;
import com.badlogic.gdx.physics.box2d.joints.RopeJointDef;
import org.ams.physics.things.def.RopeDef;
import org.ams.physics.things.def.ThingDef;
import org.ams.physics.world.BoxWorld;

/**
 * @author Andreas
 */
public class Rope extends AbstractJoint {

        private RopeJointDef ropeJointDef;

        public Rope(RopeDef def) {
                super(def);


                initFromDefinition(def);

                postInstantiation(def);

        }

        protected void initFromDefinition(RopeDef def) {
                ropeJointDef = new RopeJointDef();


                ropeJointDef.collideConnected = true;
                ropeJointDef.localAnchorA.set(def.localAnchorA);
                ropeJointDef.localAnchorB.set(def.localAnchorB);
                ropeJointDef.maxLength = def.maxLength;
        }

        public float getMaxLength() {
                return ((RopeJoint) joint).getMaxLength();
        }

        @Override
        public void thingHasBeenAddedToBoxWorld(BoxWorld boxWorld) {
                super.thingHasBeenAddedToBoxWorld(boxWorld);

                if (thingA != null)
                        ropeJointDef.bodyA = thingA.getBody();
                else
                        ropeJointDef.bodyA = boxWorld.getJointAnchor().getBody();

                if (thingB != null)
                        ropeJointDef.bodyB = thingB.getBody();
                else {
                        ropeJointDef.bodyB = boxWorld.getJointAnchor().getBody();
                }


                this.joint = thingA.getBody().getWorld().createJoint(ropeJointDef);
                this.joint.setUserData(this);
        }

        @Override
        public String toString() {
                return "R" + getID() + " A=" + thingA.getID() + " B=" + thingB.getID();
        }

        @Override
        public boolean hasBody() {
                return false;
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
        public Vector2 getLocalAnchorA() {
                return ropeJointDef.localAnchorA;
        }

        @Override
        public Vector2 getLocalAnchorB() {
                return ropeJointDef.localAnchorB;
        }


}
