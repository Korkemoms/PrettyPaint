/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.box2dinside.things;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Joint;

/**
 *
 * @author Andreas
 */
public interface JointThing extends Thing {

        ThingWithBody getThingA();

        ThingWithBody getThingB();

        Vector2 getAnchorA();

        Vector2 getAnchorB();

        Vector2 getLocalAnchorA();

        Vector2 getLocalAnchorB();

        Joint getJoint();

        Vector2 getInterpolatedPosA();

        Vector2 getInterpolatedPosB();




}
