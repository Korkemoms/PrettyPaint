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
package org.ams.testapps.paintandphysics.cardhouse;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.joints.MouseJoint;
import com.badlogic.gdx.physics.box2d.joints.MouseJointDef;
import com.badlogic.gdx.utils.Array;
import org.ams.core.CoordinateHelper;
import org.ams.core.Util;
import org.ams.paintandphysics.things.PPPolygon;
import org.ams.physics.things.Thing;
import org.ams.physics.things.ThingWithBody;
import org.ams.physics.world.BoxWorld;
import org.ams.physics.world.WorldUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * Tool for moving cards around in order to build a card house.
 */
public class CardMover extends InputAdapter {

        private boolean debug = false;

        private OrthographicCamera camera; // the camera used to display this world
        private BoxWorld world; // the world with cards in

        /** The card currently being moved. */
        private PPPolygon activeCard;
        /** The cards currently handled by the mover(cheat cards and active card). */
        private Set<PPPolygon> cards = new HashSet<PPPolygon>();
        /** Mouse joints that hold the active card and the cheat cards in place. */
        private Array<MouseJoint> mouseJointStorage = new Array<MouseJoint>();


        private Vector2 offset = new Vector2();


        private float auxMouseJointDistance = 20;

        // for turning

        /** Circle for turning. */
        private TurnCircle turnCircleInner;
        private TurnCircle turnCircleOuter;


        private boolean turning = false;
        private boolean roundedTurning = false;
        private float angleOffset = 0;
        private float angleWhenRotating = 0;
        private float angleAtRotateStart;

        private float angleRounding = 5;

        // colors
        private Color[] cheatColors = new Color[]{Color.PINK, Color.ORANGE, Color.RED};
        private Color cardColor = Color.BLACK;

        private Array<CardMoverListener> cardMoverListeners = new Array<CardMoverListener>();

        // filters to select only active cards
        private WorldUtil.Filter userFilter;
        private WorldUtil.Filter filter = new WorldUtil.Filter() {
                @Override
                public boolean accept(Object o) {
                        if (userFilter != null && !userFilter.accept(o)) {
                                return false;
                        }
                        if (!(o instanceof ThingWithBody)) return false;

                        return ((ThingWithBody) o).getBody().getType() != BodyDef.BodyType.StaticBody;
                }
        };

        /**
         * Dispose all resources and nullify references.
         * Must be called when this object is no longer used.
         */
        public void dispose() {
                if (debug) debug("Disposing resources...");

                reset();

                world = null;
                camera = null;
                turnCircleInner = null;
                turnCircleOuter = null;
                mouseJointStorage = null;
                turnCircleInner = null;
                turnCircleOuter = null;

                if (cardMoverListeners != null) cardMoverListeners.clear();
                cardMoverListeners = null;

                if (debug) debug("Finished disposing resources.");
        }

        /** Set to filter out stuff you don't want to move. */
        public void setUserFilter(WorldUtil.Filter userFilter) {
                this.userFilter = userFilter;
        }

        /** Filters away stuff you don't want to move. */
        public WorldUtil.Filter getUserFilter() {
                return userFilter;
        }

        /**
         * Tool for moving cards around in order to build a card house.
         *
         * @param camera the camera that is used to draw the world.
         * @param world  the world with cards and hopefully some ground for the cards to be on.
         */
        public CardMover(OrthographicCamera camera, BoxWorld world) {
                if (debug) Gdx.app.setLogLevel(Application.LOG_DEBUG);
                debug("New instance.");

                turnCircleInner = new TurnCircle(camera, 2.2f, 0.8f);
                turnCircleInner.setDrawMarkers(false);

                turnCircleOuter = new TurnCircle(camera, 3, 0.5f);
                turnCircleOuter.setDrawAngleSquare(false);
                turnCircleOuter.setDrawMarkers(false);


                this.camera = camera;
                this.world = world;


        }

        public void setAngleRounding(float angleRounding) {
                this.angleRounding = angleRounding;
                turnCircleInner.setMarkerSpacing(angleRounding);
        }

        public float getAngleRounding() {
                return angleRounding;
        }

        /**
         * Colors of the cards that are controlled by this CardMover. The size of this array decides how many cards
         * can be controlled at once.
         */
        public void setCheatColors(Color[] cheatColors) {
                this.cheatColors = cheatColors;
        }

        /**
         * Colors of the cards that are controlled by this CardMover. The size of this array decides how many cards
         * can be controlled at once.
         */
        public Color[] getCheatColors() {
                return cheatColors;
        }

        /** The CardMover will give this color to the cards it releases. */
        public void setCardColor(Color cardColor) {
                this.cardColor = cardColor;
        }

