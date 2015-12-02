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


import com.badlogic.gdx.graphics.Color;

/** Some settings are gathered here for easy access. */
public class CardHouseDef {

        /**
         * jSon representation of a {@link org.ams.physics.world.def.BoxWorldDef}.
         * Contains only cards, not the ground.
         */
        public String asJson;

        /** Will mess up previously saved games if changed. */
        public float cardHeight = 2f;

        /** Will mess up previously saved games if changed. */
        public float cardWidth = 0.024f;


        /** Unit is card height. */
        public int groundWidth = 40;

        /** Friction for the ground and cards. Higher friction for easier game. */
        public float friction = 0.85f;

        public float groundY = -2f;

        public float[] houseHeightUnitMultipliers = new float[]{8.89f, 0.291666f};
        public String[] houseHeightUnits = new String[]{"cm", "ft"};
        public int[] decimals = new int[]{1, 2};

        public int unit = 1;


        public float angleRounding = 5;

        // style stuff

        /**
         * The colors of the cards that are held by the card mover. To change the number of cheat cards
         * just add or remove some colors here.
         */
        public Color[] cheatColors = new Color[]{
                Color.FOREST,
                Color.YELLOW,
                Color.RED};


        public Color turnCircleColor = getColor(231, 130, 182);

        public Color cardColor = getColor(0, 0, 0);

        public Color refillCardColor = getColor(231, 130, 182);

        public Color heightLabelColor = getColor(0, 0, 0);

        public Color buttonColor = getColor(113, 186, 132);

        public Color backgroundColor = getColor(239, 231, 219);

        public Color groundColor = getColor(178, 195, 153);


        public String groundTexture = "images/backgrounds-light/green_cup.png";
        public String backgroundTexture = "images/backgrounds-light/diamond_upholstery_2X.png";


        private Color getColor(int r, int g, int b) {
                return new Color(r / 255f, g / 255f, b / 255f, 1);
        }

}
