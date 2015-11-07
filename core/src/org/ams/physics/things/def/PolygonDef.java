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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ams.physics.things.def;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 * The main purpose of these definitions is to make it easy so save and load things.
 *
 * @author Andreas
 */
public class PolygonDef extends ThingWithBodyDef {

        public Array<Vector2> vertices;

        public PolygonDef() {
                super();
        }

        public PolygonDef(PolygonDef toCopy) {
                super(toCopy);
                vertices = toCopy.getCopyOfVertices();
        }

        /** A copy is returned. */
        public Array<Vector2> getCopyOfVertices() {
                int n = vertices == null ? 0 : vertices.size;

                Array<Vector2> copy = new Array<Vector2>(true, n, Vector2.class);

                for (int i = 0; i < n; i++) {
                        copy.add(new Vector2(vertices.items[i]));
                }

                return copy;
        }

        /** Copies the given instance. */
        public void setVertices(Array<Vector2> vertices) {
                this.vertices = new Array<Vector2>(true, vertices.size, Vector2.class);
                for (Vector2 v : vertices) {
                        this.vertices.add(new Vector2((v)));
                }
        }

        @Override
        public ThingDef getCopy() {
                return new PolygonDef(this);
        }
}
