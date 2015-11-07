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
package org.ams.physics.things;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/**
 * A thing that takes up space in the world. Unlike joints or ropes that take up
 * no space. Underneath is a box2d body.
 *
 * @author Andreas
 */
public interface ThingWithBody extends Thing {

        /**
         * If you give two different things the same AntiCollisionGroup (except
         * 0), they will never collide.
         */
        void setAntiCollisionGroup(int i);

        /**
         * If you give two different things the same AntiCollisionGroup (except
         * 0), they will never collide.
         */
        int getAntiCollisionGroup();


        /**
         * A box that is as small as possible while still containing this
         * thing.
         */
        Rectangle getPhysicsBoundingBox();

        void setTransform(float x, float y, float angle);

        /**
         * Get the smoothed angle of this thing. It is an interpolated value
         * between previous and newest physics-angle.
         */
        float getInterpolatedAngle();

        /**
         * Get the smoothed position of this thing. It is an interpolated value
         * between previous and newest physics-position.
         */
        Vector2 getInterpolatedPosition();

        public Vector2 getPosition();

        public float getAngle();
}
