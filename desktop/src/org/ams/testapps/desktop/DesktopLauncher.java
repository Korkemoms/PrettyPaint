package org.ams.testapps.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import org.ams.testapps.paintandphysics.cardhouse.CardHouseGameMenu;

public class DesktopLauncher {
        public static void main(String[] arg) {
                LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
                config.width = 1280;
                config.height = 720;


                packTextures();


                new LwjglApplication(new CardHouseGameMenu(), config);
        }

        public static void packTextures() {
                TexturePacker.Settings settings = new TexturePacker.Settings();


                settings.filterMag = Texture.TextureFilter.Linear;
                settings.filterMin = Texture.TextureFilter.Linear;

                settings.paddingX = 1;
                settings.paddingY = 1;
                //settings.format = Pixmap.Format.RGB888;
                //settings.outputFormat = "jpeg";


                settings.square = true;
                //settings.duplicatePadding = true;

                settings.maxWidth = 1024;
                settings.maxHeight = 1024;


                settings.combineSubdirectories = true;


                TexturePacker.process(settings, "ui\\custom\\dark-hdpi", "ui\\custom", "custom.atlas");
        }
}
