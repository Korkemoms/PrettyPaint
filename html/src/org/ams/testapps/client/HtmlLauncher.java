package org.ams.testapps.client;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import org.ams.testapps.paintandphysics.FallingBoxes;
import org.ams.testapps.paintandphysics.physicspuzzle.PhysicsPuzzle;
import org.ams.testapps.paintandphysics.physicspuzzle.PhysicsPuzzleGameMenu;
import org.ams.testapps.prettypaint.CircleAndBackground;
import org.ams.testapps.prettypaint.JaggedPolygon;
import org.ams.testapps.prettypaint.TextureAlignmentTest;

public class HtmlLauncher extends GwtApplication {

        @Override
        public GwtApplicationConfiguration getConfig () {
                return new GwtApplicationConfiguration(1280, 720);
        }

        @Override
        public ApplicationListener getApplicationListener () {
                return new PhysicsPuzzleGameMenu();
        }
}