        /** The CardMover will give this color to the cards it releases. */
        public Color getCardColor() {
                return cardColor;
        }

        private void debug(String text) {
                if (debug) Gdx.app.log("Background", text);
        }

        /** Release all cards. */
        public void reset() {
                if (debug) debug("Resetting.");
                for (int i = mouseJointStorage.size - 1; i >= 0; i--) {
                        MouseJoint mouseJoint = mouseJointStorage.get(i);
                        destroyJoint(mouseJoint);
                }
                mouseJointStorage.clear();
                setActiveCard(null);
        }

        /** Listeners for listening to card events. */
        public void addCardListener(CardMoverListener cardMoverListener) {
                if (debug) debug("New card listener.");
                cardMoverListeners.add(cardMoverListener);
        }

        /** Listeners for listening to card events. */
        public void removeCardListener(CardMoverListener cardMoverListener) {
                if (debug) debug("Card listener removed.");
                cardMoverListeners.removeValue(cardMoverListener, true);
        }

        /** Release a card. */
        public void releaseCard(PPPolygon card) {
                if (debug) debug("Releasing card.");
                if (card == null) return;
                Thing polygon = card.getPhysicsThing();
                if (polygon == null) return;

                destroyMouseJoints(polygon);
                notifyListenersAddedRemoved();
        }

        public void setTurnCircleInnerColor(Color color) {
                turnCircleInner.setColor(color);
        }

        public Color getTurnCircleInnerColor() {
                return turnCircleInner.getColor();
        }

        public void setTurnCircleOuterColor(Color color) {
                turnCircleOuter.setColor(color);
        }

        public Color getTurnCircleOuterColor() {
                return turnCircleOuter.getColor();
        }


        public void render(ShapeRenderer worldRenderer) {

                boolean show = activeCard != null && mouseJointStorage.size >= 2;

                turnCircleInner.setVisible(show);
                turnCircleOuter.setVisible(show);


                if (!show) return;


                turnCircleInner.setAngleSquareAngle(activeCard.getPhysicsThing().getBody().getAngle());
                turnCircleInner.draw(worldRenderer);

                turnCircleOuter.setAngleSquareAngle(activeCard.getPhysicsThing().getBody().getAngle());
                turnCircleOuter.draw(worldRenderer);


        }

        private boolean hasStoredJointForThisThing(Thing thing) {
                for (MouseJoint mouseJoint : mouseJointStorage) {
                        Thing t = (Thing) mouseJoint.getBodyB().getUserData();

                        if (t == thing) return true;
                }
                return false;
        }


        private void destroyMouseJoints(Thing thing) {
                if (debug) debug("Destroying mouse joints for thing " + thing + ".");

                // destroy all the ones controlled by this CardMover
                for (int i = mouseJointStorage.size - 1; i >= 0; i--) {
                        MouseJoint mouseJoint = mouseJointStorage.get(i);
                        Body b = mouseJoint.getBodyB();

                        if (thing.getBody() == b) {
                                destroyJoint(mouseJoint);
                                mouseJointStorage.removeIndex(i);
                        }
                }


                /*for (JointEdge jointEdge : thing.getBody().getJointList()) {
                        if (jointEdge.joint instanceof MouseJoint){

                                world.world.destroyJoint(jointEdge.joint);
                        }
                }*/

        }

        private void destroyJoint(MouseJoint mouseJoint) {
                world.world.destroyJoint(mouseJoint);
        }

        private void setActiveCard(PPPolygon activeCard) {
                this.activeCard = activeCard;
                updateTurnCircleVisibility();

                for (CardMoverListener cardMoverListener : cardMoverListeners) {
                        cardMoverListener.selected(activeCard);
                }
        }


        private void updateTurnCircleVisibility() {
                boolean visible = activeCard != null && mouseJointStorage.size >= 2;
                turnCircleInner.setVisible(visible);
                turnCircleOuter.setVisible(visible);

        }

        /** While there are too many mouse joints: destroy the mouse joint that was added first. */
        private void enforceStorageLimit() {

                int cards = getNumberOfThingsRepresentedInStore();

                while (cards > cheatColors.length) {
                        MouseJoint mouseJoint = mouseJointStorage.first();
                        destroyJoint(mouseJoint);
                        mouseJointStorage.removeIndex(0);

                        MouseJoint auxMouseJoint = mouseJointStorage.first();
                        destroyJoint(auxMouseJoint);
                        mouseJointStorage.removeIndex(0);

                        if (debug) debug("Enforcing storage limit, destroying mouse joints for one card.");

                        cards--;
                }

                updateTurnCircleVisibility();
        }

