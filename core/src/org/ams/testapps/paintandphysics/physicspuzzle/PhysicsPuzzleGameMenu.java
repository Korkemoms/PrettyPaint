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

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import org.ams.core.Util;
import org.ams.prettypaint.PrettyPolygonBatch;
import org.ams.prettypaint.TexturePolygon;

import static org.ams.core.SceneUtil.*;

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
        private Image selectedThumbnail;

        private Runnable onResize; // used to lay out menus after resize

        private InputMultiplexer inputMultiplexer;

        private boolean hideAndPauseGame = true;

        // background stuff
        private OrthographicCamera backgroundCamera;
        private TexturePolygon background;
        private PrettyPolygonBatch polygonBatch;

        // puzzle textures
        private Array<String> availableRegions;
        private TextureAtlas textureAtlas; // thumbnails and backgrounds in this atlas
        private TextureRegion currentPuzzleTextureRegion; // stored here so it can be dispose()'ed

        // maps thumbnail regions to names used to load a normal sized region when game starts
        private Map<TextureRegion, String> puzzleNames = new HashMap<TextureRegion, String>();


        private static final int MAIN_MENU = 0, SETTINGS_MENU = 1, IMAGE_MENU = 2, IN_GAME = 3;
        private int currentScreen = MAIN_MENU, lastScreen;

        @Override
        public void create() {

                boolean verbose = true;
                Gdx.app.setLogLevel(verbose ? Application.LOG_DEBUG : Application.LOG_ERROR);


                Gdx.app.log("PhysicsPuzzleGameMenu", "Creating application PhysicsPuzzleGameMenu");

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
                availableRegions = findLinesThatContain("images/packed/packed.atlas", "thumbnails");


                // input
                inputMultiplexer = new InputMultiplexer();
                Gdx.input.setInputProcessor(inputMultiplexer);
                inputMultiplexer.addProcessor(stage);


                // background stuff
                polygonBatch = new PrettyPolygonBatch();
                backgroundCamera = new OrthographicCamera();


                showMainMenu();

        }

        /** This is a really crappy solution. */
        private Array<String> getAvailableBackgrounds(String match) {
                Array<String> backgrounds = new Array<String>();

                String path = "images/backgrounds-dark/escheresque_ste.png";
                if (path.contains(match)) backgrounds.add(path);

                path = "images/backgrounds-light/giftly.png";
                if (path.contains(match)) backgrounds.add(path);

                path = "images/backgrounds-light/sativa.png";
                if (path.contains(match)) backgrounds.add(path);

                path = "images/backgrounds-light/restaurant_icons.png";
                if (path.contains(match)) backgrounds.add(path);

                return backgrounds;
        }

        /** Returns all the lines in the file that contain one or more of the Strings given. */
        private Array<String> findLinesThatContain(String path, String... searchFor) {
                String file = Gdx.files.internal(path).readString();
                String[] lines = file.split("\\n");

                Array<String> result = new Array<String>();

                for (String s : lines) {
                        boolean match = false;
                        for (String contain : searchFor) {
                                if (s.contains(contain)) {
                                        match = true;
                                        break;
                                }
                        }
                        if (match) result.add(s);
                }
                return result;

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
        private TexturePolygon setRandomBackground(Array<String> selectFrom) {

                if (background != null) {
                        background.getTextureRegion().getTexture().dispose();
                }

                Texture texture = new Texture(selectFrom.random());
                texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);


                TexturePolygon texturePolygon = new TexturePolygon();
                texturePolygon.setTextureRegion(new TextureRegion(texture));

                updateBackgroundBounds(texturePolygon);
                return texturePolygon;
        }


        /**
         * Loads the big region for the selectedThumbnail and loads
         * game settings from {@link #preferences}.
         * Then it starts the game.
         */
        private void startGame() {
                if (currentPuzzleTextureRegion != null)
                        currentPuzzleTextureRegion.getTexture().dispose();

                // load the normally sized texture
                TextureRegion thumbnailRegion = getTextureRegion(selectedThumbnail);
                currentPuzzleTextureRegion = getBigRegion(thumbnailRegion);

                String name = puzzleNames.get(thumbnailRegion).replace("thumbnails/", "");

                // some game settings
                int rows = preferences.getInteger("Rows", 7);
                int columns = preferences.getInteger("Columns", 7);
                float interval = preferences.getFloat("Interval", -1);


                startGame(currentPuzzleTextureRegion, name, rows, columns, interval);
        }

        /** Start puzzling. */
        private void startGame(TextureRegion textureRegion, String textureRegionName, int rows, int columns, float interval) {

                Gdx.app.log("PhysicsPuzzleGameMenu",
                        "Starting game with textureRegion=" + puzzleNames.get(textureRegion)
                                + ", rows=" + rows + ", columns=" + columns + ", interval=" + interval);


                if (physicsPuzzle != null) {
                        physicsPuzzle.dispose();
                }


                final PhysicsPuzzleDef physicsPuzzleDef = new PhysicsPuzzleDef();
                physicsPuzzleDef.columns = columns;
                physicsPuzzleDef.rows = rows;
                physicsPuzzleDef.interval = interval;
                physicsPuzzleDef.outlineColor.set(Color.BLACK);
                physicsPuzzleDef.textureRegionName = textureRegionName;

                physicsPuzzle = new PhysicsPuzzle();
                physicsPuzzle.create(inputMultiplexer, textureRegion, physicsPuzzleDef, new PhysicsPuzzle.Callback() {
                        @Override
                        public void gameOver(boolean win) {
                                showGameOverUi();
                        }
                });

                physicsPuzzle.setZoom(1.25f);
                physicsPuzzle.setPosition(0, 0.5f);

                resumeGame();

        }

        /** Clear other ui and show just a new game button. */
        private void showGameOverUi() {

                onResize = new Runnable() {
                        @Override
                        public void run() {
                                showGameOverUi();
                        }
                };

                physicsPuzzle.setZoom(2.2f);
                physicsPuzzle.setPosition(0, -0.95f);



                showImageSelectionMenu(Align.bottom,Color.BLACK);


        }

        private void addMainMenuButton(Color color) {
                Table buttonTable = createButtonTable(color);

                stage.addActor(buttonTable);

                float x = computePreferredButtonWidth() * 0.5f;
                float y = stage.getHeight() - buttonTable.getPrefHeight() * 0.5f;

                buttonTable.setPosition(x, y);
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
                lastScreen = currentScreen;
                currentScreen = IN_GAME;

                Gdx.app.log("PhysicsPuzzleGameMenu", "Resuming game");
                hideAndPauseGame = false;


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
                menuButton.setColor(Color.BLACK);
                menuButton.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                                showMainMenu();
                        }
                });


                final TextButton pauseButton = new TextButton(physicsPuzzle.isPaused() ? "Resume" : "Pause", skin);
                pauseButton.setColor(Color.BLACK);
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


                background = setRandomBackground(getAvailableBackgrounds("light"));

                onResize.run();
        }

        /** Clear other menus and show menu for customizing a new game. */
        private void showCustomizationMenu() {
                lastScreen = currentScreen;
                currentScreen = SETTINGS_MENU;

                Gdx.app.log("PhysicsPuzzleGameMenu", "Showing customization menu");
                onResize = new Runnable() {
                        @Override
                        public void run() {
                                showCustomizationMenu();
                        }
                };


                Table customizeMenu = createCustomizationComponents();

                stage.clear();

                stage.addActor(customizeMenu);
                fillAndCenter(stage, customizeMenu);


        }

        /** Create components for customizing the game after selecting a picture. */
        private Table createCustomizationComponents() {

                Gdx.app.log("PhysicsPuzzleGameMenu", "Creating customization components");
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
                rowSlider.setValue(preferences.getInteger("Rows", 7));
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
                columnSlider.setValue(preferences.getInteger("Columns", 7));
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


                // prepare interval controls
                final CheckBox intervalCheckBox = new CheckBox("Drop interval", skin);
                intervalCheckBox.setChecked(preferences.getBoolean("EnableInterval", false));


                final Slider intervalSlider = new Slider(minInterval, maxInterval, intervalStep, false, skin);
                intervalSlider.setValue(preferences.getFloat("Interval", 1.5f));
                intervalSlider.setVisible(intervalCheckBox.isChecked());

                final Label intervalCounter = new Label(Util.safeSubstring(intervalSlider.getValue(), 4) + " s", skin);
                intervalCounter.setVisible(intervalCheckBox.isChecked());

                intervalSlider.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                                intervalCounter.setText(Util.safeSubstring(intervalSlider.getValue(), 4) + " s");
                        }
                });

                intervalCheckBox.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                                boolean enabled = intervalCheckBox.isChecked();

                                intervalSlider.setVisible(enabled);
                                intervalCounter.setVisible(enabled);

                                preferences.putBoolean("EnableInterval", enabled);
                        }
                });

                // add interval controls
                Cell<CheckBox> cell3 = controlsTable.add(intervalCheckBox);
                if (tallScreen) cell3.row();
                controlsTable.add(intervalSlider).padBottom(preferredPadding).width(buttonWidth);
                cell = controlsTable.add(intervalCounter).width(preferredLabelWidth);
                cell.row();
                if (tallScreen) cell.pad(preferredPadding);


                // prepare buttons

                Table buttonTable = new Table();
                Cell<TextButton> cell1 = null;
                boolean addPlayButton = lastScreen == IMAGE_MENU;


                TextButton backButton = new TextButton("Back", skin);
                Cell<TextButton> cell2 = buttonTable.add(backButton).width(buttonWidth).pad(preferredPadding);
                cell2.row();
                backButton.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {

                                showMainMenu();

                                // remember to next time

                                int rows = (int) rowSlider.getValue();
                                int columns = (int) columnSlider.getValue();
                                float interval = intervalCheckBox.isChecked() ? intervalSlider.getValue() : -1;

                                preferences.putInteger("Rows", rows);
                                preferences.putInteger("Columns", columns);
                                preferences.putFloat("Interval", interval);
                        }
                });

                if (!tallScreen && addPlayButton) swapActors(cell1, cell2);


                // prepare the table
                Table customizeMenu = new Table();
                customizeMenu.add(controlsTable).pad(preferredPadding).row();
                customizeMenu.add(buttonTable).pad(preferredPadding).align(Align.center).row();
                return customizeMenu;
        }


        /** Get the big version of the thumbnail. */
        private TextureRegion getBigRegion(TextureRegion thumbnailRegion) {
                String name = "images/puzzles/" + puzzleNames.get(thumbnailRegion).replace("thumbnails/", "");
                TextureRegion textureRegion;
                try {
                        textureRegion = new TextureRegion(new Texture(name + ".jpg"));
                } catch (GdxRuntimeException e) {
                        textureRegion = new TextureRegion(new Texture(name + ".png"));
                }
                return textureRegion;
        }

        /** Get the TextureRegion that the image is drawing. */
        private TextureRegion getTextureRegion(Image image) {
                TextureRegionDrawable drawable = (TextureRegionDrawable) selectedThumbnail.getDrawable();
                return drawable.getRegion();
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
                return computePreferredImageHeight(0.5f);
        }

        private float computePreferredImageHeight(float scale) {
                return Math.min(stage.getWidth(), stage.getHeight()) * scale;
        }

        /** Clear other menus. Show main menu. */
        private void showMainMenu() {
                lastScreen = currentScreen;
                currentScreen = MAIN_MENU;

                Gdx.app.log("PhysicsPuzzleGameMenu", "Showing main menu");
                onResize = new Runnable() {
                        @Override
                        public void run() {
                                showMainMenu();
                        }
                };

                background = setRandomBackground(getAvailableBackgrounds("dark"));

                Table mainMenu = createMainMenuComponents();

                stage.clear();

                stage.addActor(mainMenu);

                hideAndPauseGame = true;

                fillAndCenter(stage, mainMenu);
        }

        private Table createMainMenuComponents() {

                Gdx.app.log("PhysicsPuzzleGameMenu", "Creating main menu components");
                Table buttonTable = new Table();


                float buttonWidth = computePreferredButtonWidth();

                if (canResumeGame()) {
                        TextButton resumeButton = new TextButton("Resume", skin);
                        buttonTable.add(resumeButton).width(buttonWidth).pad(computePreferredPadding()).row();
                        resumeButton.addListener(new ClickListener() {
                                @Override
                                public void clicked(InputEvent event, float x, float y) {

                                        resumeGame();

                                        if (physicsPuzzle.isGameOver())
                                                showGameOverUi();
                                }
                        });
                }

                TextButton playButton = new TextButton("New Game", skin);
                buttonTable.add(playButton).width(buttonWidth).pad(computePreferredPadding()).row();
                playButton.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                                showImageSelectionMenu(Align.center,Color.WHITE);
                        }
                });


                TextButton settingsButton = new TextButton("Settings", skin);
                buttonTable.add(settingsButton).width(buttonWidth).pad(computePreferredPadding()).row();
                settingsButton.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                                showCustomizationMenu();
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
        private void showImageSelectionMenu(final int align,final Color buttonColor) {
                lastScreen = currentScreen;
                currentScreen = IMAGE_MENU;

                Gdx.app.log("PhysicsPuzzleGameMenu", "Showing image selection menu");
                onResize = new Runnable() {
                        @Override
                        public void run() {
                                showImageSelectionMenu(align,buttonColor);
                        }
                };

                selectedThumbnail = null;
                Table imageSelectionMenu = createImageSelectionComponents(computePreferredImageHeight(), align != Align.bottom);

                stage.clear();
                stage.addActor(imageSelectionMenu);


                float relativeHeight = 0.5f;
                float preferredImageHeight = computePreferredImageHeight(relativeHeight);
                imageSelectionMenu.setWidth(stage.getWidth());
                imageSelectionMenu.setHeight(stage.getHeight() * relativeHeight * 1.4f);

                float x = stage.getWidth() * 0.5f;


                float y = 0;
                if (align == Align.bottom) {
                        y = preferredImageHeight * 0.5f + preferredImageHeight * 0.02f;
                } else if (align == Align.center) {
                        y = Gdx.graphics.getHeight() * 0.5f;
                }


                imageSelectionMenu.setPosition(x, y, Align.center);


                addMainMenuButton(buttonColor);
        }

        /**
         * Fills an array with images found in the thumbnails folder. The textures
         * are from the texture atlas. Also stores the names of corresponding
         * normally sized textures in the map {@link #puzzleNames}.
         */
        private Array<Image> preparePuzzleThumbnails(final float preferredHeight) {
                Gdx.app.log("PhysicsPuzzleGameMenu", "Loading puzzle thumbnails");


                Array<Image> images = new Array<Image>();
                puzzleNames.clear();

                for (final String name : availableRegions) {

                        if (!name.contains("thumbnails")) continue;

                        TextureRegion textureRegion = textureAtlas.findRegion(name);

                        // add image
                        final Image image = new Image(textureRegion) {

                                // implement frustum culling
                                Rectangle cullingArea = new Rectangle();

                                @Override
                                public void draw(Batch batch, float parentAlpha) {
                                        validate();

                                        getActorBounds(this, cullingArea);

                                        // polygonBatch.frustum will be up to date as long as we are drawing backgrounds
                                        if (cullingArea.overlaps(polygonBatch.frustum))
                                                super.draw(batch, parentAlpha);

                                }
                        };
                        images.add(image);
                        image.addListener(new ClickListener() {

                                @Override
                                public void clicked(InputEvent event, float x, float y) {
                                        if (image == selectedThumbnail) {


                                                stage.clear();
                                                startGame(); // clicked on already selected thumbnail }
                                        } else
                                                selectThumbnail(image, preferredHeight);
                                }
                        });

                        // save name for later (we need it to find the normal sized image before game starts)
                        puzzleNames.put(textureRegion, name);
                }
                return images;
        }

        /** Create the scrolling images and some buttons. */
        private Table createImageSelectionComponents(float preferredImageHeight, boolean scrollBarBottom) {

                Gdx.app.log("PhysicsPuzzleGameMenu", "Creating image selection components");
                // prepare images
                Table imageTable = new Table();
                final Array<Image> puzzleImages = preparePuzzleThumbnails(preferredImageHeight);
                for (int i = 0; i < puzzleImages.size; i++) {
                        Image image = puzzleImages.get(i);


                        float imageWidth = image.getDrawable().getMinWidth();
                        float imageHeight = image.getDrawable().getMinHeight();

                        float height = preferredImageHeight;
                        float width = height * imageWidth / imageHeight;

                        float bigPad = height * 0.1f;
                        float smallPad = height * 0.02f;

                        float padLeft = i == 0 ? bigPad : smallPad;
                        float padRight = i == puzzleImages.size - 1 ? bigPad : smallPad;

                        image.addAction(Actions.sizeTo(width, height));

                        imageTable.add(image).width(width).height(height).align(Align.center).pad(bigPad, padLeft, bigPad, padRight);

                }


                final ScrollPane imageScroller = new ScrollPane(imageTable, skin);
                imageScroller.setScrollingDisabled(false, true);
                imageScroller.setScrollBarPositions(scrollBarBottom, true);


                Gdx.app.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                                Image image = puzzleImages.random();
                                Rectangle bounds = getActorBounds(image, new Rectangle());
                                imageScroller.scrollTo(bounds.x, bounds.y, bounds.width, bounds.height, true, true);
                        }
                });


                Table imageSelectionMenu = new Table();
                imageSelectionMenu.add(imageScroller);


                return imageSelectionMenu;
        }

        private Table createButtonTable(Color buttonColor) {
                Table buttonTable = new Table();


                // prepare buttons

                float buttonWidth = computePreferredButtonWidth();

                TextButton backButton = new TextButton("Main Menu", skin);
                backButton.setColor(buttonColor);
                buttonTable.add(backButton).width(buttonWidth);
                backButton.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                                showMainMenu();
                        }
                });
                return buttonTable;
        }


        /** Animate the image so it looks bigger. Also resize the previously selected image to normal size. */
        private void selectThumbnail(Image image, float preferredHeight) {
                if (image == null) {
                        Gdx.app.log("PhysicsPuzzleGameMenu", "Selecting null image");
                        return;
                }


                Gdx.app.log("PhysicsPuzzleGameMenu", "Selecting image " + image.getName());

                // scale down previous image
                if (selectedThumbnail != null && selectedThumbnail != image) {

                        float imageWidth = selectedThumbnail.getDrawable().getMinWidth();
                        float imageHeight = selectedThumbnail.getDrawable().getMinHeight();

                        float height = preferredHeight;
                        float width = height * imageWidth / imageHeight;


                        selectedThumbnail.addAction(Actions.sizeTo(width, height, 0.2f, Interpolation.pow2));
                        selectedThumbnail.addAction(Actions.moveBy(width * 0.2f, height * 0.2f, 0.2f, Interpolation.pow2));
                }


                // scale up current image

                if (selectedThumbnail != image) {

                        float imageWidth = image.getDrawable().getMinWidth();
                        float imageHeight = image.getDrawable().getMinHeight();

                        float height = computePreferredImageHeight();
                        float width = height * imageWidth / imageHeight;

                        image.addAction(Actions.sizeTo(width * 1.4f, height * 1.4f, 0.2f, Interpolation.pow2));
                        image.addAction(Actions.moveBy(-width * 0.2f, -height * 0.2f, 0.2f, Interpolation.pow2));

                        image.toFront();
                }

                selectedThumbnail = image;

        }


        @Override
        public void render() {
                Gdx.gl20.glClearColor(0, 0, 0, 1);
                Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);


                polygonBatch.begin(backgroundCamera);
                background.draw(polygonBatch);
                polygonBatch.end();


                if (!hideAndPauseGame && physicsPuzzle != null) {
                        physicsPuzzle.render();
                }


                stage.act();
                stage.draw();
        }


        @Override
        public void dispose() {

                Gdx.app.log("PhysicsPuzzleGameMenu", "Disposing resources");
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

                Gdx.app.log("PhysicsPuzzleGameMenu", "Finished disposing resources");
        }

        @Override
        public void resize(int width, int height) {

                Gdx.app.log("PhysicsPuzzleGameMenu", "Resizing to width=" + width + ", height = " + height + ".");
                if (stage != null) stage.getViewport().update(width, height, true);
                if (onResize != null) onResize.run();
                if (physicsPuzzle != null) physicsPuzzle.resize(width, height);
                if (background != null) updateBackgroundBounds(background);
        }


}
