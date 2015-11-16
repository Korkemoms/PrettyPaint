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
package org.ams.testapps.paintandphysics.physicspuzzle;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
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
import com.badlogic.gdx.utils.Array;
import org.ams.core.CameraNavigator;
import org.ams.paintandphysics.things.PPPolygon;
import org.ams.paintandphysics.things.PPThing;
import org.ams.paintandphysics.world.PPWorld;
import org.ams.physics.things.Polygon;
import org.ams.physics.things.ThingWithBody;
import org.ams.physics.things.def.PolygonDef;
import org.ams.physics.tools.BodyMover;
import org.ams.prettypaint.*;

/**
 * A puzzle game with a touch of physics. Can run as independent application,
 * or can be run from another {@link ApplicationAdapter} using
 * {@link #create(InputMultiplexer, TextureRegion, PhysicsPuzzleDef)}.
 */
public class PhysicsPuzzle extends ApplicationAdapter {


        // stuff that define any particular game
        private float blockDim = 0.5f, physicsBlockDim, visualBlockDim;
        private int columns, rows;
        private float timeBetweenBlocks, accumulator;
        private TextureRegion textureRegion;
        private final Color outlineColor = new Color(Color.BLACK);


        // when created as independent app this one is not null and must be disposed
        private TextureRegion textureRegionThatIOwn;


        // PaintAndPhysics stuff
        private PPWorld world;

        private Array<PPPolygon> blocksLeft = new Array<PPPolygon>();
        private Array<PPPolygon> blocks;
        private Array<PPPolygon> activeBlocks = new Array<PPPolygon>();

        private PPPolygon rightWall;
        private PPPolygon leftWall;
        private PPPolygon floor;


        // PrettyPaint related
        private OrthographicCamera camera;
        private PrettyPolygonBatch polygonBatch;
        private OutlineMerger outlineMerger = new OutlineMerger();
        private Array<OutlinePolygon> outlinesToMerge = new Array<OutlinePolygon>();


        // physics/box2d
        private BodyMover bodyMover;
        private Body chainBody;


        // these two are for constructing the chainBody
        private Array<Vector2> wallVerticesForGroundBody;
        private Array<Integer> platformLevels;


        // debug stuff
        private Vector2[] chainVertices;
        private ShapeRenderer shapeRenderer;
        private boolean drawChainBody = false;
        private boolean drawQueuedBlocks = false;
        private boolean drawOrigin = false;
        private boolean debugStart = false;

        private boolean paused = false;


        /** Whether this game is run independently or from another ApplicationAdapter. */
        private boolean independent = false;


        @Override
        public void dispose() {
                if (world != null) world.dispose();
                if (bodyMover != null) bodyMover.dispose();
                if (polygonBatch != null) polygonBatch.dispose();
                if (shapeRenderer != null) shapeRenderer.dispose();
                if (textureRegionThatIOwn != null) textureRegionThatIOwn.getTexture().dispose();

                world = null;
                bodyMover = null;
                polygonBatch = null;
                shapeRenderer = null;
                textureRegionThatIOwn = null;
        }

        /**
         * Used when run as an independent application.
         * Don't use this method if you want to run the puzzle from another ApplicationAdapter.
         */
        @Override
        public void create() {
                independent = true;
                Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

                PhysicsPuzzleDef def = new PhysicsPuzzleDef();
                def.textureRegionName = "pp";

                textureRegionThatIOwn = new TextureRegion(new Texture("images/puzzles/pp.JPG"));

                create(null, textureRegionThatIOwn, def);
        }

