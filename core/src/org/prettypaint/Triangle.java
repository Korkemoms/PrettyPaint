package org.prettypaint;

import com.badlogic.gdx.math.Vector2;

/**
 * Created by Andreas on 15.10.2015.
 */
public class Triangle {

        // coordinates
        public final Vector2 a;
        public final Vector2 b;
        public final Vector2 c;


        public Triangle(Vector2 a, Vector2 b, Vector2 c) {
                this.a = a;
                this.b = b;
                this.c = c;
        }

}
