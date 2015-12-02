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
package org.ams.physics.things.def;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJoint;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ObjectMap;
import org.ams.physics.things.*;
import org.ams.physics.world.BoxWorld;

import java.lang.reflect.Type;

/**
 * The main purpose of these definitions is to make it easy so save and load things.
 *
 * @author Andreas
 */
public class DefParser {

        private static final Json json = new Json();

        public static Array<ThingDef> thingsToDefinitions(Array<Thing> things) {
                Array<ThingDef> definitions = new Array<ThingDef>(true, things.size, ThingDef.class);
                for (Thing thing : things) {
                        ThingDef def = thingToDefinition(thing);
                        definitions.add(def);
                }
                return definitions;
        }

        public static ThingDef thingToDefinition(Thing thing) {
                if (thing instanceof Polygon) {
                        return polygonToDefinition((Polygon) thing);
                } else if (thing instanceof Circle) {
                        return circleToDefinition((Circle) thing);
                } else if (thing instanceof Hinge) {
                        return hingeToDefinition((Hinge) thing);
                } else if (thing instanceof Rope) {
                        return ropeToDefinition((Rope) thing);
                } else if (thing instanceof Weld) {
                        return weldToDefinition((Weld) thing);
                }

                throw new IllegalArgumentException("Unknown thing.");
        }

        public static Array<Thing> definitionsToThings(Array<ThingDef> definitions) {
                Array<Thing> things = new Array<Thing>(true, definitions.size, Thing.class);

                // Joints must be added last because a joint has to attach to other things
                // that must be present in the world when the joint is added.
                for (ThingDef def : definitions) {
                        boolean addLater = def instanceof JointThingDef;
                        if (addLater) continue;
                        Thing thing = definitionToThing(def);
                        things.add(thing);
                }

                for (ThingDef def : definitions) {
                        boolean notAdded = !(def instanceof JointThingDef);
                        if (notAdded) continue;
                        Thing thing = definitionToThing(def);
                        things.add(thing);
                }


                return things;
        }


        public static Thing definitionToThing(ThingDef def) {
                if (def instanceof PolygonDef) {
                        return new Polygon((PolygonDef) def);
                } else if (def instanceof CircleDef) {
                        return new Circle((CircleDef) def);
                } else if (def instanceof HingeDef) {
                        return new Hinge((HingeDef) def);
                } else if (def instanceof WeldDef) {
                        return new Weld((WeldDef) def);
                } else if (def instanceof RopeDef) {
                        return new Rope((RopeDef) def);
                }
                throw new IllegalArgumentException("Unknown definition.");
        }

        public static ThingDef jsonToDefinition(String thingsAsJson) {

                ObjectMap<String, String> map = json.fromJson(ObjectMap.class, thingsAsJson);

                String type = map.get("TypeOfThing");
                String defAsJson = map.get("Def");

                return jsonToDefinition(defAsJson, type);

        }

        public static Array<ThingDef> jsonToDefinition(Array<String> thingsAsJson) {
                Array<ThingDef> thingsAsThingDef = new Array<ThingDef>(true, thingsAsJson.size, ThingDef.class);

                for (int i = 0; i < thingsAsJson.size; i++) {
                        ObjectMap<String, String> map = json.fromJson(ObjectMap.class, thingsAsJson.get(i));

                        String type = map.get("TypeOfThing");
                        String defAsJson = map.get("Def");

                        ThingDef def = jsonToDefinition(defAsJson, type);

                        if (def != null) thingsAsThingDef.add(def);

                }
                return thingsAsThingDef;
        }

        public static ThingDef jsonToDefinition(String defAsJson, String type) {

                if (type.equals("Hinge")) {
                        return json.fromJson(HingeDef.class, defAsJson);
                } else if (type.equals("Weld")) {
                        return json.fromJson(WeldDef.class, defAsJson);
                } else if (type.equals("Rope")) {
                        return json.fromJson(RopeDef.class, defAsJson);
                } else if (type.equals("Polygon")) {
                        return json.fromJson(PolygonDef.class, defAsJson);
                } else if (type.equals("Circle")) {
                        return json.fromJson(CircleDef.class, defAsJson);
                }
                return null;

        }


        public static CircleDef circleToDefinition(Circle circle) {
                CircleDef circleDef = new CircleDef();

                thingWithBodyToDefinition(circle, circleDef);

                circleDef.radius = circle.radius;

                return circleDef;
        }


        public static PolygonDef polygonToDefinition(Polygon polygon) {
                PolygonDef polygonDef = new PolygonDef();

                thingWithBodyToDefinition(polygon, polygonDef);

                Array<Vector2> vertices = polygon.getVertices();
                polygonDef.setVertices(vertices);

                return polygonDef;
        }


        public static void thingWithBodyToDefinition(ThingWithBody thingWithBody, ThingWithBodyDef def) {
                Body body = thingWithBody.getBody();
                Fixture firstFixture = body.getFixtureList().first();

                thingToDefinition(thingWithBody, def);

                def.active = body.isActive();
                def.angle = body.getAngle();
                def.angularDamping = body.getAngularDamping();
                def.antiCollisionGroup = thingWithBody.getAntiCollisionGroup();
                def.awake = body.isAwake();
                def.bullet = body.isBullet();
                def.categoryBits = firstFixture.getFilterData().categoryBits;
                def.density = firstFixture.getDensity();
                def.fixedRotation = body.isFixedRotation();
                def.friction = firstFixture.getFriction();
                def.groupIndex = firstFixture.getFilterData().groupIndex;
                def.linearDamping = body.getLinearDamping();
                def.maskBits = firstFixture.getFilterData().maskBits;
                def.position.set(body.getPosition());
                def.restitution = firstFixture.getRestitution();
                def.type = body.getType();


        }


        public static RopeDef ropeToDefinition(Rope rope) {
                RopeDef ropeDef = new RopeDef();

                jointToDefinition(rope, ropeDef);

                ropeDef.maxLength = rope.getMaxLength();

                return ropeDef;
        }


        public static WeldDef weldToDefinition(Weld weld) {
                WeldDef weldDef = new WeldDef();

                jointToDefinition(weld, weldDef);

                weldDef.referenceAngle = weld.weldJointDef.referenceAngle;

                return weldDef;
        }


        public static HingeDef hingeToDefinition(Hinge hinge) {
                HingeDef hingeDef = new HingeDef();
                RevoluteJoint joint = hinge.getJoint();

                jointToDefinition(hinge, hingeDef);

                hingeDef.enableLimit = joint.isLimitEnabled();
                hingeDef.enableMotor = joint.isMotorEnabled();
                hingeDef.lowerLimit = joint.getLowerLimit();
                hingeDef.upperLimit = joint.getUpperLimit();
                hingeDef.motorSpeed = joint.getMotorSpeed();
                hingeDef.maxMotorTorque = joint.getMaxMotorTorque();

                return hingeDef;
        }


        public static void jointToDefinition(JointThing hinge, JointThingDef jointThingDef) {
                thingToDefinition(hinge, jointThingDef);

                jointThingDef.idThingA = hinge.getThingA().getID();
                jointThingDef.idThingB = hinge.getThingB().getID();
                jointThingDef.localAnchorA.set((hinge.getLocalAnchorA()));
                jointThingDef.localAnchorB.set((hinge.getLocalAnchorB()));


        }


        public static void thingToDefinition(Thing thing, ThingDef thingDef) {


                thingDef.id = thing.getID();
                //thingDef.userData = thing.getUserData();

        }

}
