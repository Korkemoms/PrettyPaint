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
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import org.ams.core.Util;
import org.ams.prettypaint.PrettyPolygonBatch;
import org.ams.prettypaint.TexturePolygon;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.Map;

/**
 * A game menu for {@link PhysicsPuzzle }.
 */
public class PhysicsPuzzleGameMenu extends ApplicationAdapter {

        private PhysicsPuzzle physicsPuzzle; // the game, a new one is created for each "game"

        private Preferences preferences; // used to remember settings

        private Skin skin;
        private Stage stage;

        // image selection menu things
        private Image selectedImage;

        private Runnable onResize; // used to lay out menus after resize

        private InputMultiplexer inputMultiplexer;

        private boolean hideAndPauseGame = true;

        // background stuff
        private OrthographicCamera backgroundCamera;
        private TexturePolygon menuBackground;
        private PrettyPolygonBatch polygonBatch;


        private TextureAtlas textureAtlas; // thumbnails and backgrounds in this atlas
        private TextureRegion currentPuzzleTextureRegion; // stored here so it can be dispose()'ed

        // maps thumbnail regions to names used to load a normal sized region when game starts
        private Map<TextureRegion, String> puzzleNames = new HashMap<TextureRegion, String>();

        @Override
        public void create() {
                preferences = Gdx.app.getPreferences("PhysicsPuzzle");

                Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

                // ui stuff
                ScreenViewport sv = new ScreenViewport();
                float ui_scale = (float) Math.abs(Math.log(Gdx.graphics.getDensity())) * 2f;
                sv.setUnitsPerPixel(1f / ui_scale);
                this.stage = new Stage(sv);
                skin = new Skin(Gdx.files.internal("ui/custom/custom.json"));


                // backgrounds and thumbnails are here
                textureAtlas = new TextureAtlas("images/packed/packed.atlas");


                // input
                inputMultiplexer = new InputMultiplexer();
                Gdx.input.setInputProcessor(inputMultiplexer);
                inputMultiplexer.addProcessor(stage);


                // background stuff
                polygonBatch = new PrettyPolygonBatch();
                backgroundCamera = new OrthographicCamera();


                showMainMenu();

        }

        /** Update the background so it looks proper. Must be done after every resize. */
        private void updateBackgroundBounds(TexturePolygon texturePolygon) {
                backgroundCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

                Array<Vector2> vertices = new Array<Vector2>();

                float halfWidth = Gdx.graphics.getWidth() * 0.5f;
                float halfHeight = Gdx.graphics.getHeight() * 0.5f;


                vertices.add(new Vector2(-halfWidth, -halfHeight));
                vertices.add(new Vector2(halfWidth, -halfHeight));
                vertices.add(new Vector2(halfWidth, halfHeight));
                vertices.add(new Vector2(-halfWidth, halfHeight));

                texturePolygon.setVertices(vertices);
                texturePolygon.setPosition(halfWidth, halfHeight);
                texturePolygon.setTextureScale(1);

        }

        /** Set a new background. */
        private TexturePolygon setBackground(TextureRegion textureRegion) {
                if (menuBackground != null) {
                        TextureRegion textureRegion1 = menuBackground.getTextureRegion();
                        if (textureRegion == textureRegion1) return menuBackground;
                }

                TexturePolygon texturePolygon = new TexturePolygon();
                texturePolygon.setTextureRegion(textureRegion);

                updateBackgroundBounds(texturePolygon);
                return texturePolygon;
        }

        /**
         * Selects a random image from folder. Then finds the corresponding texture
         * in the atlas, returns null if image is not in atlas.
         */
        private TextureRegion getRandomRegion(String folder) {
                // find all images in given folder
                FileHandle fileHandle = Gdx.files.internal(folder);
                FileHandle[] imageFiles = fileHandle.list(new FileFilter() {
                        @Override
                        public boolean accept(File file) {
                                String name = file.getName().toLowerCase();
                                return name.endsWith(".png") || name.endsWith(".jpg");
                        }
                });

                // select random one
                FileHandle selected = imageFiles[MathUtils.random(imageFiles.length - 1)];

                // get the name it has in the atlas
                String name = selected.pathWithoutExtension().replace("images/for packing/", "");

                return textureAtlas.findRegion(name);
        }