        /**
         * Use this to run the puzzle from another {@link ApplicationAdapter}.
         * <p/>
         * Remember to dispose of the atlas when you no longer use it.
         *
         * @param inputMultiplexer InputProcessors will be added to the end of the multiplexer
         * @param textureRegion    The region to draw on the puzzle.
         * @param def              defines this particular game
         */
        public void create(InputMultiplexer inputMultiplexer, TextureRegion textureRegion, PhysicsPuzzleDef def) {
                if (textureRegion == null) throw new IllegalArgumentException("TextureRegion is null.");
                if (def == null) throw new IllegalArgumentException("PhysicsPuzzleDef is null.");

                // camera stuff
                float w = Gdx.graphics.getWidth();
                float h = Gdx.graphics.getHeight();
                camera = new OrthographicCamera(10, 10 * (h / w));
                CameraNavigator cameraNavigator = new CameraNavigator(camera);

                // physics
                world = new PPWorld();
                bodyMover = new BodyMover(world.boxWorld, camera);

                // batches for drawing
                polygonBatch = new PrettyPolygonBatch();
                shapeRenderer = new ShapeRenderer();
                shapeRenderer.setAutoShapeType(true);

                // input multiplexing
                if (inputMultiplexer == null) {
                        inputMultiplexer = new InputMultiplexer();
                        Gdx.input.setInputProcessor(inputMultiplexer);
                }
                inputMultiplexer.addProcessor(bodyMover);
                //inputMultiplexer.addProcessor(cameraNavigator);


                initFromDefinition(textureRegion, def);

                lookAtPuzzle();
        }

        /** Set up the game world according to the textureRegion and definition */
        private void initFromDefinition(TextureRegion textureRegion, PhysicsPuzzleDef def) {

                // set values that define this particular game
                columns = def.columns;
                rows = def.rows;


                physicsBlockDim = blockDim * 0.96f;
                visualBlockDim = blockDim;

                outlineColor.set(def.outlineColor);

                timeBetweenBlocks = def.timeBetweenBlocks;
                accumulator = def.timeBetweenBlocks;

                this.textureRegion = textureRegion;

                // create the puzzle stuff

                // create the puzzle blocks and walls
                setUpWalls();
                blocks = initBlocks();
                blocksLeft.addAll(blocks);


                // prepare the texture and then move the blocks away
                scaleAndAlignTextures();
                for (PPPolygon block : blocks) {
                        // move them away so they aren't in the way for the BodyMover
                        if (!debugStart) block.setPosition(0, rows * blockDim * 0.5f + blockDim * 3);
                }

                // this block has to do with the ChainBody that is the physics of the "ground"
                platformLevels = initPlatformLevels();
                wallVerticesForGroundBody = computeChainVerticesForFloorAndWalls();
                updateChainBody();


                createJointAnchor();
        }

        /** Create and position all the blocks(pieces). */
        private Array<PPPolygon> initBlocks() {
                Array<PPPolygon> blocks = prepareBlocks();

                for (PPPolygon block : blocks) {
                        if (!debugStart && !drawQueuedBlocks) block.setVisible(false);
                        block.getPhysicsThing().getBody().setActive(false);
                }
                return blocks;
        }

        /** Init platform levers to -1. */
        private Array<Integer> initPlatformLevels() {
                Array<Integer> platformLevels = new Array<Integer>();
                for (int i = 0; i < columns; i++) {
                        platformLevels.add(-1);
                }
                return platformLevels;
        }

        /**
         * Scale so texture fills the entire puzzle. Aligns the TexturePolygons
         * textures such that they appear to be one when locked in. Also centers
         * the texture in the middle of the puzzle.
         */
        private void scaleAndAlignTextures() {

                // find scale so that the texture covers
                // the entire puzzle with no repetition
                Rectangle boundingBox = new Rectangle();
                boundingBox.set(leftWall.getTexturePolygon().getBoundingRectangle());
                boundingBox.merge(rightWall.getTexturePolygon().getBoundingRectangle());
                boundingBox.merge(floor.getTexturePolygon().getBoundingRectangle());

                float horizontalScale = boundingBox.width / textureRegion.getRegionWidth();
                float verticalScale = boundingBox.height / textureRegion.getRegionHeight();
                float textureScale = Math.max(horizontalScale, verticalScale);


                Array<TexturePolygon> toAlign = new Array<TexturePolygon>();
                for (PPThing thing : world.things) {
                        TexturePolygon texturePolygon = thing.getTexturePolygon();

                        if (texturePolygon != null) {
                                // set the scale we found
                                texturePolygon.setTextureScale(textureScale);
                                toAlign.add(texturePolygon);
                        }
                }

                // align all the TexturePolygons
                // the default behaviour centers the texture in the middle of the puzzle
                TextureAligner textureAligner = new TextureAligner();
                textureAligner.alignTextures(toAlign, true);
        }


