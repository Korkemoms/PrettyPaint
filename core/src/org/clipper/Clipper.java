/**
 * Boost Software License - Version 1.0 - August 17th, 2003
 * <p>
 * Permission is hereby granted, free of charge, to any person or organization
 * obtaining a copy of the software and accompanying documentation covered by
 * this license (the "Software") to use, reproduce, display, distribute,
 * execute, and transmit the Software, and to prepare derivative works of the
 * Software, and to permit third-parties to whom the Software is furnished to
 * do so, all subject to the following:
 * <p>
 * The copyright notices in the Software and this entire statement, including
 * the above license grant, this restriction and the following disclaimer,
 * must be included in all copies of the Software, in whole or in part, and
 * all derivative works of the Software, unless such copies or derivative
 * works are solely in the form of machine-executable object code generated by
 * a source language processor.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, TITLE AND NON-INFRINGEMENT. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDERS OR ANYONE DISTRIBUTING THE SOFTWARE BE LIABLE
 * FOR ANY DAMAGES OR OTHER LIABILITY, WHETHER IN CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */
package org.clipper;

import org.clipper.Point.LongPoint;

public interface Clipper {
    public enum ClipType {
        INTERSECTION, UNION, DIFFERENCE, XOR
    }

    enum Direction {
        RIGHT_TO_LEFT, LEFT_TO_RIGHT
    };

    public enum EndType {
        CLOSED_POLYGON, CLOSED_LINE, OPEN_BUTT, OPEN_SQUARE, OPEN_ROUND
    };

    public enum JoinType {
        SQUARE, ROUND, MITER
    };

    public enum PolyFillType {
        EVEN_ODD, NON_ZERO, POSITIVE, NEGATIVE
    };

    public enum PolyType {
        SUBJECT, CLIP
    };

    public interface ZFillCallback {
        void zFill( LongPoint bot1, LongPoint top1, LongPoint bot2, LongPoint top2, LongPoint pt );
    };

    //InitOptions that can be passed to the constructor ...
    public final static int REVERSE_SOLUTION = 1;

    public final static int STRICTLY_SIMPLE = 2;

    public final static int PRESERVE_COLINEAR = 4;

    boolean addPath( Path pg, PolyType polyType, boolean Closed );

    boolean addPaths( Paths ppg, PolyType polyType, boolean closed );

    void clear();

    boolean execute( ClipType clipType, Paths solution );

    boolean execute( ClipType clipType, Paths solution, PolyFillType subjFillType, PolyFillType clipFillType );

    boolean execute( ClipType clipType, PolyTree polytree );

    public boolean execute( ClipType clipType, PolyTree polytree, PolyFillType subjFillType, PolyFillType clipFillType );
}
