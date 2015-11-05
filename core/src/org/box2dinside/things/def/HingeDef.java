/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.box2dinside.things.def;

import com.badlogic.gdx.math.Vector2;
import org.box2dinside.things.ThingWithBody;

/**
 *
 * @author Andreas
 */
public class HingeDef extends JointThingDef {

        /**
         * Set the IDs and anchors using a world anchor point.
         */
        @Override
        public void initialize(ThingWithBody body1, ThingWithBody body2, Vector2 anchor) {
                // sets anchors and ids
                super.initialize(body1, body2, anchor);
        }
        
        public boolean enableLimit;
        public float upperLimit;
        public float lowerLimit;
        public boolean enableMotor;
        public float motorSpeed;
        public float maxMotorTorque;
}
