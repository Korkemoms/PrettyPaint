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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ams.physics.things;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import org.ams.physics.things.def.ThingDef;
import org.ams.physics.things.def.ThingWithBodyDef;
import org.ams.physics.world.BoxWorld;

/**
 * @author Andreas
 */
public abstract class AbstractThingWithBody extends AbstractThing implements ThingWithBody {

        /**
         * Definition from when this thing was created. Some values can change.
         */
        protected BodyDef bodyDef;
        /**
         * Definition from when this thing was created. Some values can change.
         */
        protected FixtureDef fixtureDef;


        protected Body body;

        private boolean setTransformLater = false;
        private float angleForLater;
        private Vector2 posForLater;

        /**
         * If the value is 0 then this property is ignored. Otherwise all
         * things with equal value will never collide.
         */
        protected int antiCollisionGroup = 0;

        public AbstractThingWithBody(ThingWithBodyDef def) {
                super(def);
                initFromDefinition(def);
        }

        protected void initFromDefinition(ThingWithBodyDef def) {
                bodyDef = new BodyDef();

                bodyDef.active = def.active;
                bodyDef.angle = def.angle;
                bodyDef.angularDamping = def.angularDamping;
                bodyDef.awake = def.awake;
                bodyDef.bullet = def.bullet;
                bodyDef.fixedRotation = def.fixedRotation;
                bodyDef.linearDamping = def.linearDamping;
                bodyDef.position.set(def.position);
                bodyDef.type = def.type;


                fixtureDef = new FixtureDef();

                fixtureDef.density = def.density;
                fixtureDef.friction = def.friction;
                fixtureDef.restitution = def.restitution;
                fixtureDef.filter.categoryBits = def.categoryBits;
                fixtureDef.filter.groupIndex = def.groupIndex;
                fixtureDef.filter.maskBits = def.maskBits;

                antiCollisionGroup = def.antiCollisionGroup;
        }

        @Override
        public void thingHasBeenAddedToBoxWorld(BoxWorld boxWorld) {
                super.thingHasBeenAddedToBoxWorld(boxWorld);

                body = boxWorld.world.createBody(bodyDef);
                body.setUserData(this);

                if (setTransformLater) {
                        body.setTransform(posForLater.x, posForLater.y, angleForLater);
                }

                // set some initial values to avoid this polygon being rendered on (0,0) the first frame
                float angle = body.getAngle();
                Vector2 pos = body.getPosition();


                lastPhysicsPos.set(pos);
                lastPhysicsAngle = angle;


        }


        @Override
        public boolean hasBody() {
                return true;
        }

        @Override
        public Body getBody() {
                return body;
        }

        @Override
        public void setAntiCollisionGroup(int i) {
                antiCollisionGroup = i;
        }

        @Override
        public int getAntiCollisionGroup() {
                return antiCollisionGroup;
        }


        @Override
        public void dispose() {
                super.dispose();
                body = null;
        }

        @Override
        public void interpolate(float f) {

                interpolatedPosition.x = lastPhysicsPos.x * f;
                interpolatedPosition.y = lastPhysicsPos.y * f;

                interpolatedPosition.x += body.getPosition().x * (1 - f);
                interpolatedPosition.y += body.getPosition().y * (1 - f);


                interpolatedAngle = lastPhysicsAngle * f;
                interpolatedAngle += body.getAngle() * (1 - f);


        }

        @Override
        public void storeCurrentState() {
                lastPhysicsPos.set(body.getPosition());
                lastPhysicsAngle = body.getAngle();
        }

        @Override
        public void setTransform(float x, float y, float angle) {
                if (body == null) {
                        setTransformLater = true;
                        posForLater = new Vector2(x, y);
                        angleForLater = angle;
                } else {
                        body.setTransform(x, y, angle);
                }
        }

        @Override
        public float getInterpolatedAngle() {
                return interpolatedAngle;
        }

        @Override
        public Vector2 getInterpolatedPosition() {
                return interpolatedPosition;
        }

        @Override
        public Vector2 getPosition() {
                if (body != null) return body.getPosition();
                return posForLater != null ? posForLater : new Vector2();
        }

        @Override
        public float getAngle() {
                if (body != null) return body.getAngle();
                return angleForLater;
        }

        @Override
        public void setFriction(float friction) {
                fixtureDef.friction = friction;
                if (body != null) {
                        for (Fixture fixture : body.getFixtureList()) {
                                fixture.setFriction(friction);
                        }
                }
        }

        @Override
        public float getFriction() {
                return fixtureDef.friction;
        }
}
