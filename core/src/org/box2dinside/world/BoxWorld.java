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
package org.box2dinside.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import org.box2dinside.things.JointThing;
import org.box2dinside.things.Thing;
import org.box2dinside.things.ThingWithBody;


/**
 * @author Andreas
 */
public class BoxWorld {

        public World world;

        public final Array<Thing> things = new Array<Thing>(true, 200, Thing.class);


        private final static int MAX_FPS = 60;
        private final static int MIN_FPS = 10;
        private final static float TIME_STEP = 1f / MAX_FPS;
        private final static float MAX_STEPS = 1f + MAX_FPS / MIN_FPS;
        private final static float MAX_TIME_PER_FRAME = TIME_STEP * MAX_STEPS;
        private final static int VELOCITY_ITERS = 6;
        private final static int POSITION_ITERS = 6;

        private float physicsTimeLeft;

        /**
         * Fired when things are added via the safelyAddThing or removed via
         * the safelyRemoveThing methods.
         */
        public Array<ThingListener> thingListeners = new Array();

        /** Called right after a fixed step occurs on the box2d world. */
        public Array<FixedStepListener> fixedStepListeners = new Array(true, 1, FixedStepListener.class);

        /** Fired 1 step before two things make contact. */
        public Array<ContactListener> contactListeners = new Array(true, 3, ContactListener.class);

        private int nextID = 0;

        private ThingWithBody preferredJointAnchor = null;


        public BoxWorld() {


                createWorld();


        }

        /**
         * You should use this whenever you need a new ID for a thing. Otherwise
         * complications may occur.
         */
        public int getUnusedID() {
                return nextID++;
        }

        /**
         * Remember to use {@link #getUnusedID()} to set the id of the thing you add.
         *
         * @param t
         */
        public void safelyAddThing(Thing t) {

                things.add(t);

                for (int i = 0; i < thingListeners.size; i++) {
                        thingListeners.get(i).thingAdded(t);
                }
                t.thingHasBeenAddedToBoxWorld(this);
        }

        public void clear() {

                // remove all Things
                for (int i = things.size - 1; i >= 0; i--) {
                        Thing t = things.get(i);
                        if (!(t instanceof JointThing)) continue;

                        JointThing w = (JointThing) t;

                        if (!isDisposed(t)) {
                                world.destroyJoint(w.getJoint());
                                t.dispose();
                        }

                        things.removeValue(t, true);
                }

                while (things.size > 0) {
                        Thing t = things.get(0);

                        if (t.hasBody()) {
                                world.destroyBody(things.get(0).getBody());
                                t.dispose();
                        }

                        if (!isDisposed(t)) {
                                t.dispose();
                        }

                        things.removeIndex(0);
                }


                for (ThingListener tl : thingListeners) {
                        tl.worldCleared();
                }

                // recreate default particle system
                //defaultParticleSystem.destroyParticleSystem();
                //remakeDefaultParticleSystem();
        }

        public void safelyRemoveThing(Thing t) {
                if (t == null) throw new IllegalArgumentException("Argument can not be null");

                int n = things.size;
                things.removeValue(t, true);

                if (!isDisposed(t)) {
                        if (t.hasBody()) {
                                world.destroyBody(t.getBody());
                        }
                        if (t instanceof JointThing) {
                                world.destroyJoint(((JointThing) t).getJoint());
                        }
                        t.dispose();
                }

                if (n != things.size)
                        for (int i = 0; i < thingListeners.size; i++) {
                                thingListeners.get(i).thingRemoved(t);
                        }

        }

        public boolean isDisposed(Thing t) {
                if (t == null) return true;

                boolean isDisposed = false;
                if (t instanceof ThingWithBody) {
                        return isDisposed((Body) t.getBody());
                }
                if (t instanceof JointThing) {
                        return isDisposed(((JointThing) t).getJoint());
                }
                return isDisposed;
        }

        public boolean isDisposed(Body t) {
                if (t == null) return true;

                boolean isDisposed = false;

                Array<Body> bodies = new Array();
                world.getBodies(bodies);

                isDisposed |= !bodies.contains(t, true);

                return isDisposed;
        }

        public boolean isDisposed(Joint t) {
                if (t == null) return true;

                boolean isDisposed = false;

                Array<Joint> joints = new Array();
                world.getJoints(joints);

                isDisposed |= !joints.contains(t, true);

                return isDisposed;
        }


