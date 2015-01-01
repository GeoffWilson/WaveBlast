package co.piglet.waveblast;

import java.util.concurrent.ConcurrentHashMap;

public class SoundBank {
    private ConcurrentHashMap<String, SoundEffect> soundMap;

    public SoundBank() {
        soundMap = new ConcurrentHashMap<>();
    }

    public void addSound(String name, SoundEffect soundEffect) {
        if (!soundMap.containsKey(name)) {
            soundMap.put(name, soundEffect);
        }
    }

    public void playSound(String name, int duration) {
        if (soundMap.containsKey(name)) {
            soundMap.get(name).play(duration);
        }
    }

    public void stopSound(String name) {
        if (soundMap.containsKey(name)) {
            soundMap.get(name).stop();
        }
    }
}