        /**
         * Creates a static {@link ThingWithBody} that the {@link BodyMover} needs. It is placed
         * far away.
         */
        private void createJointAnchor() {

                PolygonDef polygonDef = new PolygonDef();
                polygonDef.type = BodyDef.BodyType.StaticBody;

                polygonDef.vertices = new Array<Vector2>(true, 4, Vector2.class);
                polygonDef.vertices.add(new Vector2(-1, -1));
                polygonDef.vertices.add(new Vector2(1, -1));
                polygonDef.vertices.add(new Vector2(1, 1));
                polygonDef.vertices.add(new Vector2(-1, 1));

                polygonDef.position.set(0, -blockDim * rows * 10);


                ThingWithBody jointAnchor = new Polygon(polygonDef);
                world.boxWorld.safelyAddThing(jointAnchor);
                world.boxWorld.setPreferredJointAnchor(jointAnchor);

        }

        /** Creates 3 walls: left, floor and right. */
        private Array<PPPolygon> setUpWalls() {
                Array<PPPolygon> walls = new Array<PPPolygon>();

                float floorWidth = (columns + 2) * blockDim;
                float wallHeight = (rows + 2) * blockDim;


                floor = addWall(textureRegion, floorWidth, blockDim * 2);
                floor.setPosition(0, -wallHeight * 0.5f + blockDim);
                walls.add(floor);


                rightWall = addWall(textureRegion, wallHeight, blockDim);
                rightWall.setAngle(MathUtils.PI * 0.5f);
                rightWall.setPosition(floorWidth * 0.5f - blockDim * 0.5f, 0);
                walls.add(rightWall);


                leftWall = addWall(textureRegion, wallHeight, blockDim);
                leftWall.setAngle(MathUtils.PI * 0.5f);
                leftWall.setPosition(-floorWidth * 0.5f + blockDim * 0.5f, 0);
                walls.add(leftWall);


                // merge wall outlines
                outlinesToMerge.add(floor.getOutlinePolygons().first());
                outlinesToMerge.add(rightWall.getOutlinePolygons().first());
                outlinesToMerge.add(leftWall.getOutlinePolygons().first());
                if (!debugStart) outlineMerger.mergeOutlines(outlinesToMerge);


                return walls;

        }

        public void setPaused(boolean paused) {
                this.paused = paused;
        }

        public boolean isPaused() {
                return paused;
        }

        @Override
        public void render() {
                if (independent) { // if not independent then the owner should clear
                        Gdx.gl20.glClearColor(1, 1, 1, 1);
                        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
                }


                if (!paused) {
                        float delta = Gdx.graphics.getDeltaTime();

                        // limit accumulation so blocks don't spawn on top of each other when delta time is extreme
                        // (right after loading)
                        accumulator += Math.min(delta, timeBetweenBlocks * 0.25f);


                        // adds new blocks every timeBetweenBlocks seconds
                        if (!debugStart && accumulator > timeBetweenBlocks) {
                                accumulator -= timeBetweenBlocks;

                                if (blocksLeft.size > 0) {
                                        PPPolygon block = blocksLeft.first();
                                        blocksLeft.removeIndex(0);

                                        block.setPosition(0, rows * blockDim * 0.5f + blockDim * 3);
                                        block.setVisible(true);
                                        block.getPhysicsThing().getBody().setActive(true);
                                        activeBlocks.add(block);

                                }
                        }

                        // try to lock in blocks and update the "ground"
                        int lockedInThisTime = lockInActiveBlocksThatAreInPositionAndUpdateOutlines();
                        if (lockedInThisTime > 0 && blocksLeft.size > 0)
                                updateChainBody();


                        // body mover and world does work here

                        bodyMover.step(delta);
                        world.step(delta);
                }

                // draw stuff
                polygonBatch.begin(camera);
                world.draw(polygonBatch);
                polygonBatch.end();

                // draw debug stuff
                if (drawChainBody) drawChainBody();
                if (drawOrigin) drawOrigin();

        }

        /** For debugging. */
        public void setDebugStart(boolean debugStart) {
                this.debugStart = debugStart;
        }

