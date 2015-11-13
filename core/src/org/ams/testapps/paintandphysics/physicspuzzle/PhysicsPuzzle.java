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
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
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


public class PhysicsPuzzle extends ApplicationAdapter {


        // stuff that define any particular game
        private float blockDim, physicsBlockDim, visualBlockDim;
        private int columns, rows;
        private float timeBetweenBlocks, accumulator;
        private TextureRegion textureRegion;


        // PaintAndPhysics stuff
        private PPWorld world;

        private Array<PPPolygon> blocksLeft = new Array<PPPolygon>();
        private Array<PPPolygon> blocks;
        private Array<PPPolygon> activeBlocks = new Array<PPPolygon>();
        private Array<PPPolygon> walls;

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
        private boolean drawOrigin = false;
        private boolean debugStart = false;


        @Override
        public void dispose() {
                if (polygonBatch != null) polygonBatch.dispose();
                if (textureRegion != null) textureRegion.getTexture().dispose();
                if (shapeRenderer != null) shapeRenderer.dispose();
        }

        @Override
        public void create() {
                Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

                PhysicsPuzzleDef def = new PhysicsPuzzleDef();
                def.textureRegionName = "pp";

                TextureAtlas textureAtlas = new TextureAtlas("packedimages/pack.atlas");

                create(null, textureAtlas, def);
        }

        public void create(InputMultiplexer inputMultiplexer, TextureAtlas textureAtlas, PhysicsPuzzleDef def) {
                if (textureAtlas == null) throw new IllegalArgumentException("TextureAtlas is null.");
                if (def == null) throw new IllegalArgumentException("PhysicsPuzzleDef is null.");


                float w = Gdx.graphics.getWidth();
                float h = Gdx.graphics.getHeight();
                camera = new OrthographicCamera(10, 10 * (h / w));
                CameraNavigator cameraNavigator = new CameraNavigator(camera);

                world = new PPWorld();
                bodyMover = new BodyMover(world.boxWorld, camera);

                polygonBatch = new PrettyPolygonBatch();
                shapeRenderer = new ShapeRenderer();
                shapeRenderer.setAutoShapeType(true);

                if (inputMultiplexer == null) {
                        inputMultiplexer = new InputMultiplexer();
                        Gdx.input.setInputProcessor(inputMultiplexer);
                }
                inputMultiplexer.addProcessor(bodyMover);
                //inputMultiplexer.addProcessor(cameraNavigator);


                initFromDefinition(textureAtlas, def);
        }

        private void initFromDefinition(TextureAtlas textureAtlas, PhysicsPuzzleDef def) {

                // set values that define this particular game
                columns = def.columns;
                rows = def.rows;

                blockDim = def.blockDim;
                physicsBlockDim = blockDim * 0.96f;
                visualBlockDim = blockDim;


                timeBetweenBlocks = def.timeBetweenBlocks;
                accumulator = def.timeBetweenBlocks;

                this.textureRegion = textureAtlas.findRegion(def.textureRegionName);

                // create the puzzle stuff
                walls = setUpWalls();

                blocks = initBlocks();
                blocksLeft.addAll(blocks);

                setUpTextures();

                platformLevels = initPlatformLevels();
                wallVerticesForGroundBody = computeWallVerticesForGroundBody();

                updateChainBody();
                createJointAnchor();
        }

        private Array<PPPolygon> initBlocks() {
                Array<PPPolygon> blocks = prepareBlocks();

                for (PPPolygon block : blocks) {
                        if (!debugStart) block.setVisible(false);
                        block.getPhysicsThing().getBody().setActive(false);
                }
                return blocks;
        }


        private Array<Integer> initPlatformLevels() {
                Array<Integer> platformLevels = new Array<Integer>();
                for (int i = 0; i < columns; i++) {
                        platformLevels.add(-1);
                }
                return platformLevels;
        }

        private void setUpTextures() {
                Array<TexturePolygon> toAlign = new Array<TexturePolygon>();
                for (PPThing thing : world.things) {
                        TexturePolygon texturePolygon = thing.getTexturePolygon();

                        if (texturePolygon != null) {
                                texturePolygon.setTextureScale(camera.viewportHeight / this.textureRegion.getRegionHeight());
                                toAlign.add(texturePolygon);
                        }
                }
                TextureAligner textureAligner = new TextureAligner();
                textureAligner.alignTextures(toAlign, true);

                for (PPPolygon block : blocks) {
                        if (!debugStart) block.setPosition(0, rows * blockDim * 2);
                }
        }

        private void createJointAnchor() {
                PolygonDef polygonDef = new PolygonDef();
                polygonDef.type = BodyDef.BodyType.StaticBody;


                polygonDef.vertices = new Array<Vector2>(true, 4, Vector2.class);
                polygonDef.vertices.add(new Vector2(-1, -1));
                polygonDef.vertices.add(new Vector2(1, -1));
                polygonDef.vertices.add(new Vector2(1, 1));
                polygonDef.vertices.add(new Vector2(-1, 1));

                polygonDef.position.set(0, -blockDim * rows * 3);


                ThingWithBody jointAnchor = new Polygon(polygonDef);
                world.boxWorld.safelyAddThing(jointAnchor);
                world.boxWorld.setPreferredJointAnchor(jointAnchor);

        }

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

