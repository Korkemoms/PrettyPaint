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
import org.ams.physics.things.JointThing;
import org.ams.physics.things.ThingWithBody;

/**
 * The main purpose of these definitions is to make it easy so save and load things.
 *
 * @author Andreas
 */
public abstract class JointThingDef extends ThingDef {



        public int idThingA;
        public int idThingB;
        public final Vector2 localAnchorA = new Vector2();
        public final Vector2 localAnchorB = new Vector2();

        public JointThingDef(){
                super();
        }

        public JointThingDef(JointThingDef toCopy){
                super(toCopy);
                idThingA = toCopy.idThingA;
                idThingB = toCopy.idThingB;
                localAnchorA.set(toCopy.localAnchorA);
                localAnchorB.set(toCopy.localAnchorB);
        }

        /**
         * Set the IDs and anchors using a world anchor point.
         */
        public void initialize(ThingWithBody body1, ThingWithBody body2, Vector2 anchor) {

                idThingA = body1.getID();
                idThingB = body2.getID();

                this.localAnchorA.set(body1.getBody().getLocalPoint(anchor));
                this.localAnchorB.set(body2.getBody().getLocalPoint(anchor));
        }

}