        /**
         * Fills an array with images found in the thumbnails folder. The textures
         * are from the texture atlas. Also stores the names of corresponding
         * normally sized textures in the map {@link #puzzleNames}.
         */
        private Array<Image> loadPuzzleImages() {

                // find thumbnail names
                FileHandle fileHandle = Gdx.files.internal("images/for packing/thumbnails");
                FileHandle[] imageFiles = fileHandle.list(new FileFilter() {
                        @Override
                        public boolean accept(File file) {
                                String name = file.getName().toLowerCase();
                                return name.endsWith(".png") || name.endsWith(".jpg");
                        }
                });


                Array<Image> images = new Array<Image>();
                puzzleNames.clear();

                for (FileHandle imageFile : imageFiles) {

                        // find thumbnail region
                        String name = imageFile.pathWithoutExtension().replace("images/for packing/", "");
                        TextureRegion textureRegion = textureAtlas.findRegion(name);

                        // add image
                        final Image image = new Image(textureRegion);
                        images.add(image);
                        image.addListener(new ClickListener() {
                                @Override
                                public void clicked(InputEvent event, float x, float y) {
                                        selectImage(image);
                                }
                        });

                        // save name for later (we need it to find the normal sized image before game starts)
                        name = imageFile.path().replace("images/for packing/thumbnails/", "");
                        puzzleNames.put(textureRegion, name);
                }
                return images;
        }


        /** Start puzzling. */
        private void startGame(TextureRegion textureRegion, int rows, int columns, float interval) {

                if (physicsPuzzle != null) {
                        physicsPuzzle.dispose();
                }


                final PhysicsPuzzleDef physicsPuzzleDef = new PhysicsPuzzleDef();
                physicsPuzzleDef.columns = columns;
                physicsPuzzleDef.rows = rows;
                physicsPuzzleDef.timeBetweenBlocks = interval;
                physicsPuzzleDef.outlineColor.set(Color.BLACK);

                physicsPuzzle = new PhysicsPuzzle();
                physicsPuzzle.create(inputMultiplexer, textureRegion, physicsPuzzleDef);

                resumeGame();
        }

        private boolean canResumeGame() {
                return physicsPuzzle != null;
        }

