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

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import org.ams.physics.things.def.ThingDef;
import org.ams.physics.world.BoxWorld;

/**
 * @author Andreas
 */
public abstract class AbstractThing implements Thing {

        private int ID;

        protected final Vector2 interpolatedPosition = new Vector2();
        protected float interpolatedAngle = 0f;

        protected final Vector2 lastPhysicsPos = new Vector2();
        protected float lastPhysicsAngle = 0;

        private Object userData;

        protected ThingDef definitionFromInitialization;

        protected BoxWorld boxWorld;


        public AbstractThing(ThingDef def) {
                initFromDefinition(def);
        }

        protected void initFromDefinition(ThingDef def){
                definitionFromInitialization = def;
                userData = def.userData;
        }

        @Override
        public void thingHasBeenAddedToBoxWorld(BoxWorld boxWorld) {
                this.boxWorld = boxWorld;
                ID = boxWorld.getUnusedID();
        }

        @Override
        public Object getUserData() {
                return userData;
        }

        @Override
        public void setUserData(Object userData) {
                this.userData = userData;
        }

        @Override
        public int getID() {
                return ID;
        }

        @Override
        public void setID(int id) {
                this.ID = id;
        }

        @Override
        public Body getBody() {
                return null;
        }

        protected void postInstantiation(ThingDef def) {
        }


        @Override
        public void dispose() {
        }


}
