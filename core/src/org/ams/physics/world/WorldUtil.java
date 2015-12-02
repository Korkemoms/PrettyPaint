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
package org.ams.physics.world;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import org.ams.core.CoordinateHelper;
import org.ams.core.Timer;
import org.ams.physics.things.*;
import org.ams.core.Util;
import org.ams.physics.things.Circle;
import org.ams.physics.things.Polygon;

import java.util.HashSet;
import java.util.Set;

/**
 * Functions that can be useful when playing with box2d. These can be slow
 * and may not do what they say.
 *
 * @author Andreas
 */
public class WorldUtil {

        public static void rotateRad(ThingWithBody twb, float angleRad, Vector2 center) {
                Body body = twb.getBody();
                Vector2 pos = body.getPosition();

                twb.getBody().setTransform(pos.sub(center), body.getAngle());

                twb.getBody().setTransform(pos.rotateRad(angleRad), body.getAngle() + angleRad);

                pos.add(center);
                twb.setTransform(pos.x, pos.y, body.getAngle());

        }

        /**
         * @return the center of all argument things.
         */
        public static Vector2 findCenterOfThings(Array<Thing> things) {
                Vector2 center = new Vector2();
                for (Thing thing : things) {
                        center.add(thing.getBody().getPosition());
                }
                center.x /= things.size;
                center.y /= things.size;
                return center;
        }

        /**
         * @return the center of all argument things.
         */
        public static Vector2 findCenterOfThings(Thing... things) {
                Vector2 center = new Vector2();
                for (Thing thing : things) {
                        center.add(thing.getBody().getPosition());
                }
                center.x /= things.length;
                center.y /= things.length;
                return center;
        }

        public static class UnsupportedShapeException extends RuntimeException {

                public UnsupportedShapeException(String string) {
                        super(string);
                }
        }


        /**
         * @param fixture any fixture
         * @param x       in world units
         * @param y       in world units
         * @return whether (x,y) is inside this fixture after the fixture has
         * been rotated by angleRad.
         */
        public static boolean isPointInsideFixture(Fixture fixture, float x, float y) {
                final float[] vertx = new float[8];
                final float[] verty = new float[8];

                Vector2 vertex = new Vector2();
                Vector2 v1 = new Vector2();
                Vector2 v2 = new Vector2();

                Shape shape = fixture.getShape();
                Vector2 bodyPosition = new Vector2(fixture.getBody().getPosition());

                if (shape instanceof PolygonShape) {
                        PolygonShape polygonShape = (PolygonShape) shape;
                        int nvert = polygonShape.getVertexCount();
                        for (int i = 0; i < nvert; i++) {
                                polygonShape.getVertex(i, vertex);
                                vertex.rotateRad(fixture.getBody().getAngle());
                                vertx[i] = vertex.x + bodyPosition.x;
                                verty[i] = vertex.y + bodyPosition.y;
                        }
                        return Util.isPointInsidePolygon(vertx, verty, nvert, x, y);
                }
                if (shape instanceof CircleShape) {
                        CircleShape circleShape = (CircleShape) shape;
                        v1.x = x;
                        v1.y = y;

                        v2.x = circleShape.getPosition().x + bodyPosition.x;
                        v2.y = circleShape.getPosition().y + bodyPosition.y;

                        return v1.dst(v2) < circleShape.getRadius();

                }
                throw new UnsupportedShapeException(
                        "Unsupported shape: " + shape);
        }

        public static boolean isPointInsideBody(Body b, float x, float y) {
                Array<Fixture> fixtures = b.getFixtureList();
                for (Fixture f : fixtures) {
                        if (isPointInsideFixture(f, x, y)) return true;
                }
                return false;
        }

