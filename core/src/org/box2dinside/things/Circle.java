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

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.utils.Array;
import org.box2dinside.things.def.CircleDef;
import org.box2dinside.world.BoxWorld;
import org.core.Util;

/**
 * @author Andreas
 */
public class Circle extends AbstractThingWithBody implements ThingWithBody {

        public final float radius;

        private final Rectangle physicsBoundingBox = new Rectangle();

        @SuppressWarnings("LeakingThisInConstructor")
        public Circle(BoxWorld game, CircleDef def) {
                super(game, def);

                this.radius = def.radius;

                postInstantiation(def);
        }

        @Override
        public void thingHasBeenAddedToBoxWorld(BoxWorld boxWorld) {
                // the body is created in this supercall
                super.thingHasBeenAddedToBoxWorld(boxWorld);

                // the last thing to do is to create fixtures
                CircleShape shape = new CircleShape();
                shape.setRadius(radius);
                fixtureDef.shape = shape;
                Fixture fixture = body.createFixture(fixtureDef);
                fixture.setUserData(this);
                shape.dispose();

        }

        @Override
        public String toString() {
                return "C" + getID() + " R" + radius;
        }

        @Override
        public Rectangle getPhysicsBoundingBox() {
                physicsBoundingBox.x = body.getPosition().x;
                physicsBoundingBox.x -= radius;
                physicsBoundingBox.width = radius * 2f;

                physicsBoundingBox.y = body.getPosition().y;
                physicsBoundingBox.y -= radius;
                physicsBoundingBox.height = radius * 2f;

                return physicsBoundingBox;
        }

}
