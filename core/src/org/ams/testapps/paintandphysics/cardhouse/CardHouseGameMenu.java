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
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import org.ams.core.SceneUtil;
import org.ams.core.Util;

import java.io.File;
import java.io.FileFilter;
import java.util.Locale;

import static org.ams.core.SceneUtil.*;

/**
 * Menu and background for the card house game. When a new game is started
 * the menu is hidden and a new instance of {@link CardHouseWithGUI} is
 * created and rendered.
 */
public class CardHouseGameMenu extends ApplicationAdapter {
        // Settings for the game, gui and menu.
        private CardHouseDef cardHouseDef = new CardHouseDef();

        private boolean debug = false;

        private Skin skin;
        private Stage stage;

        private Preferences preferences; // for remembering stuff

        private InputMultiplexer inputMultiplexer;

        private Background background; // draws background for menu and game
        private CardHouseWithGUI cardHouseWithGUI;
        private Tips tips; // shared with the gui

        private Runnable onResize; // used to lay out menus after resize

        private Image circle;
        private InputProcessor touchListener;
        private boolean drawTouch = false;

        /**
         * Menu and background for the card house game. When a new game is started
         * the menu is hidden and a new instance of {@link CardHouseWithGUI} is
         * created and rendered.
         */
        public CardHouseGameMenu() {
                if (debug) Gdx.app.setLogLevel(Application.LOG_DEBUG);
        }

        private void debug(String text) {
                if (debug) Gdx.app.log("CardHouseGameMenu", text);
        }

        @Override
        public void create() {
                if (debug) debug("Creating independent application.");

                Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

                // ui stuff
                ScreenViewport sv = new ScreenViewport();
                float ui_scale = (float) Math.abs(Math.log(Gdx.graphics.getDensity())) * 2.4f;
                sv.setUnitsPerPixel(1f / ui_scale);

                // extended class to set colors of buttons
                this.stage = new Stage(sv) {
                        SceneUtil.TraverseTask traverseTask = new SceneUtil.TraverseTask() {

                                @Override
                                public boolean run(Actor actor) {
                                        if (actor instanceof TextButton)
                                                actor.setColor(cardHouseDef.buttonColor);

                                        return true; // continue traversing
                                }
                        };

                        @Override
                        public void addActor(Actor actor) {
                                super.addActor(actor);
                                traverseChildren(actor, traverseTask);
                        }
                };

                skin = new Skin(Gdx.files.internal("ui/custom/custom.json"));


                // input
                inputMultiplexer = new InputMultiplexer();
                Gdx.input.setInputProcessor(inputMultiplexer);
                inputMultiplexer.addProcessor(stage);

                // background
                background = new Background();
                background.setColor(cardHouseDef.backgroundColor);
                background.setMatchingBackground(cardHouseDef.backgroundTexture);


                tips = new Tips("CardHouseGameMenu", stage, skin);
                preferences = Gdx.app.getPreferences("CardHouseGameMenu");

                showMainMenu();

                //setDrawTouch(true);

        }

