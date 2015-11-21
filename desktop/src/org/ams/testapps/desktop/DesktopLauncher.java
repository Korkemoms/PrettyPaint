package org.ams.testapps.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import org.ams.testapps.paintandphysics.FallingBoxes;
import org.ams.testapps.paintandphysics.physicspuzzle.PhysicsPuzzle;
import org.ams.testapps.paintandphysics.physicspuzzle.PhysicsPuzzleGameMenu;
import org.ams.testapps.prettypaint.CircleAndBackground;
import org.ams.testapps.prettypaint.JaggedPolygon;
import org.ams.testapps.prettypaint.TextureAlignmentTest;
import org.ams.testapps.prettypaint.TextureAlignmentTest2;

public class DesktopLauncher {
        public static void main(String[] arg) {
                LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
                config.width = 1280;
                config.height = 720;


                // packTextures();


                new LwjglApplication(new PhysicsPuzzleGameMenu(), config);
        }

        public static void packTextures() {
                TexturePacker.Settings settings = new TexturePacker.Settings();


                settings.filterMag = Texture.TextureFilter.Linear;
                settings.filterMin = Texture.TextureFilter.Linear;

                settings.paddingX = 1;
                settings.paddingY = 1;

                settings.square = true;
                //settings.duplicatePadding = true;

                settings.maxWidth = 2048;
                settings.maxHeight = 2048;


                settings.combineSubdirectories = true;


                TexturePacker.process(settings, "images/for packing", "images/packed", "packed.atlas");
        }
}
