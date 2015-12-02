package org.ams.core;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;

/** Used for recording the game. */
public class ScreenShot implements Runnable
{
        private static int fileCounter = 0;
        private Pixmap pixmap;

        @Override
        public void run()
        {
                saveScreenshot();
        }

        public void prepare()
        {
                getScreenshot(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);
        }

        public void saveScreenshot()
        {
                FileHandle file = new FileHandle("/CardHouseRecording/shot_"+ String.format("%06d",  fileCounter++) + ".png");
                PixmapIO.writePNG(file, pixmap);
                pixmap.dispose();
        }

        public void getScreenshot(int x, int y, int w, int h, boolean flipY)
        {
                Gdx.gl.glPixelStorei(Gdx.gl.GL_PACK_ALIGNMENT, 1);
                pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);
                Gdx.gl.glReadPixels(x, y, w, h, Gdx.gl.GL_RGBA, Gdx.gl.GL_UNSIGNED_BYTE, pixmap.getPixels());
        }
}