        public static boolean circleIntersectFixture(Fixture fixture, float x, float y, float radius) {
                Shape shape = fixture.getShape();

                Vector2 v3 = new Vector2(x, y);
                Vector2 v1 = new Vector2();
                Vector2 v2 = new Vector2();
                Vector2 bodyPosition = new Vector2();

                float squaredRadius = radius * radius;

                bodyPosition.set(fixture.getBody().getPosition());
                if (shape instanceof PolygonShape) {
                        PolygonShape polygonShape = (PolygonShape) shape;
                        int nvert = polygonShape.getVertexCount();
                        float angle = fixture.getBody().getAngle();

                        polygonShape.getVertex(0, v1);
                        v1.rotateRad(angle);
                        v1.add(bodyPosition);

                        for (int i = 1; i <= nvert; i++) {
                                polygonShape.getVertex(i % nvert, v2);
                                v2.rotateRad(angle);
                                v2.add(bodyPosition);

                                if (Intersector.intersectSegmentCircle(v1, v2, v3, squaredRadius)) return true;

                                v1.set(v2);
                        }

                        return isPointInsideFixture(fixture, x, y);

                }
                if (shape instanceof CircleShape) {
                        CircleShape circleShape = (CircleShape) shape;
                        v1.x = x;
                        v1.y = y;

                        v2.x = circleShape.getPosition().x + bodyPosition.x;
                        v2.y = circleShape.getPosition().y + bodyPosition.y;

                        return v1.dst(v2) < circleShape.getRadius() + radius;

                }
                throw new UnsupportedShapeException(
                        "Unsupported shape: " + shape);
        }

        /**
         * @param things the things you wish to include in the search
         * @param worldX x in world units
         * @param worldY y in world units
         * @return the things 'under' this position that are of type T.
         */
        public static Array<Thing> getThingsAtWorldPoint(Array<Thing> things,
                                                         float worldX, float worldY, float slack) {

                Vector2 v1 = new Vector2(worldX, worldY);


                Array<Thing> thingsAtPoint = new Array();

                for (Thing thing : things) {
                        if (thingsAtPoint.contains(thing, true)) continue;

                        if (thing instanceof Hinge) {
                                Hinge hinge = (Hinge) thing;
                                if (hinge.getAnchorA().dst(v1) <= slack
                                        || hinge.getAnchorB().dst(v1) <= slack) {

                                        thingsAtPoint.add(thing);


                                }
                        } else if (thing instanceof Weld) {
                                Weld weld = (Weld) thing;
                                if (weld.getAnchorA().dst(v1) <= slack
                                        || weld.getAnchorB().dst(v1) <= slack) {
                                        thingsAtPoint.add(thing);
                                }
                        } else if (thing instanceof Polygon) {
                                Array<Fixture> fixtures = thing.getBody().getFixtureList();
                                for (Fixture f : fixtures) {
                                        if (isPointInsideFixture(f, worldX, worldY)) {

                                                thingsAtPoint.add(thing);

                                                break;
                                        }
                                }
                        } else if (thing instanceof Circle) {
                                Circle c = (Circle) thing;
                                if (c.getBody().getPosition().dst(worldX, worldY) < c.radius) {
                                        thingsAtPoint.add(c);
                                }
                        }
                }
                return thingsAtPoint;
        }


        /**
         * Unlike the method getClosestBody, this method looks at the outlines
         * of a thing when comparing distance to given point.
         *
         * @param things
         * @param worldX
         * @param worldY
         * @param maxDistance
         * @return the thing closest to given point. Null if no things are
         * within maxDistance.
         */
        public static Thing getClosestThingIntersectingCircle(Array<Thing> things, float worldX, float worldY, float maxDistance, Filter filter) {
                Array<Thing> whithinDistance = getThingsIntersectingWithCircle(things, worldX, worldY, maxDistance, filter);


                if (whithinDistance.size == 0) return null;
                if (whithinDistance.size == 1) return whithinDistance.first();

                float lower = 0;
                float higher = maxDistance;

                Array<Thing> whithinDistance2 = new Array<Thing>();
                whithinDistance2.addAll(whithinDistance);

                int iterations = 0;

                while (whithinDistance2.size != 1) {
                        if (whithinDistance2.size > 1) {
                                if (iterations > 9) return whithinDistance2.first();
                                maxDistance = lower + (higher - lower) * 0.5f;

                        } else if (whithinDistance2.size == 0)
                                maxDistance = lower + (higher - lower) * 0.5f;

                        whithinDistance2 = getThingsIntersectingWithCircle(whithinDistance, worldX, worldY, maxDistance, filter);

                        if (whithinDistance2.size > 1) higher = maxDistance;
                        if (whithinDistance2.size == 0) lower = maxDistance;
                        iterations++;
                }
                return whithinDistance2.first();
        }

        public static Set<Body> getIntersectingBodies(Rectangle rectangle, World world) {
                final Set<Body> intersectingBodies = new HashSet<Body>();

                world.QueryAABB(new QueryCallback() {
                        @Override
                        public boolean reportFixture(Fixture fixture) {
                                intersectingBodies.add(fixture.getBody());
                                return true;
                        }
                }, rectangle.x, rectangle.y, rectangle.x + rectangle.width, rectangle.y + rectangle.height);
                return intersectingBodies;
        }


