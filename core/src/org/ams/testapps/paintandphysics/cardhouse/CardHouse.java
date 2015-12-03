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
package org.ams.testapps.paintandphysics.cardhouse;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.ChainShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import org.ams.core.CameraNavigator;
import org.ams.core.CoordinateHelper;
import org.ams.core.Timer;
import org.ams.core.Util;
import org.ams.paintandphysics.things.PPPolygon;
import org.ams.paintandphysics.world.PPWorld;
import org.ams.physics.things.Polygon;
import org.ams.physics.things.Thing;
import org.ams.physics.things.ThingWithBody;
import org.ams.physics.things.def.DefParser;
import org.ams.physics.things.def.PolygonDef;
import org.ams.physics.things.def.ThingDef;
import org.ams.physics.tools.BodyMover;
import org.ams.physics.world.WorldUtil;
import org.ams.physics.world.def.BoxWorldDef;
import org.ams.prettypaint.*;

import java.util.Set;

/**
 * Card House game. It can be started as an independent application(for debugging) or
 * it can be run from another {@link ApplicationAdapter}.
 */
public class CardHouse extends ApplicationAdapter {

        // stored for access to settings
        private CardHouseDef cardHouseDef;

        // camera stuff
        private OrthographicCamera camera;
        private CameraNavigator cameraNavigator;
        private InputMultiplexer inputMultiplexer;

        // card mover stuff
        private CardMover cardMover;
        private ShapeRenderer shapeRenderer; // needed for card mover

        // world and drawing of it
        private PPWorld world;
        private PrettyPolygonBatch polygonBatch;
        private TextureRegion groundTexture;

        private Array<PPPolygon> coloredCards = new Array<PPPolygon>();

        //
        private boolean debug = false;
        private boolean independentApplication = false;
        private boolean paused = false;

        private Timer timer;
        private Tips tips;


        // refill card related
        private PPPolygon refillCard;
        private InputAdapter refillCardClickListener;
        private WorldUtil.Filter onlyThingWithBodyFilter = new WorldUtil.Filter() {
                @Override
                public boolean accept(Object o) {
                        return o instanceof ThingWithBody;
                }
        };


        // variables used for drawing the touches
        private boolean drawTouch = false;
        private InputProcessor touchListener;
        private final Vector2 touchPos = new Vector2();
        private float touchRadius = 1;

        /**
         * Card House game. It can be started as an independent application(for debugging) or
         * it can be run from another {@link ApplicationAdapter}.
         */
        public CardHouse() {
                if (debug) Gdx.app.setLogLevel(Application.LOG_DEBUG);
        }

        /**
         * Dispose all resources and nullify references.
         * Must be called when this object is no longer used.
         */
        @Override
        public void dispose() {
                if (debug) debug("Disposing resources.");

                if (inputMultiplexer != null) {
                        if (cardMover != null)
                                inputMultiplexer.removeProcessor(cardMover);
                        if (cameraNavigator != null)
                                inputMultiplexer.removeProcessor(cameraNavigator);
                        if (refillCardClickListener != null)
                                inputMultiplexer.removeProcessor(refillCardClickListener);


                }


                if (groundTexture != null) groundTexture.getTexture().dispose();
                groundTexture = null;

                if (polygonBatch != null) polygonBatch.dispose();
                polygonBatch = null;

                if (shapeRenderer != null) shapeRenderer.dispose();
                shapeRenderer = null;

                if (cardMover != null) cardMover.dispose();
                cardMover = null;

                if (world != null) world.dispose();
                world = null;

                if (independentApplication && tips != null)
                        tips.dispose();
                tips = null;


                inputMultiplexer = null;

                cameraNavigator = null;

                if (debug) debug("Finished disposing resources.");
        }

        /**
         * If you want to run this instance from another
         * {@link ApplicationAdapter} use {@link #create(InputMultiplexer, Tips, CardHouseDef)} instead.
         */
        @Override
        public void create() {
                if (debug) debug("Creating independent application.");

                independentApplication = true;
                Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);


                create(new InputMultiplexer(), null, new CardHouseDef());