        public boolean fixedStep(float delta) {
                physicsTimeLeft += delta;
                if (physicsTimeLeft > MAX_TIME_PER_FRAME) {
                        physicsTimeLeft = MAX_TIME_PER_FRAME;
                }

                boolean stepped = false;

                while (physicsTimeLeft >= TIME_STEP) {
                        physicsTimeLeft -= TIME_STEP;
                        if (physicsTimeLeft < TIME_STEP) {
                                for (int i = 0; i < things.size; i++) {
                                        things.items[i].storePreviousState();
                                }
                        }

                        world.step(TIME_STEP, VELOCITY_ITERS, POSITION_ITERS);
                        for (int i = 0; i < fixedStepListeners.size; i++) {
                                fixedStepListeners.items[i].fixedStep(TIME_STEP);
                        }

                        stepped = true;
                }

                float interpolate = 1f - physicsTimeLeft / TIME_STEP;
                //System.out.println(interpolate);

                for (int i = 0; i < things.size; i++) {
                        things.items[i].interpolate(interpolate);
                }

                return stepped;
        }


        private void fireContactListeners(ThingWithBody a, ThingWithBody b) {
                for (int i = 0; i < contactListeners.size; i++) {
                        contactListeners.items[i].contact(a, b);
                }
        }

        private void createWorld() {
                world = new World(new Vector2(0, -9.8f), true);

                world.setContactListener(new com.badlogic.gdx.physics.box2d.ContactListener() {
                        Object a, b;

                        @Override
                        public void beginContact(Contact contact) {
                                a = contact.getFixtureA().getBody().getUserData();
                                b = contact.getFixtureB().getBody().getUserData();
                                if (a instanceof ThingWithBody && b instanceof ThingWithBody)
                                        fireContactListeners((ThingWithBody) a, (ThingWithBody) b);
                        }

                        @Override
                        public void endContact(Contact contact) {
                        }


                        @Override
                        public void preSolve(Contact contact, Manifold oldManifold) {

                        }

                        @Override
                        public void postSolve(Contact contact, ContactImpulse impulse) {

                        }
                });

                world.setContactFilter(new ContactFilter() {


                        @Override
                        public boolean shouldCollide(Fixture fixtureA, Fixture fixtureB) {
                                ThingWithBody thingA = (ThingWithBody) fixtureA.getUserData();
                                ThingWithBody thingB = (ThingWithBody) fixtureB.getUserData();

                                if (thingA != null && thingB != null) {
                                        // if antiCollisionGroup is the same do not collide
                                        int antiCollisionGroupA = thingA.getAntiCollisionGroup();
                                        int antiCollisionGroupB = thingB.getAntiCollisionGroup();

                                        if (antiCollisionGroupA == antiCollisionGroupB && antiCollisionGroupA != 0) {
                                                return false;
                                        }
                                }

                                Filter filterA = fixtureA.getFilterData();
                                Filter filterB = fixtureB.getFilterData();

                                int groupIndexA = filterA.groupIndex;
                                int groupIndexB = filterB.groupIndex;

                                boolean useCategoryAndMask = groupIndexA == 0;
                                //if either fixture has a groupIndex of zero, use the category/mask rules as above

                                useCategoryAndMask |= groupIndexB == 0;
                                //if both groupIndex values are non-zero but different, use the category/mask rules as above
                                useCategoryAndMask |= (groupIndexA != 0 && groupIndexB != 0 && groupIndexA != groupIndexB);

                                if (useCategoryAndMask) {
                                        if ((filterA.maskBits & filterB.categoryBits) != 0
                                                && (filterA.categoryBits & filterB.maskBits) != 0) {

                                                return true;
                                        }
                                }

                                //if both groupIndex values are the same and positive, collide
                                if (groupIndexA > 0 && groupIndexB > 0 && groupIndexA == groupIndexB) {
                                        return true;
                                }

                                //if both groupIndex values are the same and negative, don't collide
                                // if (groupIndexA < 0 && groupIndexB < 0 && groupIndexA == groupIndexB) return false;
                                return false;
                        }

                });
        }

        public void dispose() {
                world.dispose();
        }

        /**
         * @return a Polygon that can be used as the second body for joints.
         */
        public ThingWithBody getJointAnchor() {
                if (preferredJointAnchor != null) return preferredJointAnchor;
                for (Thing t : things) {
                        if (t instanceof ThingWithBody) {
                                if (t.getBody() != null && t.getBody().getType() == BodyDef.BodyType.StaticBody) {
                                        return (ThingWithBody) t;
                                }
                        }
                }
                return null;
        }

        public void setPreferredJointAnchor(ThingWithBody twb) {

        }

        public interface ThingListener {

                void thingAdded(Thing thing);

                void thingRemoved(Thing thing);

                void worldCleared();

        }

        public static interface ContactListener {

                public void contact(ThingWithBody a, ThingWithBody b);
        }

        public static interface FixedStepListener {

                public void fixedStep(float TIME_STEP);
        }
}
