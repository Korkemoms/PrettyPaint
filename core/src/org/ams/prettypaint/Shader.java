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

package org.ams.prettypaint;

import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;

/**
 * Shader used by {@link PrettyPolygonBatch} to render both textures and outlines. It uses branching in order
 * to render textures on top of outlines and the other way around.
 */
class Shader {

        protected static class Attribute {

                protected String alias;
                protected int numComponents;
                protected VertexAttribute vertexAttribute;

                protected Attribute(String alias, int numComponents, int usage) {
                        this.alias = alias;
                        this.numComponents = numComponents;
                        vertexAttribute = new VertexAttribute(usage, numComponents, alias);
                }

                @Override
                public String toString() {
                        return alias;
                }

                /**
                 * Position of the vertex.
                 */
                public static Attribute position = new Attribute("a_position", 2, VertexAttributes.Usage.Position);

                /**
                 * For textures only the alpha value is used. For outlines the whole color is used.
                 */
                public static Attribute colorOrJustOpacity = new Attribute("a_color", 4, VertexAttributes.Usage.ColorPacked);

                /**
                 * For outlines: Ignored :(
                 * For textures: The origin such that when the {@link #position} given is (0,0) then the pixel
                 * drawn is the pixel found at {@link #originInTexture} in the texture. Alpha values.
                 */
                public static Attribute originInTexture = new Attribute("a_texture_origin", 2, VertexAttributes.Usage.TextureCoordinates);

                /**
                 * The outlines: 1st value is boldness.
                 * For textures: The position of the texture in the texture region. Alpha values between 0 and 1.
                 */
                public static Attribute sourcePositionOrBoldness = new Attribute("a_texture_position_or_boldness", 2, VertexAttributes.Usage.TextureCoordinates);

                /**
                 * If < -0.5: Outline mode.
                 * If >= -0.5: Texture mode.
                 * For outlines: Not used
                 * For textures: The size of the texture relative to the texture region. Alpha value between 0 and 1.
                 */
                public static Attribute textureSizeAndShaderChooser = new Attribute("a_texture_size", 1, VertexAttributes.Usage.TextureCoordinates);


        }

        protected static final String outlineVertexBranch
                = "v_color      = " + Attribute.colorOrJustOpacity + "             ;\n"
                + "float alpha  = " + Attribute.colorOrJustOpacity + "[3]          ;\n"
                + "float weight = " + Attribute.sourcePositionOrBoldness + ".x     ;\n"

                + "v_color[3]   =  weight*alpha*alpha                              ;\n"

                + "gl_Position  = u_worldView*" + Attribute.position + "           ;\n";

        protected static final String outlineFragmentBranch
                = "gl_FragColor = v_color                                          ;\n";


        protected static final String textureVertexBranch
                = "v_pos       = " + Attribute.sourcePositionOrBoldness + "        ;\n"
                + "v_size      = " + Attribute.textureSizeAndShaderChooser + "     ;\n"
                + "v_color     = " + Attribute.colorOrJustOpacity + "              ;\n"
                //
                + "v_texCoord0 = " + Attribute.originInTexture + "                 ;\n"
                + "gl_Position = u_worldView * " + Attribute.position + "          ;\n";

        protected static final String textureFragmentBranch

                = "vec2 pos         = v_pos+mod(v_texCoord0,v_size)                ;\n"

                + "gl_FragColor     = texture2D(u_texture,pos)                     ;\n"
                + "gl_FragColor[3] *= v_color[3]                                   ;\n";

        protected static final String vertexShader
                = "uniform mat4 u_worldView;\n"
                // needed for both branches
                + "attribute vec4 " + Attribute.position + "                       ;\n"
                //
                // needed for outline branch
                + "attribute vec4 " + Attribute.colorOrJustOpacity + "             ;\n"
                //
                // needed for texture branch
                + "attribute vec2 " + Attribute.originInTexture + "                ;\n"
                + "attribute vec2 " + Attribute.sourcePositionOrBoldness + "       ;\n"
                + "attribute float " + Attribute.textureSizeAndShaderChooser + "   ;\n"
                //
                // needed for outline branch
                + "varying vec4 v_color                                            ;\n"
                //
                // needed for texture branch
                + "varying vec2 v_pos                                              ;\n"
                + "varying float v_size                                            ;\n"
                + "varying vec2 v_texCoord0                                        ;\n"
                //
                + "void main()                                                      \n"
                + "{                                                                \n"
                + "    v_size = " + Attribute.textureSizeAndShaderChooser + "      ;\n"
                + "    if(v_size<-0.5){" + outlineVertexBranch + " }                \n"
                + "    else           {" + textureVertexBranch + " }               ;\n"
                + "}";


        protected static final String fragmentShader
                = "precision lowp float                                            ;\n"
                // needed for texture branch
                + "uniform sampler2D u_texture                                     ;\n"
                //
                // needed for texture branch
                + "varying vec2 v_texCoord0                                        ;\n"
                + "varying vec2 v_pos                                              ;\n"
                + "varying float v_size                                            ;\n"
                //
                // needed for outline branch
                + "varying vec4 v_color                                            ;\n"
                //
                + "void main()                                                      \n"
                + "{                                                                \n"
                + "    if(v_size<-0.5){" + outlineFragmentBranch + " }              \n"
                + "    else           {" + textureFragmentBranch + " }              \n"
                + "}";
}
