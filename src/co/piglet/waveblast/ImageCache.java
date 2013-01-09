package co.piglet.waveblast;

import java.awt.image.BufferedImage;
import java.util.concurrent.ConcurrentHashMap;

public class ImageCache
{
    private ConcurrentHashMap<String, BufferedImage> cache;

    public ImageCache()
    {
        cache = new ConcurrentHashMap<>();
    }

    public boolean contains(String key)
    {
        return cache.containsKey(key);
    }

    public void addToCache(String key, BufferedImage frame)
    {
        cache.put(key, frame);
    }

    public BufferedImage getFromCache(String key)
    {
        return cache.get(key);
    }
}