                Gdx.input.setInputProcessor(inputMultiplexer);

        }


        /**
         * Use this method to create a new card house from another {@link ApplicationAdapter}.
         *
         * @param inputMultiplexer {@link InputProcessor}'s will be added.
         *                         They are removed again when {@link #dispose()} is called.
         * @param tips             Use the same tips for several classes to avoid several appearing at the same time.
         * @param def              Settings and possibly some saved cards.
         */
        public void create(InputMultiplexer inputMultiplexer, Tips tips, CardHouseDef def) {
                if (debug) debug("Creating application.");

                this.cardHouseDef = def;
                this.tips = tips;

                timer = new Timer();

                // setup camera
                float w = Gdx.graphics.getWidth();
                float h = Gdx.graphics.getHeight();
                camera = new OrthographicCamera(10, 10 * (h / w));
                cameraNavigator = new CameraNavigator(camera);

                //
                polygonBatch = new PrettyPolygonBatch();
                shapeRenderer = new ShapeRenderer();
                shapeRenderer.setAutoShapeType(true);

                // create world
                world = new PPWorld();
                createJointAnchor();
                groundTexture = new TextureRegion(new Texture(cardHouseDef.groundTexture));
                addGround(groundTexture, def.groundWidth);

                //
                cardMover = createCardMover(def);

                // prepare refill card
                refillCard = createRefillCard();
                timer.runAfterNRender(new Runnable() {
                        @Override
                        public void run() {
                                refillCard.setVisible(true);
                        }
                }, 3);
                refillCardClickListener = createRefillCardClickListener(refillCard);

                // prepare input
                this.inputMultiplexer = inputMultiplexer;
                inputMultiplexer.addProcessor(refillCardClickListener);
                inputMultiplexer.addProcessor(cardMover);
                inputMultiplexer.addProcessor(cameraNavigator);

                // prepare colored cards updates
                CardMover.CardMoverListener cardMoverListener = new CardMover.CardMoverListener() {

                        @Override
                        public void removed(PPPolygon card) {
                                coloredCards.removeValue(card, true);
                        }

                        @Override
                        public void added(PPPolygon card) {
                                coloredCards.add(card);
                        }
                };
                cardMover.addCardListener(cardMoverListener);

                // possibly load some cards
                if (independentApplication) {
                        for (int i = 0; i < 10; i++) {
                                addCard(i / 3f, 0);
                        }
                }
                if (def.asJson != null) {
                        loadGame(def.asJson);
                }

                positionCamera();
        }


        /** Whether to draw the touches. For debugging. */
        public void setDrawTouch(boolean drawTouch) {
                this.drawTouch = drawTouch;

                // remove old stuff
                inputMultiplexer.removeProcessor(touchListener);

                if (drawTouch) {
                        if (shapeRenderer == null) {
                                shapeRenderer = new ShapeRenderer();
                                shapeRenderer.setAutoShapeType(true);
                        }

                        // listener for moving the circle
                        touchListener = new InputAdapter() {
                                void updateSizeAndPos(int screenX, int screenY) {
                                                touchPos.set(CoordinateHelper.getWorldCoordinates(
                                                        camera,  screenX, screenY));

                                                touchRadius = Util.getTouchRadius(camera.zoom);

                                }

                                @Override
                                public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                                        updateSizeAndPos(screenX, screenY);
                                        return false;
                                }

                                @Override
                                public boolean touchDragged(int screenX, int screenY, int pointer) {
                                        updateSizeAndPos(screenX, screenY);
                                        return false;
                                }

                        };

                        inputMultiplexer.addProcessor(0, touchListener);
                }
        }

        /** Whether the touches are drawn. For debugging. */
        public boolean isDrawingTouch() {
                return drawTouch;
        }


        /** Create a card mover that will let you move cards when you touch them. */
        public CardMover createCardMover(CardHouseDef def) {
                CardMover cardMover = new CardMover(camera, world.boxWorld);
                cardMover.setAngleRounding(def.angleRounding);
                cardMover.setCheatColors(def.cheatColors);
                cardMover.setCardColor(def.cardColor);
                cardMover.setTurnCircleOuterColor(def.turnCircleColor);
                cardMover.setTurnCircleInnerColor(new Color(def.turnCircleColor).lerp(Color.BLACK, 0.2f));

                cardMover.setUserFilter(new WorldUtil.Filter() {
                        @Override
                        public boolean accept(Object o) {
                                return o != refillCard.getPhysicsThing();
                        }
                });
                return cardMover;
        }


        /** Create a card that can be touched in order to make new cards. */
        private PPPolygon createRefillCard() {
                PPPolygon refillCard = addCard(0, 0);
                refillCard.getOutlinePolygons().first().setColor(cardHouseDef.refillCardColor);
                refillCard.getPhysicsThing().getBody().setActive(false);
                refillCard.setAngle(MathUtils.PI * 0.5f);
                refillCard.setVisible(false);
                return refillCard;
        }


        /**
         * Listens for clicks on the refillCard. If there is a click and nothing else is nearby
         * a new card is added.
         */
        private InputAdapter createRefillCardClickListener(final PPPolygon refillCard) {
                return new InputAdapter() {
                        @Override
                        public boolean touchDown(final int screenX, final int screenY, final int pointer, final int button) {

                                // see if the touch is on the refill card
                                Vector2 touchCoordinates = CoordinateHelper.getWorldCoordinates(camera, screenX, screenY);
                                Thing touchedThing = WorldUtil.getClosestThingIntersectingCircle(
                                        world.boxWorld.things, touchCoordinates.x, touchCoordinates.y,
                                        Util.getTouchRadius(camera.zoom), onlyThingWithBodyFilter);


                                if (touchedThing != refillCard.getPhysicsThing()) return false;
                                if (touchedThing == null) return false;

                                // the touch is on the refill card

                                // see if there are anything else nearby

                                Polygon polygon = (Polygon) refillCard.getPhysicsThing();
                                Rectangle rectangle = polygon.getPhysicsBoundingBox();

                                float pad = Util.getTouchRadius(camera.zoom) * 0.5f;

                                rectangle.height += pad * 2;
                                rectangle.width += pad * 2;
                                rectangle.x -= pad;
                                rectangle.y -= pad;

                                Set<Body> intersectingBodies = WorldUtil.getIntersectingBodies(rectangle, world.boxWorld.world);


                                for (Body body : intersectingBodies) {
                                        // there is something else nearby, cancel
                                        if (body != polygon.getBody()) return false;
                                }

                                // add a new card

                                Body body = refillCard.getPhysicsThing().getBody();
                                addCard(body.getPosition(), body.getAngle());


                                timer.runAfterNRender(new Runnable() {
                                        @Override
                                        public void run() {
                                                cardMover.touchDown(screenX, screenY, pointer, button);
                                        }
                                }, 1);

                                return true;
                        }
                };
        }

        private void debug(String text) {
                if (debug) {
                        Gdx.app.log("CardHouse", text);
                }
        }

        /**
         * These cards are also called cheat cards. They are held by {@link com.badlogic.gdx.physics.box2d.joints.MouseJoint}'s.
         */
        public Array<PPPolygon> getColoredCards() {
                return coloredCards;
        }


        /** The camera used for drawing the world. */
        public OrthographicCamera getCamera() {
                return camera;
        }

        /** When paused the {@link #world} is not stepped. */
        public void setPaused(boolean paused) {
                if (debug) debug(paused ? "Paused" : "Unpaused");

                this.paused = paused;

                // we don't want input when paused
                if (paused) {
                        inputMultiplexer.removeProcessor(refillCardClickListener);
                        inputMultiplexer.removeProcessor(cardMover);
                        inputMultiplexer.removeProcessor(cameraNavigator);

                } else {
                        inputMultiplexer.addProcessor(refillCardClickListener);
                        inputMultiplexer.addProcessor(cardMover);
                        inputMultiplexer.addProcessor(cameraNavigator);
                }
        }

        /** When paused the {@link #world} is not stepped. */
        public boolean isPaused() {
                return paused;
        }

        @Override
        public void render() {
                if (independentApplication) {
                        Gdx.gl20.glClearColor(1, 1, 1, 1);
                        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
                }

                // position the refill card
                Vector2 refillCardPos = CoordinateHelper.getWorldCoordinates(camera,
                        Gdx.graphics.getWidth() * 0.5f,
                        Gdx.graphics.getHeight() * 0.15f);
                refillCard.setPosition(refillCardPos);


                // step and draw world
                polygonBatch.begin(camera);
                if (!paused) world.step(Gdx.graphics.getDeltaTime());
                world.draw(polygonBatch);
                polygonBatch.end();


                shapeRenderer.begin();
                shapeRenderer.setProjectionMatrix(camera.combined);

                // draw turn circle
                cardMover.render(shapeRenderer);

                // draw touches
                if (drawTouch && Gdx.input.isButtonPressed(0)) {
                        shapeRenderer.set(ShapeRenderer.ShapeType.Line);
                        shapeRenderer.setColor(Color.RED);
                        shapeRenderer.circle(touchPos.x, touchPos.y, touchRadius, 20);
                }

                shapeRenderer.end();

                timer.step();

        }

        /** Get the height of the topmost part of a card. */
        public Vector2 getHeightOfCard(Body body, Vector2 result) {
                float hh = cardHouseDef.cardHeight * 0.5f;
                float hw = cardHouseDef.cardWidth * 0.5f;


                float top = body.getPosition().y;
                top += Math.max(Math.abs(Math.cos(body.getAngle()) * hh), hw);


                float angle = body.getAngle();
                float sin = (float) Math.sin(angle);
                float cos = (float) Math.cos(angle);


                float x = body.getPosition().x;
                if (cos < 0) x += sin * hh;
                else x -= sin * hh;


                return result.set(x, top);
        }

        /** Get the highest free(no joints) card. */
        public Body getHighestCard() {
                float top = cardHouseDef.groundY;
                Body highestBody = null;

                float hh = cardHouseDef.cardHeight * 0.5f;
                float hw = cardHouseDef.cardWidth * 0.5f;

                for (Thing thing : world.boxWorld.things) {
                        Body body = thing.getBody();

                        boolean isCard = body.isActive()
                                && body.getType() != BodyDef.BodyType.StaticBody;
                        if (!isCard) continue;

                        boolean isFreeCard = body.getJointList().size == 0;
                        if (!isFreeCard) continue;


                        float y = body.getPosition().y;
                        y += Math.max(Math.abs(Math.cos(body.getAngle()) * hh), hw);

                        if (y > top) {
                                top = y;
                                highestBody = body;
                        }

                }

                return highestBody;
        }

        /** Parse the cards into a json representation of a {@link BoxWorldDef}. */
        public String saveGame() {
                BoxWorldDef worldDef = new BoxWorldDef();

                for (Thing thing : world.boxWorld.things) {
                        // filter away things that are not cards
                        if (!(thing instanceof Polygon)) continue;
                        Polygon polygon = (Polygon) thing;

                        if (!polygon.getBody().isActive()) continue;
                        if (polygon.getBody().getType() == BodyDef.BodyType.StaticBody) continue;

                        // only cards here

                        ThingDef thingDef = DefParser.thingToDefinition(polygon);
                        worldDef.things.add(thingDef);
                }


                Json json = new Json();
                json.setIgnoreUnknownFields(true);

                return json.toJson(worldDef);
        }

        /**
         * Load cards from a json representation of a {@link BoxWorldDef}.
         * No ground is loaded. Nothing is removed, so this method should
         * only be used one time per instance of {@link CardHouse}.
         */
        private void loadGame(String asString) {
                Json json = new Json();


                BoxWorldDef worldDef = json.fromJson(BoxWorldDef.class, asString);

                Array<Thing> things = new Array<Thing>();

                for (ThingDef thingDef : worldDef.things) {
                        // filter away things that are not cards
                        // (there should not be any)

                        if (!(thingDef instanceof PolygonDef)) continue;
                        PolygonDef polygonDef = (PolygonDef) thingDef;

                        if (!polygonDef.active) continue;
                        if (polygonDef.type == BodyDef.BodyType.StaticBody) continue;

                        // only cards here

                        PPPolygon card = addCard(polygonDef.position, polygonDef.angle);
                        things.add(card.getPhysicsThing());

                }
        }

        /**
         * Positions the camera so it looks at the cards. If there are
         * no cards it looks at a default rectangle.
         */
        private void positionCamera() {
                // update camera width and height
                float h = Gdx.graphics.getHeight();
                float w = Gdx.graphics.getWidth();
                camera.setToOrtho(false, 10, 10 * (h / w));


                // find a bounding rectangle of all the cards
                Array<Thing> things = new Array<Thing>();
                for (Thing thing : world.boxWorld.things) {
                        if (!thing.getBody().isActive()) continue;
                        if (thing.getBody().getType() == BodyDef.BodyType.StaticBody) continue;

                        things.add(thing);

                }

                // set start zoom
                camera.zoom = 2f;
                camera.update();

                if (things.size > 0) { // look at the cards
                        WorldUtil.lookAt(camera, timer, things, 1.1f);
                } else {
                        WorldUtil.lookAt(camera, timer, new Rectangle(-2.5f, -2.5f, 5, 5), 1f);
                }

        }

        /**
         * Creates a bunch of blocks and adds them to the {@link #world}.
         * They are then merged so they look like the ground.
         * A body with a {@link ChainShape} is used for physical ground
         * in order to avoid ghost vertex collisions.
         */
        private void addGround(TextureRegion textureRegion, int blockCount) {
                debug("Adding ground.");


                // prepare visual ground
                float halfBlockWidth = cardHouseDef.cardHeight * 0.5f;
                float halfBlockHeight = cardHouseDef.cardHeight * 0.5f;

                float groundWidth = halfBlockWidth * 2 * blockCount;

                Array<Vector2> vertices = new Array<Vector2>();
                vertices.add(new Vector2(-halfBlockWidth * 1.001f, -halfBlockHeight));
                vertices.add(new Vector2(halfBlockWidth * 1.001f, -halfBlockHeight));
                vertices.add(new Vector2(halfBlockWidth * 1.001f, halfBlockHeight));
                vertices.add(new Vector2(-halfBlockWidth * 1.001f, halfBlockHeight));

                Array<OutlinePolygon> outlinePolygons = new Array<OutlinePolygon>();
                Array<OutlinePolygon> shadowPolygons = new Array<OutlinePolygon>();

                Array<TexturePolygon> texturePolygons = new Array<TexturePolygon>();

                for (int i = 0; i < blockCount; i++) {
                        float x = (i + 0.5f) * halfBlockWidth * 2 - groundWidth * 0.5f;

                        Vector2 pos = new Vector2(x, cardHouseDef.groundY - halfBlockHeight);
                        PPPolygon block = addGround(textureRegion, vertices, pos);

                        outlinePolygons.add(block.getOutlinePolygons().first());
                        shadowPolygons.add(block.getOutlinePolygons().get(1));

                        texturePolygons.add(block.getTexturePolygon());
                        block.getTexturePolygon().setColor(cardHouseDef.groundColor);
                }

                // merge outlines and align textures
                OutlineMerger outlineMerger = new OutlineMerger();
                outlineMerger.mergeOutlines(outlinePolygons);
                outlineMerger.mergeOutlines(shadowPolygons);

                TextureAligner textureAligner = new TextureAligner();
                textureAligner.alignTextures(texturePolygons);


                // prepare physical ground

                // create box2d body
                BodyDef bodyDef = new BodyDef();
                bodyDef.type = BodyDef.BodyType.StaticBody;
                bodyDef.active = true;

                Body chainBody = world.boxWorld.world.createBody(bodyDef);

                // create box2d fixture
                ChainShape chainShape = new ChainShape();


                Array<Vector2> chainVertices = new Array<Vector2>(true, 1, Vector2.class);
                chainVertices.addAll(outlinePolygons.first().getMyParents().first().getVertices());

                chainShape.createLoop(chainVertices.toArray());

                FixtureDef fixtureDef = new FixtureDef();
                fixtureDef.shape = chainShape;
                fixtureDef.friction = cardHouseDef.friction;

                chainBody.createFixture(fixtureDef);
                chainShape.dispose();

        }

        /**
         * Create a ground block. It is added to the {@link #world}.
         */
        private PPPolygon addGround(TextureRegion textureRegion, Array<Vector2> vertices, Vector2 pos) {


                PPPolygon block = new PPPolygon();

                // add texture
                TexturePolygon texturePolygon = new TexturePolygon();
                texturePolygon.setTextureRegion(textureRegion);
                block.setTexturePolygon(texturePolygon);

                // add outline
                OutlinePolygon outlinePolygon = new OutlinePolygon();
                block.getOutlinePolygons().add(outlinePolygon);

                // add shadow
                OutlinePolygon shadowPolygon = new OutlinePolygon();
                shadowPolygon.setColor(new Color(0, 0, 0, 0.35f));
                shadowPolygon.setHalfWidth(0.2f);
                shadowPolygon.setDrawInside(false);
                block.getOutlinePolygons().add(shadowPolygon);


                block.setVertices(vertices);

                world.addThing(block);

                block.setPosition(pos);

                return block;
        }

        /**
         * Creates a static {@link ThingWithBody} that the {@link BodyMover} needs. It is placed
         * far away.
         */
        private void createJointAnchor() {
                if (debug) debug("Creating joint anchor.");

                PolygonDef polygonDef = new PolygonDef();
                polygonDef.type = BodyDef.BodyType.StaticBody;

                polygonDef.vertices = new Array<Vector2>(true, 4, Vector2.class);
                polygonDef.vertices.add(new Vector2(-1, -1));
                polygonDef.vertices.add(new Vector2(1, -1));
                polygonDef.vertices.add(new Vector2(1, 1));
                polygonDef.vertices.add(new Vector2(-1, 1));

                polygonDef.position.set(0, -1000);


                ThingWithBody jointAnchor = new Polygon(polygonDef);
                world.boxWorld.safelyAddThing(jointAnchor);
                world.boxWorld.setPreferredJointAnchor(jointAnchor);

        }

        /** Create a card. It is added to the {@link #world}. */
        public PPPolygon addCard(Vector2 pos, float angle) {
                return addCard(pos.x, pos.y, angle);
        }

        /** Create a card. It is added to the {@link #world}. */
        public PPPolygon addCard(Vector2 pos) {
                return addCard(pos.x, pos.y, 0);
        }

        /** Create a card. It is added to the {@link #world}. */
        public PPPolygon addCard(float x, float y) {
                return addCard(x, y, 0);
        }

        /** Create a card. It is added to the {@link #world}. */
        public PPPolygon addCard(float x, float y, float angle) {
                if (debug) debug("Adding card at x=" + x + ", y=" + y + ".");

                PPPolygon card = new PPPolygon();

                PolygonDef def = new PolygonDef();
                def.friction = cardHouseDef.friction;
                card.setPhysicsThing(new Polygon(def));

                float hw = cardHouseDef.cardWidth * 0.5f;
                float hh = cardHouseDef.cardHeight * 0.5f;

                // prepare box2d body
                Array<Vector2> vertices = new Array<Vector2>();
                vertices.add(new Vector2(-hw, -hh));
                vertices.add(new Vector2(hw, -hh));
                vertices.add(new Vector2(hw, hh));
                vertices.add(new Vector2(-hw, hh));

                ((Polygon) card.getPhysicsThing()).setVertices(vertices);


                // prepare outline
                float visualHalfWidth = hw * 2;

                OutlinePolygon outlinePolygon = new OutlinePolygon();
                card.getOutlinePolygons().add(outlinePolygon);
                vertices = new Array<Vector2>();
                vertices.add(new Vector2(0, -hh));
                vertices.add(new Vector2(0, hh));
                outlinePolygon.setVertices(vertices);
                outlinePolygon.setClosedPolygon(false);
                outlinePolygon.setColor(cardHouseDef.cardColor);
                outlinePolygon.setHalfWidth(visualHalfWidth * 1f);

                //
                card.setPosition(x, y);
                card.setAngle(angle);


                world.addThing(card);

                return card;
        }


        @Override
        public void resize(int width, int height) {
                if (debug) debug("Resizing, width=" + width + ", height = " + height + ".");

                if (independentApplication) {
                        if (tips != null) tips.resize(width, height);
                }

                positionCamera();
        }

        /** The tool that moves the cards around. */
        public CardMover getCardMover() {
                return cardMover;
        }

}