/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.box2dinside.things.def;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;

/**
 * @author Andreas
 */
public abstract class ThingWithBodyDef extends ThingDef {

        public final Vector2 position = new Vector2();
        public float angle;

        public int antiCollisionGroup;
        public short groupIndex;
        public short maskBits;
        public short categoryBits;

        public float angularDamping = 0.1f;
        public float linearDamping = 0.1f;
        public float density = 2f;
        public float friction = 1f;
        public float restitution = 0.2f;

        public boolean active;
        public boolean awake;

        public boolean bullet;
        public boolean fixedRotation;

        public BodyDef.BodyType type = BodyDef.BodyType.DynamicBody;


}
