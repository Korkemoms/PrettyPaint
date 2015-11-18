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

package org.ams.physics.tools;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.joints.MouseJoint;
import com.badlogic.gdx.physics.box2d.joints.MouseJointDef;
import org.ams.core.CoordinateHelper;
import org.ams.core.Util;
import org.ams.physics.things.Thing;
import org.ams.physics.things.ThingWithBody;
import org.ams.physics.world.BoxWorld;
import org.ams.physics.world.WorldUtil;


/**
 *
 */
public class BodyMover extends InputAdapter {
        private OrthographicCamera camera;
        private BoxWorld world;

        private MouseJoint mouseJoint;
        private Vector2 offset = new Vector2();

        private float desiredAngle = 0;
        private boolean turning = false;

        private boolean active = true;


        private WorldUtil.Filter onlyThingWithBodyFilter = new WorldUtil.Filter() {
                @Override
                public boolean accept(Object o) {
                        return o instanceof ThingWithBody;
                }
        };


        public BodyMover(BoxWorld world, OrthographicCamera camera) {
                this.world = world;
                this.camera = camera;
        }

        public void setActive(boolean active) {
                this.active = active;
        }

        public boolean isActive() {
                return active;
        }

        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) {

                boolean b = mouseJoint != null;
                turning = false;

                if (mouseJoint != null) {
                        world.world.destroyJoint(mouseJoint);
                        mouseJoint = null;
                }

                return b;
        }

        @Override
        public boolean touchDragged(int screenX, int screenY, int pointer) {
                boolean b = mouseJoint != null;

                if (mouseJoint != null) {
                        Vector2 worldCoordinates = CoordinateHelper.getWorldCoordinates(camera, screenX, screenY);
                        worldCoordinates.add(offset);
                        mouseJoint.setTarget(worldCoordinates);
                }

                return b;
        }

        public void step(float delta) {
                /*if (mouseJoint == null) return;
                if (!turning) return;

                Body body = mouseJoint.getBodyB();


                float bodyAngle = body.getAngle();


                float nextAngle = bodyAngle + body.getAngularVelocity() / 3.0f;// 1/3 second
                float totalRotation = desiredAngle - nextAngle;//use angle in next time step

                float torque = body.getMass() * 10;

                body.applyTorque(totalRotation < 0 ? -torque : torque, true);*/
        }


        @Override
        public boolean scrolled(int amount) {
                if (mouseJoint == null) return false;

                if (!turning) {
                        turning = true;

                        desiredAngle = mouseJoint.getBodyB().getAngle();

                        float f = desiredAngle % MathUtils.PI * 0.5f;
                        if (f < MathUtils.PI * 0.25f) {
                                desiredAngle -= f;
                                desiredAngle += (MathUtils.PI * 0.5f - f);
                        }

                }


                desiredAngle += MathUtils.PI * 0.5f * amount;

                return true;
        }

        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {

                turning = false;
                if (mouseJoint != null) {
                        world.world.destroyJoint(mouseJoint);
                        mouseJoint = null;
                }

                if (!active) return false;

                Vector2 worldCoordinates = CoordinateHelper.getWorldCoordinates(camera, screenX, screenY);

                Thing closestThing = WorldUtil.getClosestThingIntersectingCircle(
                        world.things,
                        worldCoordinates.x,
                        worldCoordinates.y,
                        Util.getTouchRadius(camera.zoom),
                        onlyThingWithBodyFilter);

                if (closestThing == null) return false;


                ThingWithBody jointAnchor = world.getJointAnchor();
                if (closestThing == jointAnchor) return false;
                if (closestThing.getBody().getType() == BodyDef.BodyType.StaticBody) return false;

                if (jointAnchor == null) {
                        Gdx.app.log("BodyMover", "No joint anchor :(");
                        return false;
                }

                MouseJointDef mouseJointDef = new MouseJointDef();
                mouseJointDef.collideConnected = true;
                mouseJointDef.maxForce = closestThing.getBody().getMass() * 100;
                mouseJointDef.dampingRatio = 0;
                mouseJointDef.bodyA = world.getJointAnchor().getBody();
                mouseJointDef.bodyB = closestThing.getBody();
                mouseJointDef.target.set(closestThing.getBody().getPosition());

                mouseJoint = (MouseJoint) world.world.createJoint(mouseJointDef);

                offset.set(closestThing.getBody().getPosition()).sub(worldCoordinates);

                return true;
        }

        public void dispose() {
                if (mouseJoint != null) world.world.destroyJoint(mouseJoint);
        }
}