        /**
         * Resumes game if there is any.
         * Clears other menus and shows some buttons in the top left corner.
         * Changes background.
         */
        private void resumeGame() {
                hideAndPauseGame = false;

                stage.clear();

                final Table inGameButtonTable = new Table();

                // sets position of buttons to top left corner
                onResize = new Runnable() {
                        @Override
                        public void run() {
                                inGameButtonTable.setPosition(
                                        inGameButtonTable.getPrefWidth() * 0.5f,
                                        stage.getHeight() - inGameButtonTable.getPrefHeight() * 0.5f);
                        }
                };


                // create buttons

                final TextButton menuButton = new TextButton("Menu", skin);
                menuButton.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                                showMainMenu();
                                hideAndPauseGame = true;
                        }
                });


                final TextButton pauseButton = new TextButton(physicsPuzzle.isPaused() ? "Resume" : "Pause", skin);
                pauseButton.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                                physicsPuzzle.setPaused(!physicsPuzzle.isPaused());
                                pauseButton.setText(physicsPuzzle.isPaused() ? "Resume" : "Pause");


                                if (physicsPuzzle.isPaused()) {
                                        inGameButtonTable.add(menuButton).width(pauseButton.getPrefWidth());
                                } else inGameButtonTable.removeActor(menuButton);


                                onResize.run();
                        }
                });


                // show some buttons
                inGameButtonTable.add(pauseButton).padBottom(computePreferredPadding()).row();
                stage.addActor(inGameButtonTable);

                if (physicsPuzzle.isPaused()) {
                        inGameButtonTable.add(menuButton).width(pauseButton.getPrefWidth());
                } else inGameButtonTable.removeActor(menuButton);


                menuBackground = setBackground(getRandomRegion("images/for packing/backgrounds-light"));

                onResize.run();
        }

        /** Clear other menus and show menu for customizing a new game. */
        private void showCustomizationMenu() {
                onResize = new Runnable() {
                        @Override
                        public void run() {
                                showCustomizationMenu();
                        }
                };


                Table customizeMenu = createCustomizationComponents();

                stage.clear();
                stage.addActor(customizeMenu);
                fillAndCenter(customizeMenu);

        }

        /** Create components for customizing the game after selecting a picture. */
        private Table createCustomizationComponents() {
                int minRows = 1, maxRows = 50;
                int minColumns = 1, maxColumns = 50;
                float minInterval = 0.2f, maxInterval = 7f, intervalStep = 0.1f;

                boolean tallScreen = Gdx.graphics.getHeight() > Gdx.graphics.getWidth();
                float preferredLabelWidth = new Label("0000", skin).getPrefWidth();
                float preferredPadding = computePreferredPadding();
                float buttonWidth = computePreferredButtonWidth();

                // prepare sliders

                Table controlsTable = new Table(skin);


                // row count slider and labels
                Cell<Label> cell = controlsTable.add("Rows");
                if (tallScreen) cell.row();

                final Slider rowSlider = new Slider(minRows, maxRows, 1, false, skin);
                rowSlider.setValue(preferences.getInteger("rows", 7));
                controlsTable.add(rowSlider).padBottom(preferredPadding).width(buttonWidth);

                final Label rowCounter = new Label(String.valueOf(rowSlider.getValue()), skin);
                cell = controlsTable.add(rowCounter).width(preferredLabelWidth);
                cell.row();

                if (tallScreen) cell.pad(preferredPadding);

                rowSlider.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                                rowCounter.setText(String.valueOf(rowSlider.getValue()));
                        }
                });


                // column count slider and labels
                cell = controlsTable.add("Columns");
                if (tallScreen) cell.row();


                final Slider columnSlider = new Slider(minColumns, maxColumns, 1, false, skin);
                columnSlider.setValue(preferences.getInteger("columns", 7));
                controlsTable.add(columnSlider).padBottom(preferredPadding).width(buttonWidth);

                final Label columnCounter = new Label(String.valueOf(columnSlider.getValue()), skin);
                cell = controlsTable.add(columnCounter).width(preferredLabelWidth);
                cell.row();

                if (tallScreen) cell.pad(preferredPadding);

                columnSlider.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                                columnCounter.setText(String.valueOf(columnSlider.getValue()));
                        }
                });


                // interval slider and labels
                cell = controlsTable.add("Time between blocks");
                if (tallScreen) cell.row();


                final Slider intervalSlider = new Slider(minInterval, maxInterval, intervalStep, false, skin);
                intervalSlider.setValue(preferences.getFloat("interval", 1.5f));
                controlsTable.add(intervalSlider).padBottom(preferredPadding).width(buttonWidth);

                final Label intervalCounter = new Label(String.valueOf(intervalSlider.getValue()), skin);
                cell = controlsTable.add(intervalCounter).width(preferredLabelWidth);
                cell.row();

                if (tallScreen) cell.pad(preferredPadding);

                intervalSlider.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                                intervalCounter.setText(Util.safeSubstring(intervalSlider.getValue(), 4));
                        }
                });


                // prepare buttons

                Table buttonTable = new Table();


                TextButton playButton = new TextButton("Play", skin);
                Cell<TextButton> cell1 = buttonTable.add(playButton).width(buttonWidth).pad(preferredPadding);
                if (tallScreen) cell1.row();

                playButton.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {

                                // load the normally sized texture
                                TextureRegionDrawable drawable = (TextureRegionDrawable) selectedImage.getDrawable();
                                TextureRegion thumbnailRegion = drawable.getRegion();
                                String name = puzzleNames.get(thumbnailRegion);
                                if (currentPuzzleTextureRegion != null)
                                        currentPuzzleTextureRegion.getTexture().dispose();

                                currentPuzzleTextureRegion = new TextureRegion(new Texture("images/puzzles/" + name));

                                // some game settings
                                int rows = (int) rowSlider.getValue();
                                int columns = (int) columnSlider.getValue();
                                float interval = intervalSlider.getValue();


                                startGame(currentPuzzleTextureRegion, rows, columns, interval);

                                // remember to next time
                                preferences.putInteger("rows", rows);
                                preferences.putInteger("columns", columns);
                                preferences.putFloat("interval", interval);

                        }
                });


                TextButton backButton = new TextButton("Back", skin);
                Cell<TextButton> cell2 = buttonTable.add(backButton).width(buttonWidth).pad(preferredPadding);
                cell2.row();
                backButton.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                                showImageSelectionMenu();
                        }
                });

                if (!tallScreen) swapActors(cell1, cell2);


                // prepare the table
                Table customizeMenu = new Table();
                customizeMenu.add(controlsTable).pad(preferredPadding).row();
                customizeMenu.add(buttonTable).pad(preferredPadding).align(Align.center).row();
                return customizeMenu;
        }

        private void swapActors(Cell cell, Cell cell1) {
                Actor actor = cell.getActor();
                Actor actor1 = cell1.getActor();

                cell.clearActor();
                cell1.clearActor();

                cell.setActor(actor1);
                cell1.setActor(actor);
        }

        private float computePreferredPadding() {
                float preferred = Math.min(stage.getWidth(), stage.getHeight()) * 0.005f;

                float minimum = (float) Math.sqrt(Gdx.graphics.getDensity()) * 2f;
                if (preferred < minimum)
                        preferred = minimum;

                return preferred;
        }

        private float computePreferredButtonWidth() {
                float preferred = Math.min(stage.getWidth(), stage.getHeight()) * 0.3f;

                float minimum = 250f;
                if (preferred < minimum)
                        preferred = minimum;

                return preferred;
        }

        private float computePreferredImageHeight() {
                return Math.min(stage.getWidth(), stage.getHeight()) * 0.5f;
        }

        /** Clear other menus. Show main menu. */
        private void showMainMenu() {
                onResize = new Runnable() {
                        @Override
                        public void run() {
                                showMainMenu();
                        }
                };

                menuBackground = setBackground(getRandomRegion("images/for packing/backgrounds-dark"));

                Table mainMenu = createMainMenuComponents();

                stage.clear();
                stage.addActor(mainMenu);

                fillAndCenter(mainMenu);
        }

        private Table createMainMenuComponents() {
                Table buttonTable = new Table();


                float buttonWidth = computePreferredButtonWidth();

                if (canResumeGame()) {
                        TextButton resumeButton = new TextButton("Resume", skin);
                        buttonTable.add(resumeButton).width(buttonWidth).pad(computePreferredPadding()).row();
                        resumeButton.addListener(new ClickListener() {
                                @Override
                                public void clicked(InputEvent event, float x, float y) {
                                        resumeGame();
                                }
                        });
                }

                TextButton playButton = new TextButton("New Game", skin);
                buttonTable.add(playButton).width(buttonWidth).pad(computePreferredPadding()).row();
                playButton.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                                showImageSelectionMenu();
                        }
                });


                TextButton exitButton = new TextButton("Exit", skin);
                buttonTable.add(exitButton).width(buttonWidth).pad(computePreferredPadding()).row();
                exitButton.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                                Gdx.app.exit();
                        }
                });


                Table mainMenu = new Table();
                mainMenu.add(buttonTable);
                return mainMenu;

        }

        /** Clear other menus. Show a scroll pane with thumbnails. Also some buttons. */
        private void showImageSelectionMenu() {
                onResize = new Runnable() {
                        @Override
                        public void run() {
                                showImageSelectionMenu();
                        }
                };


                Table imageSelectionMenu = createImageSelectionComponents();


                stage.clear();
                stage.addActor(imageSelectionMenu);
                fillAndCenter(imageSelectionMenu);


                Image toSelect = selectedImage;
                selectedImage = null;
                selectImage(toSelect);

        }

        /** Create the scrolling images and some buttons. */
        private Table createImageSelectionComponents() {

                // prepare images
                Table imageTable = new Table();
                Array<Image> puzzleImages = loadPuzzleImages();
                for (int i = 0; i < puzzleImages.size; i++) {
                        Image image = puzzleImages.get(i);


                        float imageWidth = image.getDrawable().getMinWidth();
                        float imageHeight = image.getDrawable().getMinHeight();

                        float height = computePreferredImageHeight();
                        float width = height * imageWidth / imageHeight;

                        float bigPad = width * 0.1f;
                        float smallPad = width * 0.02f;

                        float padLeft = i == 0 ? bigPad : smallPad;
                        float padRight = i == puzzleImages.size - 1 ? bigPad : smallPad;

                        image.addAction(Actions.sizeTo(width, height));

                        imageTable.add(image).width(width).height(height).align(Align.center).pad(bigPad, padLeft, bigPad, padRight);
                }


                ScrollPane imageScroller = new ScrollPane(imageTable, skin);
                imageScroller.setScrollingDisabled(false, true);


                Table buttonTable = new Table();


                // prepare buttons

                float buttonWidth = computePreferredButtonWidth();
                TextButton selectButton = new TextButton("Select", skin);
                Cell<TextButton> cell = buttonTable.add(selectButton).width(buttonWidth).pad(computePreferredPadding());
                selectButton.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                                showCustomizationMenu();
                        }
                });


                TextButton backButton = new TextButton("Back", skin);
                Cell<TextButton> cell1 = buttonTable.add(backButton).width(buttonWidth).pad(computePreferredPadding());
                cell1.row();
                backButton.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                                showMainMenu();
                        }
                });


                // feels better
                boolean tallScreen = Gdx.graphics.getHeight() > Gdx.graphics.getWidth();
                if (!tallScreen) swapActors(cell, cell1);


                Table imageSelectionMenu = new Table();
                imageSelectionMenu.add(imageScroller).row();
                imageSelectionMenu.add(buttonTable);


                return imageSelectionMenu;
        }

        /** Animate the image so it looks bigger. Also resize the previously selected image to normal size. */
        private void selectImage(Image image) {
                // scale up current image
                if (selectedImage != null && selectedImage != image) {

                        float imageWidth = selectedImage.getDrawable().getMinWidth();
                        float imageHeight = selectedImage.getDrawable().getMinHeight();

                        float height = computePreferredImageHeight();
                        float width = height * imageWidth / imageHeight;

                        selectedImage.addAction(Actions.sizeTo(width, height, 0.2f, Interpolation.pow2));
                        selectedImage.addAction(Actions.moveBy(width * 0.2f, height * 0.2f, 0.2f, Interpolation.pow2));
                }

                // scale down previous image
                if (selectedImage != image) {

                        float imageWidth = image.getDrawable().getMinWidth();
                        float imageHeight = image.getDrawable().getMinHeight();

                        float height = computePreferredImageHeight();
                        float width = height * imageWidth / imageHeight;

                        image.addAction(Actions.sizeTo(width * 1.4f, height * 1.4f, 0.2f, Interpolation.pow2));
                        image.addAction(Actions.moveBy(-width * 0.2f, -height * 0.2f, 0.2f, Interpolation.pow2));

                        image.toFront();
                }

                selectedImage = image;

        }


        @Override
        public void render() {
                Gdx.gl20.glClearColor(0, 0, 0, 1);
                Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);


                polygonBatch.begin(backgroundCamera);
                menuBackground.draw(polygonBatch);
                polygonBatch.end();


                if (!hideAndPauseGame && physicsPuzzle != null) {
                        physicsPuzzle.render();
                }


                stage.act();
                stage.draw();
        }


        private void fillAndCenter(Table table) {
                table.setWidth(stage.getWidth());
                table.setHeight(stage.getHeight());

                float x = stage.getWidth() * 0.5f;
                float y = stage.getHeight() * 0.5f;

                table.setPosition(x, y, Align.center);
        }

        @Override
        public void dispose() {
                if (skin != null) skin.dispose();
                if (stage != null) stage.dispose();
                if (physicsPuzzle != null) physicsPuzzle.dispose();
                if (polygonBatch != null) polygonBatch.dispose();
                if (textureAtlas != null) textureAtlas.dispose();
                if (preferences != null) preferences.flush();
                if (currentPuzzleTextureRegion != null) currentPuzzleTextureRegion.getTexture().dispose();

                skin = null;
                stage = null;
                physicsPuzzle = null;
                polygonBatch = null;
                textureAtlas = null;
                preferences = null;
                currentPuzzleTextureRegion = null;

        }

        @Override
        public void resize(int width, int height) {
                if (stage != null) stage.getViewport().update(width, height, true);
                if (onResize != null) onResize.run();
                if (physicsPuzzle != null) physicsPuzzle.resize(width, height);
                if (menuBackground != null) updateBackgroundBounds(menuBackground);
        }


}