        /** For debugging. */
        public boolean isDebugStart() {
                return debugStart;
        }

        /** For debugging. */
        public void setDrawChainBody(boolean drawChainBody) {
                this.drawChainBody = drawChainBody;
        }

        /** For debugging. */
        public boolean isDrawingChainBody() {
                return drawChainBody;
        }

        /** For debugging. */
        public void setDrawOrigin(boolean drawOrigin) {
                this.drawOrigin = drawOrigin;
        }

        /** For debugging. */
        public boolean isDrawingOrigin() {
                return drawChainBody;
        }

        /** For debugging. */
        public void setDrawQueuedBlocks(boolean drawQueuedBlocks) {
                this.drawQueuedBlocks = drawQueuedBlocks;
        }

        /** For debugging. */
        public boolean isDrawingQueuedBlocks() {
                return drawQueuedBlocks;
        }

        /** For debugging. */
        private void drawChainBody() {
                if (chainVertices == null) return;


                shapeRenderer.begin();

                shapeRenderer.setProjectionMatrix(camera.combined);
                shapeRenderer.set(ShapeRenderer.ShapeType.Line);
                shapeRenderer.setColor(Color.RED);


                for (int i = 0; i < chainVertices.length; i++) {
                        Vector2 a = chainVertices[i];
                        Vector2 b = chainVertices[(i + 1) % chainVertices.length];

                        shapeRenderer.line(a, b);

                }

                shapeRenderer.end();
        }

        /** For debugging. */
        private void drawOrigin() {

                shapeRenderer.begin();

                shapeRenderer.setProjectionMatrix(camera.combined);
                shapeRenderer.set(ShapeRenderer.ShapeType.Line);
                shapeRenderer.setColor(Color.ORANGE);

                shapeRenderer.line(-10, 0, 10, 0);
                shapeRenderer.line(0, -10, 0, 10);


                shapeRenderer.end();
        }

        /**
         * Update the chain body that is the "ground" that active blocks crash with.
         * This must be called after a new block is locked in.
         */
        private void updateChainBody() {
                // destroy the old one, if any
                if (chainBody != null) {
                        world.boxWorld.world.destroyBody(chainBody);
                        chainBody = null;
                }

                // create box2d body
                BodyDef bodyDef = new BodyDef();
                bodyDef.type = BodyDef.BodyType.StaticBody;
                bodyDef.active = true;
                chainBody = world.boxWorld.world.createBody(bodyDef);

                // create box2d fixture
                ChainShape chainShape = new ChainShape();
                chainVertices = computeChainVertices();
                chainShape.createLoop(chainVertices);
                chainBody.createFixture(chainShape, 0);
                chainShape.dispose();

        }

        /** Finds vertices that form an (almost complete) outline of the walls and floor. */
        private Array<Vector2> computeChainVerticesForFloorAndWalls() {
                Array<Vector2> vertices = new Array<Vector2>();

                {
                        Rectangle boundingRectangle = ((Polygon) rightWall.getPhysicsThing()).getPhysicsBoundingBox();
                        float halfWidth = boundingRectangle.width * 0.5f;
                        float halfHeight = boundingRectangle.height * 0.5f;
                        Vector2 center = boundingRectangle.getCenter(new Vector2());

                        vertices.add(new Vector2(center).add(-halfWidth, halfHeight));
                        vertices.add(new Vector2(center).add(halfWidth, halfHeight));
                }
                {
                        Rectangle boundingRectangle = ((Polygon) floor.getPhysicsThing()).getPhysicsBoundingBox();
                        float halfWidth = boundingRectangle.width * 0.5f;
                        float halfHeight = boundingRectangle.height * 0.5f;
                        Vector2 center = boundingRectangle.getCenter(new Vector2());

                        vertices.add(new Vector2(center).add(halfWidth, -halfHeight));
                        vertices.add(new Vector2(center).add(-halfWidth, -halfHeight));
                }

                {
                        Rectangle boundingRectangle = ((Polygon) leftWall.getPhysicsThing()).getPhysicsBoundingBox();
                        float halfWidth = boundingRectangle.width * 0.5f;
                        float halfHeight = boundingRectangle.height * 0.5f;
                        Vector2 center = boundingRectangle.getCenter(new Vector2());

                        vertices.add(new Vector2(center).add(-halfWidth, halfHeight));
                        vertices.add(new Vector2(center).add(halfWidth, halfHeight));
                }

                return vertices;

        }

