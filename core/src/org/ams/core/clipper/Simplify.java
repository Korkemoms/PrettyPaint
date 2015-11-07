
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

package org.ams.core.clipper;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 *
 * @author Andreas
 */
public class Simplify {

        private static final double scale = 1000000;

        private static Point.LongPoint convertToLongPoint(Vector2 v) {
                return new Point.LongPoint((long) (v.x * scale), (long) (v.y * scale));
        }

        private static Vector2 convertToVector(Point.LongPoint lp) {
                return new Vector2((float) (lp.getX() / scale), (float) (lp.getY() / scale));
        }

        public static Array<Array<Vector2>> simplify(Array<Vector2> vertices) {
                Path path = new Path();
                for (Vector2 v : vertices) {
                        path.add(convertToLongPoint(v));
                }
                Paths paths = DefaultClipper.simplifyPolygon(path);

                Array<Array<Vector2>> vv = new Array();
                for (int i = 0; i < paths.size(); i++) {
                        Array<Vector2> v = new Array();

                        for (Point.LongPoint p : paths.get(i)) {
                                v.add(convertToVector(p));
                        }
                        vv.add(v);
                }
                return vv;
        }
}
