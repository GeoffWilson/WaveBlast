package co.piglet.waveblast;

import uk.co.kernite.VGM.VGMPlayer;

public class Level
{
    public Level()
    {
        Thread t = new Thread(new BackgroundMusic());
        t.start();
    }

    private class BackgroundMusic implements Runnable
    {
        @Override
        public void run()
        {
            // Set background music
            VGMPlayer audio = new VGMPlayer(22050);
            try
            {
                audio.loadFile("audio/background.vgz");
                audio.setVolume(0.35d);
                audio.startTrack(1, 600);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
}
