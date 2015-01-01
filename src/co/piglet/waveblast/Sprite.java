package co.piglet.waveblast;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class Sprite {
    private HashMap<String, SpriteAnimation> animations;
    private SpriteAnimation activeAnimation;

    private String animationString;

    private Timer timer;

    public boolean isAlive = true;

    public int enemyType = 0;

    public int x;;
    public int y;

    public boolean rotated;

    public boolean isBounceBack;

    public Sprite(String sprite, ImageCache cache) {
        // Init Objects
        animations = new HashMap<>();

        x = 1;
        y = 1;

        // Loading the animations
        SpriteAnimation animation = new SpriteAnimation(sprite, cache);
        animations.put("east", animation);
        SpriteAnimation death = new SpriteAnimation("die", cache);
        animations.put("die", death);
        animations.put("east", animation);
    }

    public void loadAdditionalAnimations(String sprite, ImageCache cache) {
        SpriteAnimation shield = new SpriteAnimation(sprite, cache);
        animations.put(sprite, shield);
    }

    public void setActiveAnimation(String target, int interval) {
        if (!target.equals(animationString)) {
            if (activeAnimation != null) activeAnimation.stopAnimation();
            activeAnimation = animations.get(target);
            activeAnimation.animate(interval);
            animationString = target;
        }
    }

    public BufferedImage getFrame() {
        if (rotated) {
            return activeAnimation.getRotatedFrame();
        }
        return activeAnimation.getFrame();
    }

    public void terminate() {
        timer.cancel();
        activeAnimation.stopAnimation();
    }

    public void stopMove() {
        timer.cancel();
    }

    public void moveSprite(int incX, int incY, int interval) {
        timer = new Timer();
        timer.schedule(new MovementTimer(incX, incY), 0, interval);
    }

    private class MovementTimer extends TimerTask {
        private int incX;
        private int incY;

        public MovementTimer(int incX, int incY) {
            this.incX = incX;
            this.incY = incY;
        }

        @Override
        public void run() {
            x += incX;
            y += incY;
        }
    }
}


