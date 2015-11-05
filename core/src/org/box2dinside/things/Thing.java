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
package org.box2dinside.things;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import org.box2dinside.world.BoxWorld;

/**
 * @author Andreas
 */
public interface Thing {

        /**
         * The box2d body of this item. Is null for things that have no
         * body(for example a hinge).
         */
        Body getBody();

        int getID();

        void setID(int id);

        void setUserData(Object userData);

        Object getUserData();


        /**
         * Joints do not have bodies. Circles and polygons do.
         */
        boolean hasBody();

        /**
         * Removes any references to all box2d objects so that the underlying
         * box2d objects can be deleted. May also dispose other resources later.
         */
        void dispose();


        /**
         * Sets the smoothed-physics-position and smoothed-physics-angle
         * variables. They are set by linearly interpolating between previous
         * and newest physics state, where the alpha value is set by looking at
         * how far behind the rendering is to the physics state.
         */
        void interpolate(float f);

        /**
         * Update the previous-physics-position and previous-physics-angle
         * variables. These are then later used to for linear interpolation
         * between previous and newest physics state. This is very useful when
         * the frame-rate is varying.
         */
        void storePreviousState();

        /**
         * Should be called by the BoxWorld after this thing has been added to
         * it. In this method the thing can prepare itself for a life in the
         * BoxWorld.
         */
        void thingHasBeenAddedToBoxWorld(BoxWorld boxWorld);


}