        /** Count the cards being controlled. */
        private int getNumberOfThingsRepresentedInStore() {
                Set<Body> thingCountingSet = new HashSet<Body>();

                for (MouseJoint mouseJoint : mouseJointStorage) {
                        Body body = mouseJoint.getBodyB();
                        thingCountingSet.add(body);
                }
                return thingCountingSet.size();

        }

        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                Vector2 touchCoordinates = CoordinateHelper.getWorldCoordinates(camera, screenX, screenY);

                // check if touch is for turning

                boolean onInner = turnCircleInner.isOnTurnCircle(touchCoordinates);
                boolean onOuter = turnCircleOuter.isOnTurnCircle(touchCoordinates);


                turning = onInner || onOuter;

                if (turning) {
                        roundedTurning = onInner;
                        // start turning routine

                        if (debug) debug("Turning card.");
                        MouseJoint mouseJoint = mouseJointStorage.get(mouseJointStorage.size - 2);
                        MouseJoint auxMouseJoint = mouseJointStorage.get(mouseJointStorage.size - 1);


                        Vector2 v = new Vector2(auxMouseJoint.getTarget()).sub(mouseJoint.getTarget());
                        float a = v.angleRad();

                        Vector2 w = new Vector2(touchCoordinates).sub(mouseJoint.getTarget());
                        float b = w.angleRad();

                        angleOffset = b - a;


                        return true;
                }


                // find card to move
                Thing touchedThing = WorldUtil.getClosestThingIntersectingCircle(
                        world.things, touchCoordinates.x, touchCoordinates.y,
                        Util.getTouchRadius(camera.zoom), filter);

                PPPolygon card = null;
                if (touchedThing == null && turnCircleInner.isInsideTurnCircle(touchCoordinates)) {
                        card = this.activeCard;
                } else if (touchedThing != null && touchedThing.getBody().isActive()) {
                        card = (PPPolygon) touchedThing.getUserData();
                }

                // cancel if no card
                if (card == null) {
                        setActiveCard(null);
                        return false;

                }


                if (debug) debug("Moving card.");

                boolean directTouch = touchedThing != null;
                boolean newCard = this.activeCard != card;

                if (newCard || directTouch) {
                        // prepare to move card with fresh mouse joints

                        resetColors();
                        setActiveCard(card);

                        // make new mouse joints
                        destroyMouseJoints(card.getPhysicsThing());
                        MouseJoint mouseJoint = createMouseJoint(touchCoordinates);
                        mouseJointStorage.add(mouseJoint);
                        mouseJointStorage.add(createAuxMouseJoint(touchCoordinates));

                        // move turn circle
                        updateTurnCirclePos(mouseJoint.getTarget());

                        // remove oldest joints
                        enforceStorageLimit();

                        // let listeners know which card is added and which was removed
                        notifyListenersAddedRemoved();

                        // force it to move
                        Body body = card.getPhysicsThing().getBody();
                        body.setAwake(true);
                        body.setActive(true);

                        // reset some stuff
                        offset.set(0, 0);
                        angleWhenRotating = 0;

                        angleAtRotateStart = activeCard.getPhysicsThing().getBody().getAngle();


                } else if (turnCircleInner.isInsideTurnCircle(touchCoordinates)) {
                        // just update offsets for current card
                        offset.set(touchCoordinates).sub(mouseJointStorage.get(mouseJointStorage.size - 2).getTarget());

                }

