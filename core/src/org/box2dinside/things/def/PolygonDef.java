/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.box2dinside.things.def;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 *
 * @author Andreas
 */
public class PolygonDef extends ThingWithBodyDef {

        public Array<Vector2> vertices;

        /** A copy is returned. */
        public Array<Vector2> getCopyOfVertices() {
                Array<Vector2> copy = new Array(true, this.vertices.size, Vector2.class);

                for(Vector2 v : this.vertices){
                        copy.add(new Vector2(v));
                }

                return copy;
        }

        /** Copies the given instance. */
        public void setVertices(Array<Vector2> vertices) {
                this.vertices = new Array(true, vertices.size, Vector2.class);
                for(Vector2 v : vertices){
                        this.vertices.add(new Vector2((v)));
                }
        }

}
