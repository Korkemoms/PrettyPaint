package org.prettypaint;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/**
 * Created by Andreas on 20.10.2015.
 */
public class BoundingBox {
        /** The actual bounding rectangle */
        Rectangle rectangle;

        /** Index of the first vertex that this bounding rectangle include. */
        int begin;
        /** The number of vertices that this bounding rectangle include. */
        int count;

        /** Triangle strip for the insideStrip part */
        Array<Float> insideVertexData = new Array<Float>(true, 20, Float.class);

        /** Triangle strip for the outside part. */
        Array<Float> outsideVertexData = new Array<Float>(true, 20, Float.class);


        BoundingBox(Rectangle r, int begin) {
                this.rectangle = r;
                this.begin = begin;
        }

}