        /**
         * @param things the things you wish to include in the search
         * @param worldX x in world units
         * @param worldY y in world units
         * @param radius radius of circle in world units
         * @return the things 'under' this position that are of type T.
         */
        public static Array<Thing> getThingsIntersectingWithCircle(Array<Thing> things,
                                                                   float worldX, float worldY, float radius, Filter filter) {

                Array<Thing> found = new Array();
                Vector2 v3 = new Vector2(worldX, worldY);

                for (Thing thing : things) {
                        if (found.contains(thing, true)) continue;
                        if (filter != null && !filter.accept(thing)) continue;

                        if (thing instanceof JointThing) {
                                JointThing joint = (JointThing) thing;
                                if (joint.getAnchorA().dst(v3) <= radius
                                        || joint.getAnchorB().dst(v3) <= radius) {

                                        found.add(thing);

                                }
                        } else if (thing instanceof Polygon) {
                                Array<Fixture> fixtures = thing.getBody().getFixtureList();
                                for (Fixture f : fixtures) {
                                        if (circleIntersectFixture(f, worldX, worldY, radius)) {
                                                found.add(thing);
                                                break;
                                        }
                                }

                        } else if (thing instanceof Circle) {
                                Circle c = (Circle) thing;
                                if (c.getBody().getPosition().dst(v3) < c.radius + radius)
                                        found.add(thing);

                        }
                }

                return found;
        }


        public static interface Filter {

                public boolean accept(Object o);
        }

        /**
         * Only looks at a bodies center when comparing bodies.
         *
         * @param pos        the position for which you want to find the closest
         *                   polygon
         * @param closestPos the center of the closest polygon is put in this
         *                   vector
         * @return the body whose center is closest to given pos
         */
        public static ThingWithBody getClosestBody(Array<Thing> things, Vector2 pos, Vector2 closestPos, Filter filter) {
                float closestDst = Float.MAX_VALUE;
                Vector2 closestCenter = null;
                ThingWithBody closestThing = null;

                for (Thing thing : things) {
                        if (!filter.accept(thing)) continue;

                        if (!(thing instanceof ThingWithBody)) continue;
                        ThingWithBody p = (ThingWithBody) thing;

                        float dst = p.getBody().getPosition().dst(pos);
                        if (dst < closestDst) {
                                closestDst = dst;
                                closestCenter = p.getBody().getPosition();
                                closestThing = p;
                        }
                }
                if (closestPos != null && closestCenter != null) closestPos.set(closestCenter);
                return closestThing;

        }

        /**
         * @param pos        the position for which you want to find the closest
         *                   polygon
         * @param closestPos the center of the closest polygon is put in this
         *                   vector
         * @param ignore     any polygons you wish to ignore in the search
         * @return the closest polygon
         */
        public static ThingWithBody getClosestBody(Array<Thing> things, Vector2 pos, Vector2 closestPos, ThingWithBody... ignore) {
                float closestDst = Float.MAX_VALUE;
                Vector2 closestCenter = null;
                ThingWithBody closestThing = null;

                for (Thing thing : things) {
                        boolean _ignore = false;
                        for (ThingWithBody p : ignore) {
                                if (p == thing) {
                                        _ignore = true;
                                        break;
                                }
                        }
                        if (_ignore) continue;

                        if (!(thing instanceof ThingWithBody)) continue;
                        ThingWithBody p = (ThingWithBody) thing;

                        float dst = p.getBody().getPosition().dst(pos);
                        if (dst < closestDst) {
                                closestDst = dst;
                                closestCenter = p.getBody().getPosition();
                                closestThing = p;
                        }
                }
                if (closestPos != null) closestPos.set(closestCenter);
                return closestThing;

        }

        public static Array<Array<Thing>> splitIntoGroupsOfConnected(Array<Thing> things) {
                Array<Thing> copy = new Array();
                copy.addAll(things);

                Array<Array<Thing>> split = new Array();
                for (int i = copy.size - 1; i >= 0; i--) {
                        if (i >= copy.size) continue;
                        Thing t = copy.get(i);
                        Array<Thing> connected = getAllConnectedThings(t);
                        for (Thing _t : connected) {
                                if (copy.contains(_t, true)) {
                                        if (!connected.contains(_t, true)) connected.add(_t);
                                        copy.removeValue(_t, true);
                                }
                        }
                        split.add(connected);

                }
                return split;
        }

