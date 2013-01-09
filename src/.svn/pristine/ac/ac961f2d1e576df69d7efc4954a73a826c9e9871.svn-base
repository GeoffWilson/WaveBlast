package co.piglet.waveblast;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class SpriteAnimation
{
    private ArrayList<BufferedImage> frames;
    private Timer timer;
    private int frameCount = 0;
    private double radians;
    private String animationName;

    public SpriteAnimation(String spriteName, ImageCache cache)
    {
        // This is where we load the sprite data in
        try
        {
            animationName = spriteName;
            frames = new ArrayList<>();
            switch (spriteName)
            {
                case "powerup":
                    for (int i = 1; i < 7; i++)
                    {
                        String frameName = "sprites/" + spriteName + "-" + i + ".png";
                        if (cache.contains(frameName)) frames.add(cache.getFromCache(frameName));
                        else
                        {
                            BufferedImage newFrame = ImageIO.read(new FileInputStream("sprites/" + spriteName + "-" + i + ".png"));
                            frames.add(newFrame);
                            cache.addToCache(frameName, newFrame);
                        }
                    }
                case "lazerstart":
                    for (int i = 1; i < 3; i++)
                    {
                        String frameName = "sprites/" + spriteName + "-" + i + ".png";
                        if (cache.contains(frameName)) frames.add(cache.getFromCache(frameName));
                        else
                        {
                            BufferedImage newFrame = ImageIO.read(new FileInputStream("sprites/" + spriteName + "-" + i + ".png"));
                            frames.add(newFrame);
                            cache.addToCache(frameName, newFrame);
                        }
                    }
                    break;
                case "lazer":
                    for (int i = 1; i < 21; i++)
                    {
                        String frameName = "sprites/" + spriteName + "-" + i + ".png";
                        if (cache.contains(frameName)) frames.add(cache.getFromCache(frameName));
                        else
                        {
                            BufferedImage newFrame = ImageIO.read(new FileInputStream("sprites/" + spriteName + "-" + i + ".png"));
                            frames.add(newFrame);
                            cache.addToCache(frameName, newFrame);
                        }
                    }
                    break;
                case "ship-shield":
                    for (int i = 1; i < 6; i++)
                    {
                        String frameName = "sprites/" + spriteName + "-" + i + ".png";
                        if (cache.contains(frameName)) frames.add(cache.getFromCache(frameName));
                        else
                        {
                            BufferedImage newFrame = ImageIO.read(new FileInputStream("sprites/" + spriteName + "-" + i + ".png"));
                            frames.add(newFrame);
                            cache.addToCache(frameName, newFrame);
                        }
                    }
                    for (int i = 5; i > 0; i--)
                    {
                        String frameName = "sprites/" + spriteName + "-" + i + ".png";
                        if (cache.contains(frameName)) frames.add(cache.getFromCache(frameName));
                        else
                        {
                            BufferedImage newFrame = ImageIO.read(new FileInputStream("sprites/" + spriteName + "-" + i + ".png"));
                            frames.add(newFrame);
                            cache.addToCache(frameName, newFrame);
                        }
                    }
                    break;
                case "die":
                    for (int i = 1; i < 12; i++)
                    {
                        String frameName = "sprites/" + spriteName + "-" + i + ".png";
                        if (cache.contains(frameName)) frames.add(cache.getFromCache(frameName));
                        else
                        {
                            BufferedImage newFrame = ImageIO.read(new FileInputStream("sprites/" + spriteName + "-" + i + ".png"));
                            frames.add(newFrame);
                            cache.addToCache(frameName, newFrame);
                        }
                    }
                    break;
                default:
                    if (cache.contains(spriteName)) frames.add(cache.getFromCache(spriteName));
                    else
                    {
                        BufferedImage newFrame = ImageIO.read(new FileInputStream(spriteName));
                        frames.add(newFrame);
                        cache.addToCache(spriteName, newFrame);
                    }
                    break;

            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void animate(int interval)
    {
        frameCount = 0;
        timer = new Timer();
        timer.schedule(new AnimationTimer(), 0, interval);
    }

    public void stopAnimation()
    {
        timer.cancel();
    }

    public BufferedImage getFrame()
    {
        return frames.get(frameCount);
    }

    public BufferedImage getRotatedFrame()
    {
        BufferedImage frame = frames.get(frameCount);
        BufferedImage b = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D)b.getGraphics();
        g.rotate(radians, 15, 15);
        g.drawImage(frame, 0, 0, null);
        return b;
    }

    private class AnimationTimer extends TimerTask
    {
        @Override
        public void run()
        {
            radians += 0.0628d;

            if (animationName.equals("die"))
            {
                if (frameCount < frames.size() - 1) frameCount++;
            }
            else if (animationName.equals("new-die"))
            {
                if (frameCount < frames.size() - 1) frameCount ++;
            }
            else
            {
                if (frameCount == frames.size() - 1) frameCount = 0;
                else frameCount++;
            }
        }
    }
}