        /**
         * Finds the vertices for the chain body that is the "ground" for the active blocks.
         * It forms an outline around the walls, floor and locked blocks.
         */
        private Vector2[] computeChainVertices() {

                Array<Vector2> platformVertices = new Array<Vector2>();

                platformVertices.addAll(wallVerticesForGroundBody);

                // first i find all the platforms that the locked blocks form
                Array<Array<Integer>> platforms = new Array<Array<Integer>>();
                {
                        Array<Integer> platform = new Array<Integer>();
                        platforms.add(platform);

                        int row = platformLevels.get(0);
                        int previousRow = row;
                        platform.add(row);

                        for (int i = 1; i < platformLevels.size; i++) {
                                row = platformLevels.get(i);
                                if (row != previousRow) {
                                        platform = new Array<Integer>();
                                        platforms.add(platform);
                                }
                                platform.add(row);

                                previousRow = row;
                        }
                }

                // for each platform 2 vertices is added
                // special treatment for platforms with index -1(a platform on the floor)
                // and for platforms touching the left or right wall
                int i = 0;
                int previousPlatformRow = -1;
                for (Array<Integer> platform : platforms) {
                        int platformColumnBegin = i; // inclusive
                        int platformColumnEnd = i + platform.size - 1; // not inclusive
                        int platformRow = platform.first();

                        Vector2 beginPos, endPos;


                        if (platformRow >= 0) {
                                PPPolygon platformBegin = getBlock(platformRow, platformColumnBegin);
                                beginPos = new Vector2(platformBegin.getPhysicsThing().getBody().getPosition());
                                beginPos.add(-physicsBlockDim * 0.5f, physicsBlockDim * 0.5f);

                                PPPolygon platformEnd = getBlock(platformRow, platformColumnEnd);
                                endPos = new Vector2(platformEnd.getPhysicsThing().getBody().getPosition());
                                endPos.add(physicsBlockDim * 0.5f, physicsBlockDim * 0.5f);
                        } else {
                                // platform is on the floor
                                // we get coordinates for an equally wide platform just
                                // one row higher up, then lower height by one block height

                                PPPolygon platformBegin = getBlock(0, platformColumnBegin);
                                beginPos = new Vector2((Vector2) platformBegin.getUserData());
                                beginPos.add(-physicsBlockDim * 0.5f, physicsBlockDim * 0.5f - blockDim);

                                PPPolygon platformEnd = getBlock(0, platformColumnEnd);
                                endPos = new Vector2((Vector2) platformEnd.getUserData());
                                endPos.add(physicsBlockDim * 0.5f, physicsBlockDim * 0.5f - blockDim);

                        }


                        if (platformRow < previousPlatformRow) {
                                beginPos.sub((blockDim - physicsBlockDim), 0);
                        } else {
                                if (platformVertices.size > 0 && platformColumnBegin != 0) {
                                        platformVertices.peek().add(blockDim - physicsBlockDim, 0);
                                }
                        }

                        if (platformColumnEnd == columns - 1) {
                                // platform touching right wall, adjust slightly
                                endPos.add(blockDim - physicsBlockDim, 0);
                        }
                        if (platformColumnBegin == 0) {
                                // platform touching left wall, adjust slightly
                                beginPos.sub(blockDim - physicsBlockDim, 0);
                        }


                        // make sure i don't add vertices that are too close to the previous
                        // (this is only an issue for the top row)
                        if (platformVertices.size == 0 || platformVertices.peek().dst(beginPos) > 0.01f)
                                platformVertices.add(beginPos);

                        if (platformVertices.size == 0 || platformVertices.peek().dst(endPos) > 0.01f)
                                platformVertices.add(endPos);


                        i += platform.size;
                        previousPlatformRow = platformRow;

                }


                return platformVertices.toArray(Vector2.class);
        }


