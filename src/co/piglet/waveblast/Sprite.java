package co.piglet.waveblast;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Timer;

public class Sprite {

    private HashMap<String, SpriteAnimation> animations;
    private SpriteAnimation activeAnimation;

    private String animationString;

    public boolean isAlive = true;

    public EnemyTypes enemyType = EnemyTypes.STANDARD;

    public int x;
    public int incX;

    public int y;
    public int incY;

    public boolean rotated;
    public boolean tileable;

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
        activeAnimation.stopAnimation();
    }

    public void moveSprite(int incX, int incY) {
        this.incX = incX;
        this.incY = incY;
    }

    public void move() {
        if (isAlive) {
            x += incX;
            y += incY;
        }
    }

}


