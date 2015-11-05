/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.box2dinside.things.def;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJoint;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ObjectMap;
import org.box2dinside.things.*;
import org.box2dinside.world.BoxWorld;

/**
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
                        return PolygonDefParser.polygonToPolygonDef((Polygon) thing);
                } else if (thing instanceof Circle) {
                        return CircleDefParser.circleToDefinition((Circle) thing);
                } else if (thing instanceof Hinge) {
                        return HingeDefParser.hingeToHingeDef((Hinge) thing);
                } else if (thing instanceof Rope) {
                        return RopeDefParser.ropeToRopeDef((Rope) thing);
                } else if (thing instanceof Weld) {
                        return WeldDefParser.weldToWeldDef((Weld) thing);
                }

                return null;
        }

        public static Array<Thing> definitionsToThings(BoxWorld boxWorld,Array<ThingDef> definitions) {
                Array<Thing> things = new Array<Thing>(true, definitions.size, Thing.class);
                for (ThingDef def : definitions) {
                        Thing thing = definitionToThing(boxWorld,def);
                        things.add(thing);
                }
                return things;
        }

        public static Thing definitionToThing(BoxWorld boxWorld, ThingDef def) {
                if (def instanceof PolygonDef) {
                        return PolygonDefParser.polygonDefToPolygon(boxWorld,(PolygonDef) def);
                } else if (def instanceof CircleDef) {
                        return CircleDefParser.definitionToCircle(boxWorld,(CircleDef) def);
                } else if (def instanceof HingeDef) {
                        return HingeDefParser.hingeDefToHinge(boxWorld,(HingeDef) def);
                } else if (def instanceof WeldDef) {
                        return WeldDefParser.weldDefToWeld(boxWorld,(WeldDef) def);
                } else if (def instanceof RopeDef) {
                        return RopeDefParser.ropeDefToRope(boxWorld,(RopeDef) def);
                }
                return null;
        }

        public static ThingDef jsonToDefinition(String thingsAsJson) {

                ObjectMap<String, String> map = json.fromJson(ObjectMap.class, thingsAsJson);

                String type = map.get("TypeOfThing");
                String defAsJson = map.get("Def");

                ThingDef def = jsonToDefinition(defAsJson, type);

                return def;

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
                        return HingeDefParser.jSonToHingeDef(defAsJson);
                } else if (type.equals("Weld")) {
                        return WeldDefParser.jSonToWeldDef(defAsJson);
                } else if (type.equals("Rope")) {
                        return RopeDefParser.jSonToRopeDef(defAsJson);
                } else if (type.equals("Polygon")) {
                        return PolygonDefParser.jSonToPolygonDef(defAsJson);
                } else if (type.equals("Circle")) {
                        return CircleDefParser.jSonToDefinition(defAsJson);
                }
                return null;

        }


        protected static class CircleDefParser {

                public static CircleDef jSonToDefinition(String jsonString) {
                        CircleDef circleDef = json.fromJson(CircleDef.class, jsonString);

                        return circleDef;
                }

                public static Circle definitionToCircle(BoxWorld boxWorld,CircleDef circleDef) {
                        Circle circle = new Circle(boxWorld,circleDef);
                        return circle;
                }

                public static CircleDef circleToDefinition(Circle circle) {
                        CircleDef circleDef = new CircleDef();

                        ThingWithBodyParser.thingWithBodyToThingWithBodyDef(circle, circleDef);

                        circleDef.radius = circle.radius;

                        return circleDef;
                }
        }

        protected static class PolygonDefParser {

                public static PolygonDef jSonToPolygonDef(String jsonString) {
                        PolygonDef polygonDef = json.fromJson(PolygonDef.class, jsonString);

                        return polygonDef;
                }

                public static Polygon polygonDefToPolygon(BoxWorld boxWorld,PolygonDef polygonDef) {
                        Polygon polygon = new Polygon(boxWorld,polygonDef);
                        return polygon;
                }

                public static PolygonDef polygonToPolygonDef(Polygon polygon) {
                        PolygonDef polygonDef = new PolygonDef();

                        ThingWithBodyParser.thingWithBodyToThingWithBodyDef(polygon, polygonDef);

                        Array<Vector2> vertices = polygon.getVertices();
                        polygonDef.setVertices(vertices);

                        return polygonDef;
                }
        }

        protected static class ThingWithBodyParser {

                public static void thingWithBodyToThingWithBodyDef(ThingWithBody thingWithBody, ThingWithBodyDef def) {
                        Body body = thingWithBody.getBody();
                        Fixture firstFixture = body.getFixtureList().first();

                        ThingDefParser.thingToThingDef(thingWithBody, def);

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
        }

        protected static class RopeDefParser {

                public static RopeDef jSonToRopeDef(String jsonString) {
                        RopeDef ropeDef = json.fromJson(RopeDef.class, jsonString);

                        return ropeDef;
                }

                public static Rope ropeDefToRope(BoxWorld boxWorld,RopeDef ropeDef) {
                        Rope rope = new Rope(boxWorld,ropeDef);
                        return rope;
                }

                public static RopeDef ropeToRopeDef(Rope rope) {
                        RopeDef ropeDef = new RopeDef();

                        JointThingDefParser.jointToJointThingDef(rope, ropeDef);

                        ropeDef.maxLength = rope.getMaxLength();

                        return ropeDef;
                }
        }

        protected static class WeldDefParser {

                public static WeldDef jSonToWeldDef(String jsonString) {
                        WeldDef weldDef = json.fromJson(WeldDef.class, jsonString);

                        return weldDef;
                }

                public static Weld weldDefToWeld(BoxWorld boxWorld,WeldDef weldDef) {
                        Weld weld = new Weld(boxWorld,weldDef);
                        return weld;
                }

                public static WeldDef weldToWeldDef(Weld weld) {
                        WeldDef weldDef = new WeldDef();

                        JointThingDefParser.jointToJointThingDef(weld, weldDef);

                        weldDef.referenceAngle = weld.weldJointDef.referenceAngle;

                        return weldDef;
                }
        }

        protected static class HingeDefParser {

                public static HingeDef jSonToHingeDef(String jsonString) {
                        HingeDef hingeDef = json.fromJson(HingeDef.class, jsonString);

                        return hingeDef;
                }

                public static Hinge hingeDefToHinge(BoxWorld boxWorld,HingeDef hingeDef) {
                        Hinge hinge = new Hinge(boxWorld,hingeDef);
                        return hinge;
                }

                public static HingeDef hingeToHingeDef(Hinge hinge) {
                        HingeDef hingeDef = new HingeDef();
                        RevoluteJoint joint = hinge.getJoint();

                        JointThingDefParser.jointToJointThingDef(hinge, hingeDef);

                        hingeDef.enableLimit = joint.isLimitEnabled();
                        hingeDef.enableMotor = joint.isMotorEnabled();
                        hingeDef.lowerLimit = joint.getLowerLimit();
                        hingeDef.upperLimit = joint.getUpperLimit();
                        hingeDef.motorSpeed = joint.getMotorSpeed();
                        hingeDef.maxMotorTorque = joint.getMaxMotorTorque();

                        return hingeDef;
                }
        }

        protected static class JointThingDefParser {

                public static void jointToJointThingDef(JointThing hinge, JointThingDef jointThingDef) {
                        ThingDefParser.thingToThingDef(hinge, jointThingDef);

                        jointThingDef.idThingA = hinge.getThingA().getID();
                        jointThingDef.idThingB = hinge.getThingB().getID();
                        jointThingDef.localAnchorA = new Vector2(hinge.getLocalAnchorA());
                        jointThingDef.localAnchorB = new Vector2(hinge.getLocalAnchorB());


                }
        }

        protected static class ThingDefParser {

                public static void thingToThingDef(Thing thing, ThingDef thingDef) {


                        thingDef.id = thing.getID();
                        thingDef.userData = thing.getUserData();

                }
        }
}