        /**
         * Checks whether a block should be locked into its final position.
         * Returns true when it is close enough to its final position and there
         * is a locked block under its final position.
         */
        private boolean allowedToLock(PPPolygon block) {
                int row = getRow(block);
                int column = getColumn(block);

                Vector2 currentPos = block.getPhysicsThing().getBody().getPosition();
                Vector2 finalPosition = (Vector2) block.getUserData();

                if (currentPos.dst(finalPosition) >= 0.02f) return false;

                return platformLevels.get(column) == row - 1;

        }

        /** Finds the block that belongs to given row and column. */
        private PPPolygon getBlock(int row, int column) {
                int index = row * columns;
                index += column;
                return blocks.get(index);
        }

        /** Finds target row of block. */
        private int getRow(PPPolygon block) {
                int index = blocks.indexOf(block, true);
                return index / columns;
        }

        /** Finds target column of block. */
        private int getColumn(PPPolygon block) {
                int index = blocks.indexOf(block, true);
                return index % columns;
        }

        /**
         * Does what it says. For each active block it checks if it is in position and if
         * there is a block locked in underneath. If so it locks the block in and updates outlines.
         */
        private int lockInActiveBlocksThatAreInPositionAndUpdateOutlines() {
                int lockedIn = 0;
                for (int i = activeBlocks.size - 1; i >= 0; i--) {
                        PPPolygon block = activeBlocks.get(i);

                        if (!allowedToLock(block)) continue;
                        lockedIn++;

                        int column = getColumn(block);
                        platformLevels.set(column, platformLevels.get(column) + 1);

                        Vector2 finalPosition = (Vector2) block.getUserData();
                        block.setPosition(finalPosition);

                        block.getPhysicsThing().getBody().setActive(false);
                        activeBlocks.removeIndex(i);

                        outlinesToMerge.add(block.getOutlinePolygons().first());
                        if (!debugStart) outlineMerger.mergeOutlines(outlinesToMerge);

                }
                return lockedIn;
        }

        /** Add a wall or floor. */
        private PPPolygon addWall(TextureRegion textureRegion, float wallWidth, float wallHeight) {
                PPPolygon wall = new PPPolygon();

                // add box2d polygon
                PolygonDef def = new PolygonDef();
                def.type = BodyDef.BodyType.StaticBody;
                def.active = false;
                wall.setPhysicsThing(new Polygon(def));

                // add textureRegionName
                TexturePolygon texturePolygon = new TexturePolygon();
                texturePolygon.setTextureRegion(textureRegion);
                wall.setTexturePolygon(texturePolygon);

                // add outline
                OutlinePolygon outlinePolygon = new OutlinePolygon();
                outlinePolygon.setColor(outlineColor);
                outlinePolygon.setHalfWidth(0.01f);
                wall.getOutlinePolygons().add(outlinePolygon);

                // set properties of all 3 members

                // set the visual vertices
                float halfWidth = wallWidth * 0.5f;
                halfWidth += (visualBlockDim - blockDim) * 0.5f;
                float halfHeight = wallHeight * 0.5f;
                halfHeight += (visualBlockDim - blockDim) * 0.5f;

                Array<Vector2> vertices = new Array<Vector2>();
                vertices.add(new Vector2(-halfWidth, halfHeight));
                vertices.add(new Vector2(-halfWidth, -halfHeight));
                vertices.add(new Vector2(halfWidth, -halfHeight));
                vertices.add(new Vector2(halfWidth, halfHeight));

                wall.getTexturePolygon().setVertices(vertices);
                wall.getOutlinePolygons().first().setVertices(vertices);


                // set the physics vertices
                halfWidth = wallWidth * 0.5f;
                halfWidth -= (blockDim - physicsBlockDim) * 0.5f;
                halfHeight = wallHeight * 0.5f;
                halfHeight -= (blockDim - physicsBlockDim) * 0.5f;

                vertices = new Array<Vector2>();
                vertices.add(new Vector2(-halfWidth, halfHeight));
                vertices.add(new Vector2(-halfWidth, -halfHeight));
                vertices.add(new Vector2(halfWidth, -halfHeight));
                vertices.add(new Vector2(halfWidth, halfHeight));

                ((Polygon) wall.getPhysicsThing()).setVertices(vertices);

                world.addThing(wall);

                return wall;
        }

