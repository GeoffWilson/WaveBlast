package co.piglet.waveblast;

import uk.co.kernite.VGM.VGMPlayer;

import java.util.Timer;
import java.util.TimerTask;

public class SoundEffect {
    private VGMPlayer audio;
    private Timer timer;

    public SoundEffect(String sound, double volume) {
        try {
            timer = new Timer();
            audio = new VGMPlayer(22050);
            audio.setVolume(volume);
            audio.loadFile(sound);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            audio.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void play(int duration) {
        timer.schedule(new PlaySound(duration), 0);
    }

    private class PlaySound extends TimerTask {
        private int duration;

        public PlaySound(int duration) {
            this.duration = duration;
        }

        @Override
        public void run() {
            try {
                audio.startTrack(1, duration);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
