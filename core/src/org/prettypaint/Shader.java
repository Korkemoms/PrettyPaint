package org.prettypaint;

import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;

/**
 * Shader used by {@link PolygonBatch} to render both textures and outlines. It uses branching in order
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
                 * For outlines: the color of the outline.
                 * For textures: 1st value is source scaling. 4th value is opacity.
                 */
                public static Attribute colorOrScale = new Attribute("a_color", 4, VertexAttributes.Usage.ColorPacked);

                /**
                 * For outlines: Ignored :(
                 * For textures: The translation of the source texture. Alpha values between 0 and 1.
                 */
                public static Attribute textureTranslation = new Attribute("a_texture_translation", 2, VertexAttributes.Usage.TextureCoordinates);

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
                = "v_color      = " + Attribute.colorOrScale + "                   ;\n"
                + "float alpha  = " + Attribute.colorOrScale + "[3]                ;\n"
                + "float weight = " + Attribute.sourcePositionOrBoldness + ".x     ;\n"

                + "v_color[3]   =  weight*alpha*alpha                              ;\n"

                + "gl_Position  = u_worldView*" + Attribute.position + "           ;\n";

        protected static final String outlineFragmentBranch
                = "gl_FragColor = v_color                                          ;\n";


        protected static final String textureVertexBranch
                = "v_pos       = " + Attribute.sourcePositionOrBoldness + "        ;\n"
                + "v_size      = " + Attribute.textureSizeAndShaderChooser + "     ;\n"
                + "v_color     = " + Attribute.colorOrScale + "                    ;\n"
                //
                + "v_texCoord0 = " + Attribute.textureTranslation + "              ;\n"
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
                + "attribute vec4 " + Attribute.colorOrScale + "                   ;\n"
                //
                // needed for texture branch
                + "attribute vec2 " + Attribute.textureTranslation + "             ;\n"
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
