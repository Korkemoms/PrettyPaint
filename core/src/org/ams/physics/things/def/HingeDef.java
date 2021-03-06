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
import org.ams.physics.things.ThingWithBody;

/**
 * The main purpose of these definitions is to make it easy so save and load things.
 *
 * @author Andreas
 */
public class HingeDef extends JointThingDef {


        public boolean enableLimit;
        public float upperLimit;
        public float lowerLimit;
        public boolean enableMotor;
        public float motorSpeed;
        public float maxMotorTorque;

        public HingeDef() {
                super();
        }

        public HingeDef(HingeDef toCopy) {
                super(toCopy);
                enableLimit = toCopy.enableLimit;
                upperLimit = toCopy.upperLimit;
                lowerLimit = toCopy.lowerLimit;
                enableMotor = toCopy.enableMotor;
                motorSpeed = toCopy.motorSpeed;
                maxMotorTorque = toCopy.maxMotorTorque;
        }

        /**
         * Set the IDs and anchors using a world anchor point.
         */
        @Override
        public void initialize(ThingWithBody body1, ThingWithBody body2, Vector2 anchor) {
                // sets anchors and ids
                super.initialize(body1, body2, anchor);
        }

        @Override
        public ThingDef getCopy() {
                return new HingeDef(this);
        }
}
