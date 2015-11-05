/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.box2dinside.things;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.utils.Array;
import org.box2dinside.things.def.ThingWithBodyDef;
import org.box2dinside.world.BoxWorld;

/**
 * @author Andreas
 */
public abstract class AbstractThingWithBody extends AbstractThing implements ThingWithBody {

        /**
         * Definition from when this thing was created.
         */
        protected BodyDef bodyDef;
        /**
         * Definition from when this thing was created.
         */
        protected FixtureDef fixtureDef;

        protected Body body;

        /**
         * If the value is 0 then this property is ignored. Otherwise all
         * things with equal value will never collide.
         */
        protected int antiCollisionGroup = 0;

        @SuppressWarnings("LeakingThisInConstructor")
        public AbstractThingWithBody(BoxWorld boxWorld, ThingWithBodyDef def) {
                super(boxWorld, def);

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
        public void storePreviousState() {
                lastPhysicsPos.set(body.getPosition());
                lastPhysicsAngle = body.getAngle();
        }

        @Override
        public void setTransform(float x, float y, float angle) {
                body.setTransform(x, y, angle);
        }

        @Override
        public float getInterpolatedAngle() {
                return interpolatedAngle;
        }

        @Override
        public Vector2 getInterpolatedPosition() {
                return interpolatedPosition;
        }
}
