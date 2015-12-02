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
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import org.ams.core.CoordinateHelper;
import org.ams.core.SceneUtil;
import org.ams.core.Timer;
import org.ams.core.Util;
import org.ams.paintandphysics.things.PPPolygon;

/**
 * Gui for the card house game, it also runs the game itself.
 * It can be started as an independent application(for debugging) or
 * it can be run from another {@link ApplicationAdapter}.
 */
public class CardHouseWithGUI extends ApplicationAdapter {

        private boolean debug = false;
        private boolean independentApplication = false;
        private long renderCount = 0;

        private CardHouseDef cardHouseDef;
        private CardHouse cardHouse;

        private Stage stage;
        private Skin skin;

        private Tips tips;

        private Timer timer;

        // updates the angle buttons
        private Runnable angleUpdater;

        // updates the angle label in the center of the turn circle
        // depends on vales updated by the angleUpdater
        private Runnable angleUpdaterForCenter;

        // updates the label showing the height of the house
        private Runnable topUpdater;


        private Runnable guiResizeTask;


        private InputMultiplexer inputMultiplexer;


        /**
         * This value is updated by a {@link org.ams.testapps.paintandphysics.cardhouse.CardMover.CardMoverListener}.
         * It is used to position the angleLabel.
         */
        private Vector2 cardTurnCirclePosition = new Vector2();

        /**
         * When the timer runs the angleUpdater then this string has the text of the
         * angleButton for the last selected card. It is done this way so the
         * string has to be created one time instead of two times per render.
         */
        private String angleText;

        public CardHouseWithGUI() {
                if (debug) Gdx.app.setLogLevel(Application.LOG_DEBUG);
        }

        private void debug(String text) {
                if (debug) Gdx.app.log("CardHouseWithGUI", text);

        }

        public CardHouse getCardHouse() {
                return cardHouse;
        }

        /**
         * Dispose all resources and nullify references.
         * Must be called when this object is no longer used.
         */
        @Override
        public void dispose() {
                if (debug) debug("Disposing resources...");

                if (cardHouse != null) cardHouse.dispose();

                if (independentApplication) {
                        if (skin != null) skin.dispose();


                        if (inputMultiplexer != null) inputMultiplexer.removeProcessor(stage);


                        if (stage != null) stage.dispose();

                        if (tips != null) tips.dispose();

                }
                skin = null;
                inputMultiplexer = null;
                stage = null;
                tips = null;


                if (timer != null) timer.clear();
                timer = null;

                angleUpdater = null;
                angleUpdaterForCenter = null;

                topUpdater = null;

                guiResizeTask = null;


                if (debug) debug("Finished disposing resources.");
        }

        /**
         * If you want to run this instance from another
         * {@link ApplicationAdapter} use {@link #create(Stage, Skin, CardHouseDef, InputMultiplexer, Tips)} instead.
         */
        @Override
        public void create() {
                if (debug) debug("Creating independent application.");


                independentApplication = true;


                Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);


                // ui stuff
                ScreenViewport sv = new ScreenViewport();
                float ui_scale = (float) Math.abs(Math.log(Gdx.graphics.getDensity())) * 2f;
                sv.setUnitsPerPixel(1f / ui_scale);
                Stage stage = new Stage(sv);
                Skin skin = new Skin(Gdx.files.internal("ui/custom/custom.json"));

                // input
                InputMultiplexer inputMultiplexer = new InputMultiplexer();
                Gdx.input.setInputProcessor(inputMultiplexer);
                inputMultiplexer.addProcessor(stage);

                //
                create(stage, skin, new CardHouseDef(), inputMultiplexer, new Tips("CardHouseWithGUI", stage, skin));

