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
public class WeldDef extends JointThingDef {

        /**
         * Set the IDs, anchors, and reference angle using a world anchor point.
         */
        @Override
        public void initialize(ThingWithBody body1, ThingWithBody body2, Vector2 anchor) {
                // sets anchors and ids
                super.initialize(body1, body2, anchor);
                
                referenceAngle = body2.getBody().getAngle() - body1.getBody().getAngle();
        }

        public float referenceAngle;
}