        /**
         * The things with the least joints between them and argument thing have
         * the lowest indexes in the returned array.
         */
        public static Array<Thing> getAllConnectedThings(Thing t) {
                return getAllConnectedThings(new Array(), t, null);
        }

        /**
         * The things with the least joints between them and argument thing have
         * the lowest indexes in the returned array.
         */
        public static Array<Thing> getAllConnectedThings(Thing t, Filter filter) {
                return getAllConnectedThings(new Array(), t, filter);
        }

        private static Array<Thing> getAllConnectedThings(Array<Thing> alreadyFound, Thing t, Filter filter) {
                if (t == null || !t.hasBody() || t.getBody() == null || alreadyFound.contains(t, true)) {
                        return alreadyFound;
                }

                alreadyFound.add(t);

                Array<JointEdge> joints = t.getBody().getJointList();

                for (int i = 0; i < joints.size; i++) {
                        JointEdge joint = joints.get(i);
                        Thing _t = (Thing) joint.joint.getUserData();
                        if (_t != null && !alreadyFound.contains(_t, true)) {
                                if (filter == null || filter.accept(_t)) {
                                        alreadyFound.add(_t);
                                }
                        }

                        ThingWithBody tw = (ThingWithBody) joint.other.getUserData();
                        Array<Thing> af = getAllConnectedThings(alreadyFound, tw, filter);
                        for (Thing _tw : af) {
                                if (_tw != null && !alreadyFound.contains(_tw, true)) {
                                        if (filter == null || filter.accept(_tw)) {
                                                alreadyFound.add(_tw);
                                        }
                                }
                        }
                }

                return alreadyFound;
        }

        public static void lookAt(final OrthographicCamera camera, final Timer timer, Array<Thing> things, float zoomMultiplier) {
                if (things.size == 0) return;

                Array<ThingWithBody> bodies = new Array<ThingWithBody>();
                for (int i = 0; i < things.size; i++) {
                        if (things.get(i).hasBody()) {
                                ThingWithBody twb = (ThingWithBody) things.get(i);
                                bodies.add(twb);
                        }
                }

                Rectangle boundingBox = new Rectangle(bodies.first().getPhysicsBoundingBox());
                for (int i = 1; i < bodies.size; i++) {
                        boundingBox.merge(bodies.get(i).getPhysicsBoundingBox());
                }

                lookAt(camera, timer, boundingBox, zoomMultiplier);
        }

        public static void lookAt(final OrthographicCamera camera, final Timer timer, Rectangle box, float zoomMultiplier) {


                final Vector2 targetPos = new Vector2();
                box.getCenter(targetPos);
                final float startZoom = camera.zoom;

                float horisontalZoom = (box.width) / camera.viewportWidth;
                float verticalZoom = (box.height) / camera.viewportHeight;

                float _targetZoom = horisontalZoom > verticalZoom ? horisontalZoom : verticalZoom;
                _targetZoom *= zoomMultiplier;
                final float targetZoom = _targetZoom < 0.5f ? 0.5f : _targetZoom;

                final Vector2 startPos = CoordinateHelper.getWorldCoordinates(camera, Gdx.graphics.getWidth() * 0.5f, Gdx.graphics.getHeight() * 0.5f);


                Runnable runnable = new Runnable() {
                        float linearAlpha = 0;

                        Vector3 v = new Vector3();
                        Vector3 v1 = new Vector3();

                        float secondsToFinish = 0.7f;

                        @Override
                        public void run() {
                                linearAlpha += Gdx.graphics.getDeltaTime() / secondsToFinish;
                                if (linearAlpha > 1) {
                                        linearAlpha = 1;
                                        timer.remove(this);
                                }

                                float alpha = Interpolation.pow2Out.apply(linearAlpha);

                                float zoom = startZoom * (1 - alpha) + targetZoom * alpha;
                                camera.zoom = zoom;

                                v.x = startPos.x * (1 - alpha) + targetPos.x * alpha;
                                v.y = startPos.y * (1 - alpha) + targetPos.y * alpha;
                                v.z = camera.position.z;
                                camera.position.set(v);

                                camera.update();

                        }
                };

                timer.runOnRender(runnable);
        }

}