        /** Prepare all the puzzle pieces. Bottom blocks are added first. */
        private Array<PPPolygon> prepareBlocks() {
                float rowWidth = blockDim * columns;
                float halfRowWidth = rowWidth * 0.5f;
                float halfBlockDim = blockDim * 0.5f;


                Array<PPPolygon> blocks = new Array<PPPolygon>();

                for (int row = 0; row < rows; row++) {
                        float y = -rows * blockDim * 0.5f + blockDim * 1.5f;
                        y += blockDim * row;

                        for (int col = 0; col < columns; col++) {
                                float x = -halfRowWidth + halfBlockDim;
                                x += blockDim * col;
                                PPPolygon block = prepareBlock(textureRegion, x, y);
                                blocks.add(block);

                        }
                }

                return blocks;
        }

        /** Prepare one of the puzzle pieces */
        private PPPolygon prepareBlock(TextureRegion textureRegion, float x, float y) {
                // make a moving block with textureRegionName and outlines
                PPPolygon block = new PPPolygon();

                // add box2d polygon
                PolygonDef def = new PolygonDef();
                def.friction = 0.05f;
                def.fixedRotation = true;

                block.setPhysicsThing(new Polygon(def));

                // add textureRegionName
                TexturePolygon texturePolygon = new TexturePolygon();
                texturePolygon.setTextureRegion(new TextureRegion(textureRegion));
                block.setTexturePolygon(texturePolygon);

                // add outline
                OutlinePolygon outlinePolygon = new OutlinePolygon();
                outlinePolygon.setColor(outlineColor);
                outlinePolygon.setHalfWidth(0.01f);
                block.getOutlinePolygons().add(outlinePolygon);

                // set properties of all 3 PPPolygon members


                // the visuals look cooler if they are a little bit larger
                float half = visualBlockDim * 0.5f;
                Array<Vector2> vertices = new Array<Vector2>();
                vertices.add(new Vector2(-half, -half));
                vertices.add(new Vector2(half, -half));
                vertices.add(new Vector2(half, half));
                vertices.add(new Vector2(-half, half));


                block.getTexturePolygon().setVertices(vertices);
                for (OutlinePolygon polygon : block.getOutlinePolygons()) {
                        polygon.setVertices(vertices);
                }


                // the box2d box must be a little bit smaller in order for all to fit
                half = physicsBlockDim * 0.5f;
                vertices = new Array<Vector2>();
                vertices.add(new Vector2(-half, -half));
                vertices.add(new Vector2(half, -half));
                vertices.add(new Vector2(half, half));
                vertices.add(new Vector2(-half, half));

                ((Polygon) block.getPhysicsThing()).setVertices(vertices);


                block.setPosition(x, y);
                block.setUserData(new Vector2(x, y)); // need to remember this position for later

                // insert so blocks are painted before walls (looks slightly better)
                world.insertThing(0, block);
                return block;
        }

        @Override
        public void resize(int width, int height) {
                camera.setToOrtho(false, 10, 10 * ((float) height / (float) width));

                lookAtPuzzle();
        }

        private void lookAtPuzzle() {
                // look at the entire puzzle with a little margin on the sides

                Rectangle boundingBox = new Rectangle();
                boundingBox.set(leftWall.getTexturePolygon().getBoundingRectangle());
                boundingBox.merge(rightWall.getTexturePolygon().getBoundingRectangle());
                boundingBox.merge(floor.getTexturePolygon().getBoundingRectangle());

                // look at the center of the puzzle
                Vector2 center = boundingBox.getCenter(new Vector2());
                camera.position.x = center.x;
                camera.position.y = center.y;

                // zoom so we see all of it with some margin on the sides
                float horizontalZoom = boundingBox.width / camera.viewportWidth;
                float verticalZoom = boundingBox.height / camera.viewportHeight;
                float targetZoom = Math.max(horizontalZoom, verticalZoom);
                targetZoom *= 1.05f;

                camera.zoom = targetZoom;

                camera.update();
        }
}