        @Override
        public void render() {
                Gdx.gl20.glClearColor(1, 1, 1, 1);
                Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
                float delta = Gdx.graphics.getDeltaTime();


                accumulator += Math.min(delta, timeBetweenBlocks * 0.25f);

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
                int lockedInThisTime = lockInActiveBlocksThatAreInPosition();

                if (lockedInThisTime > 0 && blocksLeft.size > 0) {
                        updateChainBody();
                }

                bodyMover.step(delta);
                world.step(delta);


                polygonBatch.begin(camera);
                world.draw(polygonBatch);
                polygonBatch.end();

                if (drawChainBody) drawChainBody();
                if (drawOrigin) drawOrigin();

        }

        public void setDebugStart(boolean debugStart) {
                this.debugStart = debugStart;
        }

        public boolean isDebugStart() {
                return debugStart;
        }

        public void setDrawChainBody(boolean drawChainBody) {
                this.drawChainBody = drawChainBody;
        }

        public boolean isDrawingChainBody() {
                return drawChainBody;
        }

        public void setDrawOrigin(boolean drawOrigin) {
                this.drawOrigin = drawOrigin;
        }

        public boolean isDrawingOrigin() {
                return drawChainBody;
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


        private void updateChainBody() {
                if (chainBody != null) {
                        world.boxWorld.world.destroyBody(chainBody);
                        chainBody = null;
                }

                chainVertices = computeChainVertices();


                BodyDef bodyDef = new BodyDef();
                bodyDef.type = BodyDef.BodyType.StaticBody;
                bodyDef.active = true;
                chainBody = world.boxWorld.world.createBody(bodyDef);

                ChainShape chainShape = new ChainShape();

                chainShape.createLoop(chainVertices);

                chainBody.createFixture(chainShape, 0);

                chainShape.dispose();

        }

        private Array<Vector2> computeWallVerticesForGroundBody() {
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

        private Vector2[] computeChainVertices() {

                Array<Vector2> platformVertices = new Array<Vector2>();

                platformVertices.addAll(wallVerticesForGroundBody);


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
                                PPPolygon platformBegin = getBlock(0, platformColumnBegin);
                                beginPos = new Vector2((Vector2) platformBegin.getUserData());
                                beginPos.add(-physicsBlockDim * 0.5f, -physicsBlockDim * 0.5f);


                                PPPolygon platformEnd = getBlock(0, platformColumnEnd);
                                endPos = new Vector2((Vector2) platformEnd.getUserData());
                                endPos.add(physicsBlockDim * 0.5f, -physicsBlockDim * 0.5f);
                        }


                        if (platformRow < previousPlatformRow) {
                                beginPos.sub((blockDim - physicsBlockDim), 0);
                        } else {
                                if (platformVertices.size > 0 && platformColumnBegin != 0) {
                                        platformVertices.peek().add(blockDim - physicsBlockDim, 0);
                                }
                        }

                        if (platformColumnEnd == columns - 1) {
                                endPos.add(blockDim - physicsBlockDim, 0);
                        }
                        if (platformColumnBegin == 0) {
                                beginPos.sub(blockDim - physicsBlockDim, 0);
                        }

                        if (platformRow == -1) {
                                beginPos.sub(0, blockDim - physicsBlockDim);
                                endPos.sub(0, blockDim - physicsBlockDim);

                        }

                        // make sure i dont add vertices that are too close to the previous
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

        private PPPolygon getBlock(int row, int column) {
                int index = row * columns;
                index += column;
                return blocks.get(index);
        }

        private boolean allowedToLock(PPPolygon block) {
                int row = getRow(block);
                int column = getColumn(block);

                Vector2 currentPos = block.getPhysicsThing().getBody().getPosition();
                Vector2 finalPosition = (Vector2) block.getUserData();

                if (currentPos.dst(finalPosition) >= 0.02f) return false;

                return platformLevels.get(column) == row - 1;

        }

        private int getRow(PPPolygon block) {
                int index = blocks.indexOf(block, true);

                return index / columns;
        }

        private int getColumn(PPPolygon block) {
                int index = blocks.indexOf(block, true);

                return index % columns;
        }

        private int lockInActiveBlocksThatAreInPosition() {
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

                        OutlinePolygon correctOutlineParent =
                                walls.first().getOutlinePolygons().first().getMyParents().first();

                        outlinesToMerge.add(block.getOutlinePolygons().first());
                        if (!debugStart) outlineMerger.mergeOutlines(outlinesToMerge);

                }
                return lockedIn;
        }

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
                outlinePolygon.setHalfWidth(0.01f);
                wall.getOutlinePolygons().add(outlinePolygon);

                // set properties of all 3 members


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

                wall.setPosition(0, 0);
                return wall;
        }

        /** Bottom blocks are added first. */
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

                world.insertThing(0, block);
                return block;
        }

        @Override
        public void resize(int width, int height) {

                camera.setToOrtho(false, 10, 10 * ((float) height / (float) width));
                camera.position.x = 0;
                camera.position.y = 0;

                camera.update();
        }
}