                timer.runAfterNRender(new Runnable() {
                        @Override
                        public void run() {
                                startGame(null);
                        }
                }, 1);
        }

        /**
         * Use this method to create a new card house with gui from another {@link ApplicationAdapter}.
         *
         * @param stage            Stage to put gui actors in.
         * @param skin             Skin for the gui actors.
         * @param cardHouseDef     Settings and possibly some saved cards.
         * @param inputMultiplexer {@link InputProcessor}'s are added to this one.
         * @param tips             Use the same tips for several classes to avoid several appearing at the same time.
         */
        public void create(Stage stage, Skin skin, CardHouseDef cardHouseDef, InputMultiplexer inputMultiplexer, Tips tips) {
                if (debug) debug("Creating application.");

                this.cardHouseDef = cardHouseDef;

                this.tips = tips;
                this.stage = stage;
                this.skin = skin;

                this.inputMultiplexer = inputMultiplexer;

                timer = new Timer();
        }

        /**
         * Creates the label that shows the height of the card house.
         * Also prepares a {@link Runnable} that keeps the label updated.
         */
        private Label createTopLabel() {
                // create label
                final Label label = new Label("", skin);
                label.setStyle(new Label.LabelStyle(label.getStyle()));
                label.getStyle().fontColor = cardHouseDef.heightLabelColor;

                // prepare runnable
                timer.remove(topUpdater);
                topUpdater = new Runnable() {
                        Vector2 top = new Vector2();
                        Vector2 interpolatedTop = new Vector2(0, cardHouseDef.groundY);
                        Vector2 screenPos = new Vector2();

                        long counter = 0;
                        Body highestBody;

                        @Override
                        public void run() {

                                // find the highest body
                                if (counter % 10 == 0) // its not necessary to find the highest body each frame
                                        highestBody = cardHouse.getHighestCard();

                                // find the height of the highest body
                                if (highestBody != null)
                                        top = cardHouse.getHeightOfCard(highestBody, top);
                                else top.set(0, cardHouseDef.groundY);

                                // update the position of the label
                                interpolatedTop.lerp(top, 0.05f);
                                screenPos = CoordinateHelper.getScreenCoordinates(
                                        cardHouse.getCamera(), interpolatedTop.x,
                                        interpolatedTop.y, screenPos);
                                Vector2 stagePos = stage.screenToStageCoordinates(screenPos);
                                label.setPosition(stagePos.x, stage.getHeight() - stagePos.y + label.getPrefHeight());

                                // update the text of the label
                                if (counter++ % 10 == 0) { // its not necessary to update the text each frame
                                        if (top.y == cardHouseDef.groundY) {
                                                label.setText("");
                                        } else {
                                                // get height and convert to specified unit
                                                float height = top.y - cardHouseDef.groundY;
                                                height = height / cardHouseDef.cardHeight;
                                                int unit = cardHouseDef.unit;
                                                height *= cardHouseDef.houseHeightUnitMultipliers[unit];

                                                int decimalCount = cardHouseDef.decimals[unit];

                                                float n = 1 / (float) (Math.pow(10, decimalCount));
                                                height = Util.roundToNearestN(height, n);

                                                // prepare text
                                                String decimals = Util.getDecimals(height, decimalCount);
                                                int integer = (int) height;
                                                String text = integer + "." + decimals;
                                                text += cardHouseDef.houseHeightUnits[unit];

                                                label.setText(text);
                                        }
                                }
                        }
                };
                timer.runOnRender(topUpdater);

                return label;
        }

        /**
         * Creates the label that shows the angle of the card being moved by the {@link CardMover}.
         * Also prepares a {@link Runnable} that keeps the label updated.
         */
        private Label createAngleLabel() {
                // create label
                final Label label = new Label("", skin);
                label.setStyle(new Label.LabelStyle(label.getStyle()));
                label.getStyle().fontColor = cardHouseDef.turnCircleColor;

                // prepare Runnable
                timer.remove(angleUpdaterForCenter);
                angleUpdaterForCenter = new Runnable() {


                        @Override
                        public void run() {
                                // the world position is updated by the  angleUpdater

                                // convert world pos to stage pos
                                Vector2 screenPos = CoordinateHelper.getScreenCoordinates(
                                        cardHouse.getCamera(), cardTurnCirclePosition.x,
                                        cardTurnCirclePosition.y, new Vector2());
                                Vector2 stagePos = stage.screenToStageCoordinates(screenPos);

                                //
                                label.setPosition(stagePos.x, stage.getHeight() - stagePos.y);

                                if (renderCount % 5 == 0) {
                                        label.setText(angleText);
                                        label.setVisible(cardHouse.getCardMover().isTurnCircleVisible());
                                }
                        }
                };
                timer.runOnRender(angleUpdaterForCenter);

                return label;
        }

        /** Get a string that represents the current state of the game. */
        public String saveGame() {
                return cardHouse.saveGame();
        }

        /**
         * Call after {@link #create(Stage, Skin, CardHouseDef, InputMultiplexer, Tips)}
         * to start the game.
         * <p/>
         * This method is called on {@link #create()} when running this app independently.
         *
         * @param onPause something to do when the game is paused. For example
         *                show a menu.
         */
        public void startGame(final Runnable onPause) {
                if (debug) debug("Starting a new game.");

                if (cardHouse != null) cardHouse.dispose();


                cardHouse = new CardHouse();
                cardHouse.create(inputMultiplexer, tips, cardHouseDef);

                cardHouse.getCardMover().addCardListener(new CardMover.CardMoverListener() {

                        @Override
                        public void turnCirclePositionChanged(Vector2 pos) {
                                // needed for the label that shows the angle of the
                                // active card
                                cardTurnCirclePosition.set(pos);
                        }

                        @Override
                        public void selected(PPPolygon card) {
                                if (card == null) return;
                                // only one below is shown each time selected is called
                                showTipTouchInsideCircle();
                                showTipTurnCircle();
                                showTipReleaseCard();
                        }

                });

                showGUI(onPause);

                showTipNewCard();
        }

        private void showTipNewCard() {
                if (tips.isAnyTipVisible()) return;
                String key = "TipNewCardDone";

                tips.queueTip(key, "Touch here to\nget a new card", new Tips.ChangingVector() {
                        @Override
                        public Vector2 get() {
                                return new Vector2(stage.getWidth() * 0.5f, stage.getHeight() * 0.83f);
                        }
                });
        }

        private void showTipReleaseCard() {
                if (tips.isAnyTipVisible()) return;
                String key = "TipReleaseCard";

                tips.queueTip(key, "Touch here to\ndrop a card", new Tips.ChangingVector() {
                        @Override
                        public Vector2 get() {
                                float buttonWidth = new TextButton("360.0", skin).getPrefWidth();
                                float x = stage.getWidth() - buttonWidth - SceneUtil.getPreferredPadding(stage) * 2;
                                return new Vector2(x, stage.getHeight() * 0.9f);
                        }
                });
        }

        private void showTipTurnCircle() {
                if (tips.isAnyTipVisible()) return;
                final String key = "TipTurnCircle";

                tips.queueTip(key, "Touch on the circle\nto turn a card", null);
        }

        private void showTipTouchInsideCircle() {
                if (tips.isAnyTipVisible()) return;
                final String key = "TipTouchInsideCircle";

                tips.queueTip(key, "Touch inside the circle\nto move a card", null);
        }

        /**
         * Resume the game.
         *
         * @param onPause something to do when pausing the game, for example show a menu.
         */
        public void resumeGame(final Runnable onPause) {
                if (debug) debug("Resuming game.");

                cardHouse.setPaused(false);
                showGUI(onPause);

        }

        /** Pause the game. */
        public void pauseGame() {
                pauseGame(null);
        }

        private void pauseGame(final Runnable onPause) {
                if (debug) debug("Pausing game.");

                if (onPause != null) onPause.run();
                cardHouse.setPaused(true);
        }

        public boolean isPaused() {
                return cardHouse.isPaused();
        }

        /**
         * Show the gui, the stage is cleared first.
         *
         * @param onPause something to do when the men button is clicked. Probably show a menu.
         */
        private void showGUI(final Runnable onPause) {
                if (debug) debug("Showing GUI.");

                stage.clear();

                // prepare menu button
                final TextButton menuButton = new TextButton("Menu", skin);
                menuButton.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                                pauseGame(onPause);
                        }
                });

                final Table mainMenuTable = new Table();
                mainMenuTable.add(menuButton).pad(SceneUtil.getPreferredPadding(stage));
                stage.addActor(mainMenuTable);

                // prepare angle buttons
                final Table angleButtonsTable = createAngleButtons();
                stage.addActor(angleButtonsTable);
                // prepare angle label
                stage.addActor(createAngleLabel());
                // prepare height of house label
                stage.addActor(createTopLabel());


                // sets position of buttons on resize
                guiResizeTask = new Runnable() {
                        @Override
                        public void run() {
                                mainMenuTable.setPosition(
                                        mainMenuTable.getPrefWidth() * 0.5f,
                                        stage.getHeight() - mainMenuTable.getPrefHeight() * 0.5f);

                                angleButtonsTable.setPosition(
                                        stage.getWidth() - angleButtonsTable.getPrefWidth() * 0.5f,
                                        stage.getHeight() - angleButtonsTable.getPrefHeight() * 0.5f);


                        }
                };
                guiResizeTask.run();
        }


        @Override
        public void resize(int width, int height) {
                if (debug) debug("Resizing, width=" + width + ", height=" + height + ".");


                if (independentApplication) {
                        stage.getViewport().update(width, height, true);

                        if (tips != null) tips.resize(width, height);
                }
                if (cardHouse != null) cardHouse.resize(width, height);

                if (guiResizeTask != null) guiResizeTask.run();


        }

        @Override
        public void render() {
                if (independentApplication) {

                        Gdx.gl20.glClearColor(1, 1, 1, 1);
                        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
                }

                if (cardHouse != null) cardHouse.render();


                if (independentApplication) {

                        stage.act();
                        stage.draw();
                }

                tips.drawArrows();


                timer.step();
                renderCount++;
        }

        /**
         * Get first card with given color. Only works on the cards
         * returned by {@link CardHouse#getColoredCards()}.
         */
        private PPPolygon getCard(Color color) {
                Array<PPPolygon> cards = cardHouse.getColoredCards();
                PPPolygon card = null;

                for (PPPolygon card1 : cards) {
                        Color color1 = card1.getOutlinePolygons().first().getColor();
                        if (color1.equals(color)) {
                                card = card1;
                                break;
                        }
                }
                return card;
        }

        /**
         * Release first card with given color. Only works on the
         * cards returned by {@link CardHouse#getColoredCards()}.
         */
        private void releaseCard(Color color) {
                debug("Releasing card with color=" + color + ".");

                PPPolygon card = getCard(color);

                if (card == null) return;
                cardHouse.getCardMover().releaseCard(card);
        }

        private TextButton createAngleButton() {
                final TextButton textButton = new TextButton(null, skin);

                textButton.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                                releaseCard(textButton.getLabel().getColor());
                        }
                });
                return textButton;
        }

        /**
         * Creates one angle-button for each cheat color. These can be clicked
         * to release the card with the same color as the text of the button.
         * Also creates a {@link Runnable} that keeps the buttons updated.
         */
        private Table createAngleButtons() {
                final Table table = new Table();

                // create buttons
                float width = new TextButton("360.0", skin).getPrefWidth();
                final TextButton[] textButtons = new TextButton[cardHouseDef.cheatColors.length];
                for (int i = 0; i < textButtons.length; i++) {
                        final TextButton textButton = createAngleButton();
                        textButton.setVisible(false);
                        table.add(textButton).width(width).pad(SceneUtil.getPreferredPadding(stage)).row();
                        textButtons[i] = textButton;
                }

                // prepare updates
                timer.remove(angleUpdater);
                angleUpdater = new Runnable() {

                        @Override
                        public void run() {

                                if (renderCount % 5 != 0) return;

                                Array<PPPolygon> coloredCards = cardHouse.getColoredCards();

                                int i = 0;
                                for (TextButton textButton : textButtons) {

                                        // is there a colored card for this button?
                                        boolean gotCard = coloredCards.size > i;

                                        if (gotCard != textButton.isVisible())
                                                textButton.setVisible(gotCard);


                                        if (gotCard) {
                                                PPPolygon card = coloredCards.get(i);

                                                // compute an intuitive angle
                                                float angleRad = card.getPhysicsThing().getBody().getAngle();
                                                float angleDeg = angleRad * MathUtils.radiansToDegrees;
                                                angleDeg = Math.abs(angleDeg);
                                                angleDeg %= 180;
                                                if (angleDeg > 90) angleDeg = 180 - angleDeg;
                                                angleDeg = Util.roundToNearestN(angleDeg, 0.1f);


                                                // convert to text and update button
                                                String decimals = Util.getDecimals(angleDeg, 1);
                                                int noDecimals = (int) angleDeg;
                                                String angleText = noDecimals + "." + decimals;
                                                textButton.setText(angleText);


                                                // update the angleText for the angleLabel
                                                Color color = card.getOutlinePolygons().first().getColor();

                                                if (color == cardHouseDef.cheatColors[0])
                                                        CardHouseWithGUI.this.angleText = angleText;

                                                textButton.getLabel().setColor(color);

                                        }
                                        i++;
                                }
                        }
                };
                timer.runOnRender(angleUpdater);


                return table;
        }




}
