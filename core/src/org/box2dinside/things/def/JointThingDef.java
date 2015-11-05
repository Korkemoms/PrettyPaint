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
public abstract class JointThingDef extends ThingDef {

        /**
         * Set the IDs and anchors using a world anchor point.
         */
        public void initialize(ThingWithBody body1, ThingWithBody body2, Vector2 anchor) {

                idThingA = body1.getID();
                idThingB = body2.getID();

                this.localAnchorA = new Vector2(body1.getBody().getLocalPoint(anchor));
                this.localAnchorB = new Vector2(body2.getBody().getLocalPoint(anchor));
        }

        public int idThingA;
        public int idThingB;
        public Vector2 localAnchorA;
        public Vector2 localAnchorB;
}