        /** Whether to draw the touches. For debugging. */
        public void setDrawTouch(boolean drawTouch) {
                this.drawTouch = drawTouch;

                // remove old stuff
                inputMultiplexer.removeProcessor(touchListener);
                stage.getActors().removeValue(circle, true);


                if (drawTouch) {
                        // prepare image
                        TextureRegion textureRegion = skin.getRegion("circle");
                        circle = new Image(textureRegion);
                        circle.setTouchable(Touchable.disabled);

                        // listener for moving the circle
                        touchListener = new InputAdapter() {
                                Vector2 touch = new Vector2();
                                Vector2 tmp = new Vector2();

                                void updateSizeAndPos(int screenX, int screenY) {
                                        touch = stage.screenToStageCoordinates(tmp.set(screenX, screenY));

                                        CardHouse cardHouse = null;
                                        if (cardHouseWithGUI != null) cardHouse = cardHouseWithGUI.getCardHouse();

                                        float worldZoom = 1;
                                        if (cardHouse != null)
                                                worldZoom = cardHouseWithGUI.getCardHouse().getCamera().zoom;

                                        float touchRadius = Util.getTouchRadius(worldZoom) * 100;
                                        circle.setSize(touchRadius * 2, touchRadius * 2);
                                        circle.setPosition(touch.x, touch.y, Align.center);
                                }

                                @Override
                                public boolean touchDown(int screenX, int screenY, int pointer, int button) {

                                        circle.setVisible(true);
                                        if (!stage.getActors().contains(circle, true))
                                                stage.addActor(circle);

                                        updateSizeAndPos(screenX, screenY);
                                        return false;
                                }

                                @Override
                                public boolean touchDragged(int screenX, int screenY, int pointer) {
                                        circle.setVisible(true);
                                        updateSizeAndPos(screenX, screenY);
                                        return false;
                                }

                                @Override
                                public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                                        circle.setVisible(false);
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

        /**
         * Hides the menu and creates a new {@link CardHouseWithGUI} that receives input
         * and is drawn.
         *
         * @param asJson jSon representation of a {@link org.ams.physics.world.def.BoxWorldDef}.
         *               Contains only cards, not the ground.
         */

        private void startGame(String asJson) {
                if (debug) debug("Starting game.");

                if (cardHouseWithGUI != null) cardHouseWithGUI.dispose();

                // prepare definition
                cardHouseDef.asJson = asJson;
                cardHouseDef.unit = preferences.getInteger("Unit", Locale.getDefault() == Locale.US ? 1 : 0);
                cardHouseDef.angleRounding = preferences.getInteger("AngleRounding", 5);


                // create gui
                cardHouseWithGUI = new CardHouseWithGUI();
                cardHouseWithGUI.create(stage, skin, cardHouseDef, inputMultiplexer, tips);
                cardHouseWithGUI.startGame(new Runnable() {
                        @Override
                        public void run() {
                                showMainMenu();
                        }
                });


                onResize = null;

        }

        private boolean canResumeGame() {
                return cardHouseWithGUI != null;
        }

        private void resumeGame() {
                if (debug) debug("Resuming game.");

                cardHouseWithGUI.resumeGame(new Runnable() {
                        @Override
                        public void run() {
                                showMainMenu();
                        }
                });
                onResize = null;
        }


        /** Clear other menus. Show main menu. */
        private void showMainMenu() {
                if (debug) debug("Showing main menu.");
                onResize = new Runnable() {
                        @Override
                        public void run() {
                                showMainMenu();
                        }
                };


                Table table = createMainMenuComponents();
                ScrollPane scrollPane = new ScrollPane(table);

                stage.clear();
                stage.addActor(scrollPane);
                fillAndCenter(stage, scrollPane);
        }

        /** Create buttons for main menu and put them in a table. */
        private Table createMainMenuComponents() {
                if (debug) debug("Creating main menu components.");
                Table buttonTable = new Table();


                float buttonWidth = getPreferredButtonWidth(stage);

                if (canResumeGame()) {
                        TextButton resumeButton = new TextButton("Resume", skin);
                        buttonTable.add(resumeButton).width(buttonWidth).pad(getPreferredPadding(stage)).row();
                        resumeButton.addListener(new ClickListener() {
                                @Override
                                public void clicked(InputEvent event, float x, float y) {

                                        resumeGame();

                                }
                        });
                }


                TextButton playButton = new TextButton("New Game", skin);
                buttonTable.add(playButton).width(buttonWidth).pad(getPreferredPadding(stage)).row();
                playButton.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                                startGame(null);
                        }
                });


                if (canResumeGame()) {
                        TextButton saveButton = new TextButton("Save Game", skin);
                        buttonTable.add(saveButton).width(buttonWidth).pad(getPreferredPadding(stage)).row();
                        saveButton.addListener(new ClickListener() {
                                @Override
                                public void clicked(InputEvent event, float x, float y) {
                                        showSaveGameMenu();

                                }
                        });
                }

                TextButton loadButton = new TextButton("Load Game", skin);
                buttonTable.add(loadButton).width(buttonWidth).pad(getPreferredPadding(stage)).row();
                loadButton.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                                showLoadGameMenu();
                        }
                });


                TextButton settingsButton = new TextButton("Settings", skin);
                buttonTable.add(settingsButton).width(buttonWidth).pad(getPreferredPadding(stage)).row();
                settingsButton.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                                showSettingsMenu();
                        }
                });


                TextButton exitButton = new TextButton("Exit", skin);
                buttonTable.add(exitButton).width(buttonWidth).pad(getPreferredPadding(stage)).row();
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


        /** Clear other menus. Show load menu. */
        private void showLoadGameMenu() {
                if (debug) debug("Showing load game menu.");
                onResize = new Runnable() {
                        @Override
                        public void run() {
                                showLoadGameMenu();
                        }
                };


                Table table = createLoadMenuComponents();

                stage.clear();
                stage.addActor(table);
                fillAndCenter(stage, table);
        }

        /** Create a list with saved games and some buttons and put it in a table. */
        private Table createLoadMenuComponents() {
                Array<String> savedGames = getSavedGames(true);

                final List<String> savedGamesList = createSavedGamesList(savedGames);


                Table buttonTable = new Table();
                float buttonWidth = getPreferredButtonWidth(stage);


                // Buttons
                TextButton loadGameButton = new TextButton("Load", skin);
                buttonTable.add(loadGameButton).width(buttonWidth)
                        .pad(getPreferredPadding(stage)).padTop(getPreferredPadding(stage) * 6).row();
                loadGameButton.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {

                                FileHandle file = Gdx.files.external("CardHouse Saved Games/" + savedGamesList.getSelected() + ".json");

                                if (!file.exists())
                                        file = Gdx.files.internal("Example saves/" + savedGamesList.getSelected() + ".json");

                                if (file.exists()) {
                                        startGame(file.readString());
                                }
                        }
                });

                TextButton deleteSaveButton = new TextButton("Delete", skin);
                buttonTable.add(deleteSaveButton).width(buttonWidth)
                        .pad(getPreferredPadding(stage)).row();
                deleteSaveButton.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                                final String save = savedGamesList.getSelected();

                                SceneUtil.confirm(stage, skin, "Delete " + save + "?", "Yes", "No", new SceneUtil.ConfirmCallback() {
                                        @Override
                                        public void confirm(boolean confirmed) {
                                                if (confirmed) {
                                                        Gdx.files.external("CardHouse Saved Games/" + save + ".json").delete();
                                                }
                                                showLoadGameMenu();
                                        }
                                });
                        }
                });

                TextButton backButton = new TextButton("Back", skin);
                buttonTable.add(backButton).width(buttonWidth).pad(getPreferredPadding(stage)).row();
                backButton.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                                showMainMenu();
                        }
                });


                Table table = new Table();
                table.add(new ScrollPane(savedGamesList)).pad(getPreferredPadding(stage)).row();
                table.add(buttonTable);
                return table;

        }


        /** Clear other menus. Show save menu. */
        private void showSaveGameMenu() {
                if (debug) debug("Showing save game menu.");
                onResize = new Runnable() {
                        @Override
                        public void run() {
                                showSaveGameMenu();
                        }
                };


                Table table = createSaveMenuComponents();

                table.padBottom(stage.getHeight() * 0.5f);
                ScrollPane scrollPane = new ScrollPane(table);

                stage.clear();
                stage.addActor(scrollPane);
                fillAndCenter(stage, scrollPane);

                // set keyboard focus on textfield
                traverseChildren(table, new SceneUtil.TraverseTask() {
                        @Override
                        public boolean run(Actor actor) {
                                if (actor instanceof TextField) {
                                        stage.setKeyboardFocus(actor);
                                        return false; // stop searching
                                }
                                return true; // continue searching
                        }
                });
        }

        /** Create a list with saved games, a TextField and some buttons and put it in a table. */
        private Table createSaveMenuComponents() {
                Array<String> savedGames = getSavedGames(false);

                final List<String> savedGamesList = createSavedGamesList(savedGames);


                Table buttonTable = new Table();
                float buttonWidth = getPreferredButtonWidth(stage);

                // TextField
                final TextField textField = new TextField(savedGamesList.getSelected(), skin);
                textField.getStyle().fontColor = Color.BLACK;
                textField.getStyle().focusedFontColor = Color.BLACK;
                textField.setColor(Color.BLACK);

                buttonTable.add(textField).width(buttonWidth).row();


                textField.setSelection(0, textField.getText().length());
                savedGamesList.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                                textField.setText(savedGamesList.getSelected());
                                textField.setSelection(0, textField.getText().length());
                        }
                });


                // Buttons
                TextButton saveGameButton = new TextButton("Save", skin);
                buttonTable.add(saveGameButton).width(buttonWidth)
                        .pad(getPreferredPadding(stage)).padTop(getPreferredPadding(stage) * 6).row();
                saveGameButton.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                                final String asJson = cardHouseWithGUI.saveGame();

                                final FileHandle fileHandle = Gdx.files.external("CardHouse Saved Games/" + textField.getText() + ".json");

                                if (fileHandle.exists()) {
                                        SceneUtil.confirm(stage, skin, "Overwrite?", "Yes", "No", new SceneUtil.ConfirmCallback() {
                                                @Override
                                                public void confirm(boolean confirmed) {
                                                        if (confirmed) {
                                                                fileHandle.writeString(asJson, false);
                                                                showMainMenu();
                                                        }
                                                }
                                        });
                                } else {
                                        fileHandle.writeString(asJson, false);
                                        showMainMenu();
                                }

                                textField.getOnscreenKeyboard().show(false);


                        }
                });


                TextButton deleteSaveButton = new TextButton("Delete", skin);
                buttonTable.add(deleteSaveButton).width(buttonWidth)
                        .pad(getPreferredPadding(stage)).row();
                deleteSaveButton.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                                final String save = savedGamesList.getSelected();

                                SceneUtil.confirm(stage, skin, "Delete " + save + "?", "Yes", "No", new SceneUtil.ConfirmCallback() {
                                        @Override
                                        public void confirm(boolean confirmed) {
                                                if (confirmed) {
                                                        Gdx.files.external("CardHouse Saved Games/" + save + ".json").delete();
                                                }
                                                showSaveGameMenu();
                                        }
                                });


                                textField.getOnscreenKeyboard().show(false);
                        }
                });

                TextButton backButton = new TextButton("Back", skin);
                buttonTable.add(backButton).width(buttonWidth).pad(getPreferredPadding(stage)).row();
                backButton.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                                showMainMenu();
                                textField.getOnscreenKeyboard().show(false);
                        }
                });


                Table table = new Table();
                table.add(savedGamesList).pad(getPreferredPadding(stage)).row();
                table.add(buttonTable);
                return table;

        }

        /**
         * Create a list that contains currently saved games.
         * It remembers the last selected item for next session.
         */
        private List<String> createSavedGamesList(Array<String> savedGames) {

                // load items and skin
                final List<String> savedGamesList = new List<String>(skin);
                savedGamesList.setItems(savedGames);
                savedGamesList.getStyle().fontColorSelected = Color.BLACK;
                savedGamesList.getStyle().fontColorUnselected = Color.BLACK;

                // remember last selected item
                if (savedGamesList.getItems().size > 0)
                        savedGamesList.setSelectedIndex(MathUtils.clamp(0, savedGamesList.getItems().size - 1, preferences.getInteger("SelectedSave")));

                // setup so it will remember last item to next time
                savedGamesList.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                                preferences.putInteger("SelectedSave", savedGamesList.getSelectedIndex());
                                preferences.flush();
                        }
                });

                return savedGamesList;

        }

        /**
         * Create an array of filenames of saved games. Two folders are
         * loaded: one example folder and a folder of user saves.
         */
        private Array<String> getSavedGames(boolean includeExamples) {
                Array<String> savedGames = new Array<String>();

                if (includeExamples) { // examples
                        FileHandle folder = Gdx.files.internal("Example saves");
                        FileHandle[] files = folder.list(new FileFilter() {
                                @Override
                                public boolean accept(File file) {
                                        return file.getName().endsWith(".json");
                                }
                        });
                        for (FileHandle fileHandle : files) {
                                savedGames.add(fileHandle.nameWithoutExtension());
                        }
                }
                { // user saves
                        FileHandle folder = Gdx.files.external("CardHouse Saved Games");
                        FileHandle[] files = folder.list(new FileFilter() {
                                @Override
                                public boolean accept(File file) {
                                        return file.getName().endsWith(".json");
                                }
                        });
                        for (FileHandle fileHandle : files) {
                                String name = fileHandle.nameWithoutExtension();
                                if (!savedGames.contains(name, false))
                                        savedGames.add(name);
                        }
                }


                return savedGames;
        }

        /** Clear other menus. Show settings. */
        private void showSettingsMenu() {
                if (debug) debug("Showing settings menu.");
                onResize = new Runnable() {
                        @Override
                        public void run() {
                                showSettingsMenu();
                        }
                };

                Table table = createSettingsComponents();

                ScrollPane scrollPane = new ScrollPane(table);
                table.padBottom(stage.getHeight() * 0.5f);

                stage.clear();
                stage.addActor(scrollPane);
                fillAndCenter(stage, scrollPane);
        }

        /**
         * Create a bunch of setting controls and put them in a table. New settings
         * are only applied when the ok button is pressed.
         */
        private Table createSettingsComponents() {
                if (debug) debug("Creating settings menu components.");

                Table buttonTable = new Table();

                float buttonWidth = getPreferredButtonWidth(stage);

                final Array<Runnable> onOk = new Array<Runnable>();

                // reset tips
                final CheckBox resetTipsCheckBox = new CheckBox("Reset tips", skin);
                resetTipsCheckBox.getLabel().setColor(Color.BLACK);
                buttonTable.add(resetTipsCheckBox).width(buttonWidth).pad(getPreferredPadding(stage)).row();
                onOk.add(new Runnable() {
                        @Override
                        public void run() {
                                if (resetTipsCheckBox.isChecked())
                                        tips.reset();
                        }
                });


                { // select unit
                        Table localTable = new Table();

                        Label label = new Label("Unit:", skin);
                        label.setColor(Color.BLACK);
                        localTable.add(label).pad(getPreferredPadding(stage));

                        final SelectBox<String> selectBox = new SelectBox<String>(skin);
                        selectBox.setItems(cardHouseDef.houseHeightUnits);
                        selectBox.setSelectedIndex(preferences.getInteger("Unit"));
                        selectBox.getStyle().fontColor = Color.BLACK;

                        localTable.add(selectBox).pad(getPreferredPadding(stage)).row();
                        onOk.add(new Runnable() {
                                @Override
                                public void run() {
                                        preferences.putInteger("Unit", selectBox.getSelectedIndex());
                                        preferences.flush();
                                }
                        });
                        buttonTable.add(localTable).row();
                }


                { // select angle rounding
                        Table localTable = new Table();

                        Label label = new Label("Angle rounding:", skin);
                        label.setColor(Color.BLACK);
                        localTable.add(label).pad(getPreferredPadding(stage));

                        final SelectBox<String> selectBox = new SelectBox<String>(skin);

                        Array<String> items = new Array<String>();
                        for (int i = 1; i <= 36; i++) {
                                items.add(String.valueOf(i));
                        }

                        selectBox.setItems(items);
                        selectBox.setSelected(String.valueOf(preferences.getInteger("AngleRounding", 5)));
                        selectBox.getStyle().fontColor = Color.BLACK;

                        localTable.add(selectBox).pad(getPreferredPadding(stage)).row();
                        onOk.add(new Runnable() {
                                @Override
                                public void run() {
                                        preferences.putInteger("AngleRounding",
                                                Integer.valueOf(selectBox.getSelected()));
                                        preferences.flush();
                                }
                        });
                        buttonTable.add(localTable).row();
                }


                // buttons
                TextButton okButton = new TextButton("Ok", skin);
                buttonTable.add(okButton).width(buttonWidth).pad(getPreferredPadding(stage)).padTop(getPreferredPadding(stage) * 6).row();
                okButton.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                                // apply new settings
                                for (Runnable r : onOk) r.run();
                                showMainMenu();
                        }
                });

                TextButton cancelButton = new TextButton("Cancel", skin);
                buttonTable.add(cancelButton).width(buttonWidth).pad(getPreferredPadding(stage)).row();
                cancelButton.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                                showMainMenu();
                        }
                });


                Table mainMenu = new Table();
                mainMenu.add(buttonTable);
                return mainMenu;

        }


        @Override
        public void render() {
                Gdx.gl20.glClearColor(1, 1, 1, 1);
                Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);

                background.render();

                if (cardHouseWithGUI != null && !cardHouseWithGUI.isPaused()) cardHouseWithGUI.render();

                stage.act();
                stage.draw();

        }

        /**
         * Dispose all resources and nullify references.
         * Must be called when this object is no longer used.
         */
        @Override
        public void dispose() {
                if (debug) debug("Disposing resources...");

                if (skin != null) skin.dispose();
                skin = null;

                if (stage != null) stage.dispose();
                stage = null;

                if (background != null) background.dispose();
                background = null;


                if (cardHouseWithGUI != null) cardHouseWithGUI.dispose();
                cardHouseWithGUI = null;

                if (tips != null) tips.dispose();
                tips = null;

                preferences = null;


                if (debug) debug("Finished disposing resources.");
        }

        @Override
        public void resize(int width, int height) {
                if (debug) debug("Resizing, width=" + width + ", height = " + height + ".");

                stage.getViewport().update(width, height, true);


                if (cardHouseWithGUI != null) cardHouseWithGUI.resize(width, height);

                if (onResize != null) onResize.run();

                if (background != null) background.resize(width, height);

                if (tips != null) tips.resize(width, height);
        }


}