                return true;
        }


        private void updateTurnCirclePos(Vector2 mouseJointTarget) {
                Vector2 pos = new Vector2(mouseJointTarget);
                turnCircleInner.setPosition(pos);
                turnCircleOuter.setPosition(pos);


                for (CardMoverListener cardMoverListener : cardMoverListeners) {
                        cardMoverListener.turnCirclePositionChanged(pos);
                }
        }

        @Override
        public boolean touchDragged(int screenX, int screenY, int pointer) {
                if (activeCard == null) return false;
                if (mouseJointStorage.size < 2) return false;

                Vector2 worldCoordinates = CoordinateHelper.getWorldCoordinates(camera, screenX, screenY);


                MouseJoint mouseJoint = mouseJointStorage.get(mouseJointStorage.size - 2);
                MouseJoint auxMouseJoint = mouseJointStorage.get(mouseJointStorage.size - 1);


                if (turning) {
                        Vector2 v = new Vector2(worldCoordinates).sub(mouseJoint.getTarget());
                        float a = v.angleRad();

                        angleWhenRotating = a - angleOffset;

                        if (roundedTurning) {
                                // rounding
                                float round = angleRounding * MathUtils.degreesToRadians;
                                angleWhenRotating = Util.roundToNearestN(angleWhenRotating, round);
                                float f = angleAtRotateStart % round;
                                angleWhenRotating -= f;
                        }


                        auxMouseJoint.setTarget(new Vector2(auxMouseJointDistance, 0).rotateRad(angleWhenRotating).add(mouseJoint.getTarget()));

                } else {

                        if (turnCircleInner.isOutsideTurnCircle(worldCoordinates)) return false;

                        mouseJoint.setTarget(new Vector2(worldCoordinates).sub(offset));

                        auxMouseJoint.setTarget(new Vector2(auxMouseJointDistance, 0).rotateRad(angleWhenRotating)
                                .add(worldCoordinates).sub(offset));


                        updateTurnCirclePos(mouseJoint.getTarget());
                }


                return true;
        }


        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) {

                return activeCard != null;
        }


        // not super happy about this solution
        private void notifyListenersAddedRemoved() {
                if (debug) debug("Updating cards.");

                Set<PPPolygon> newCards = new HashSet<PPPolygon>();

                for (int i = 0; i < mouseJointStorage.size; i++) {
                        MouseJoint mouseJoint = mouseJointStorage.get(i);

                        Color color = cheatColors[(mouseJointStorage.size - i - 1) / 2];


                        Body b = mouseJoint.getBodyB();
                        Thing thing = (Thing) b.getUserData();
                        PPPolygon card = (PPPolygon) thing.getUserData();


                        updateColor(card, color);

                        newCards.add(card);
                }


                // notify listeners about removed cards
                for (PPPolygon card : cards) {
                        boolean removed = !newCards.contains(card);
                        if (removed) {

                                updateColor(card, cardColor); // reset color on removed cards

                                for (CardMoverListener cardMoverListener : cardMoverListeners) {
                                        cardMoverListener.removed(card);
                                }
                        }
                }

                // notify listeners about added cards
                for (PPPolygon card : newCards) {
                        boolean newCard = !cards.contains(card);
                        if (newCard) {
                                for (CardMoverListener cardMoverListener : cardMoverListeners) {
                                        cardMoverListener.added(card);
                                }
                        }
                }

                cards = newCards;


        }

        private void resetColors() {
                for (MouseJoint mouseJoint : mouseJointStorage) {
                        resetColor(mouseJoint);
                }
        }

        private void resetColor(MouseJoint mouseJoint) {
                Body b = mouseJoint.getBodyB();
                Thing thing = (Thing) b.getUserData();

                PPPolygon card = (PPPolygon) thing.getUserData();

                updateColor(card, cardColor);
        }

        private void updateColor(PPPolygon card, Color color) {
                card.getOutlinePolygons().peek().setColor(color);
                /*for (OutlinePolygon outlinePolygon : card.getOutlinePolygons()) {
                        outlinePolygon.setColor(color);
                }*/

        }

        public boolean isTurnCircleVisible() {
                return turnCircleInner.isVisible();
        }

        private MouseJoint createMouseJoint(Vector2 touchCoordinates) {
                Body body = activeCard.getPhysicsThing().getBody();

                MouseJointDef mouseJointDef = new MouseJointDef();
                mouseJointDef.target.set(touchCoordinates);
                mouseJointDef.bodyA = world.getJointAnchor().getBody();
                mouseJointDef.bodyB = body;
                mouseJointDef.collideConnected = true;
                mouseJointDef.maxForce = 300 * body.getMass();
                mouseJointDef.dampingRatio = 0;

                return (MouseJoint) world.world.createJoint(mouseJointDef);
        }

        private MouseJoint createAuxMouseJoint(Vector2 touchCoordinates) {
                Body body = activeCard.getPhysicsThing().getBody();

                Vector2 auxCoordinates = new Vector2(touchCoordinates).add(auxMouseJointDistance, 0);

                MouseJointDef mouseJointDef = new MouseJointDef();
                mouseJointDef.target.set(auxCoordinates);
                mouseJointDef.bodyA = world.getJointAnchor().getBody();
                mouseJointDef.bodyB = body;
                mouseJointDef.collideConnected = true;
                mouseJointDef.maxForce = 300 * body.getMass();
                mouseJointDef.dampingRatio = 0;

                return (MouseJoint) world.world.createJoint(mouseJointDef);
        }

        public static class CardMoverListener {
                void added(PPPolygon card) {
                }

                void removed(PPPolygon card) {
                }

                void selected(PPPolygon card) {
                }

                void turnCirclePositionChanged(Vector2 pos) {
                }


        }

}
