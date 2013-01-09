package co.piglet.waveblast;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.LockSupport;

/**
 * Version 1.0 Rendering engine for WaveBlast
 */
public class GameEngine
{
    private int resolutionX;
    private int resolutionY;
    private boolean fullScreen;

    // Rendering
    private Canvas c;
    private Graphics2D g;
    private BufferStrategy strategy;
    private ImageCache cache;

    // Audio Flags
    private boolean shieldSoundOn;
    private SoundBank sounds;

    // Controls
    private boolean upPressed;
    private boolean downPressed;
    private boolean leftPressed;
    private boolean rightPressed;
    private boolean spacePressed;
    private boolean controlPressed;
    private boolean canFire;
    private boolean shieldOn;
    private int shieldEnergy = 1000;

    private ControllerSupport controller;

    // Game Status
    private int score;
    private int lives = 3;
    private boolean dead = false;
    private boolean gameOver = false;
    private boolean gameStarted = false;

    // Rendering flags
    private boolean flash;
    private Timer flashTimer;

    // PowerUps
    private boolean hasPowerUp = false;

    private boolean nonStop = false;
    private boolean tripleShot = false;
    private boolean rapidFire = false;
    private boolean extraShield = false;
    private int powerUpEnergy = 1000;

    private boolean hasLaser;
    private boolean drawLazer;
    private Sprite lazer;

    private boolean running = false;

    // Render Queues
    private Sprite playerShip;
    private ConcurrentLinkedQueue<Sprite> hostileEntities;
    private ConcurrentLinkedQueue<Sprite> hostileProjectile;
    private ConcurrentLinkedQueue<Sprite> friendlyEntities;
    private ConcurrentLinkedQueue<Sprite> friendlyProjectiles;
    private ConcurrentLinkedQueue<Sprite> powerUps;
    private ConcurrentLinkedQueue<Sprite> stars;
    private ConcurrentLinkedQueue<Sprite> laserStars;
    //private BufferedImage level;
    private BufferedImage title;

    // Debug shapes
    private Rectangle laserShape = new Rectangle(0, 0, 0, 0);

    public GameEngine(boolean fullScreen, int x, int y)
    {
        // Create the frame
        Dimension d;
        if (!fullScreen) d = new Dimension(x, y + 28);
        else d = new Dimension(x, y);

        controller = new ControllerSupport();

        cache = new ImageCache();

        sounds = new SoundBank();
        this.loanSoundEffects();

        resolutionX = (int) d.getWidth();
        resolutionY = (int) d.getHeight();
        this.fullScreen = fullScreen;

        Frame container = new Frame("WaveBlast 1.4");
        container.setIgnoreRepaint(true);

        GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice graphicsDevice = graphicsEnvironment.getDefaultScreenDevice();

        if (this.fullScreen)
        {
            container.setUndecorated(true);
            if (graphicsDevice.isFullScreenSupported())
            {
                graphicsDevice.setFullScreenWindow(container);
            }
            else
            {
                System.out.println("Full screen is not supported on your system :(");
            }
        }
        if (graphicsDevice.isDisplayChangeSupported())
        {
            int colorDepth = 32;
            graphicsDevice.setDisplayMode(new DisplayMode(this.resolutionX, this.resolutionY, colorDepth, DisplayMode.REFRESH_RATE_UNKNOWN));
        }

        container.setPreferredSize(new Dimension(resolutionX, resolutionY));
        container.setLayout(new BorderLayout());
        container.setBounds(0, 0, resolutionX, resolutionY);
        container.pack();
        container.setResizable(false);
        container.setVisible(true);
        container.addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                System.exit(0);
            }
        });

        container.requestFocus();

        c = new Canvas();
        c.setBounds(0, 0, resolutionX, resolutionY);
        c.setIgnoreRepaint(true);
        c.addKeyListener(new KeyInputHandler());
        c.requestFocus();

        container.add(c, BorderLayout.CENTER);
        container.pack();

        // Double buffering
        c.createBufferStrategy(2);
        strategy = c.getBufferStrategy();

        // Create graphics object from the buffer strategy
        g = (Graphics2D) strategy.getDrawGraphics();

        HashMap<RenderingHints.Key, Object> renderingHints = new HashMap<>();
        renderingHints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);
        renderingHints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        //renderingHints.put(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // Enable AA for vectors
        g.setRenderingHints(renderingHints);

        try
        {
            flashTimer = new Timer();
            flashTimer.schedule(new FlashTimer(), 0, 500);
            title = ImageIO.read(new FileInputStream("sprites/title.png"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void loanSoundEffects()
    {
        sounds.addSound("explode", new SoundEffect("audio/pop.vgz", 1.2d));
        sounds.addSound("shoot", new SoundEffect("audio/pewpew.vgz", 0.22d));
        sounds.addSound("triple" , new SoundEffect("audio/triple.vgz", 0.22d));
        sounds.addSound("shield" , new SoundEffect("audio/shieldfx.vgz", 2.0d));
        sounds.addSound("shield-off", new SoundEffect("audio/shielddrop.vgz", 0.25d));
        sounds.addSound("power", new SoundEffect("audio/powerup.vgz", 0.6d));
        sounds.addSound("reflect", new SoundEffect("audio/reflect.vgz", 2.0d));
        sounds.addSound("laser", new SoundEffect("audio/lazer.vgz", 2.0d));
    }

    private void renderUI()
    {
        Font f = new Font("Consolas", Font.BOLD, 22);
        g.setFont(f);
        g.setColor(Color.GRAY);
        g.drawString("SCORE: " + score, 21, 40);
        g.drawString("SCORE: " + score, 19, 40);
        g.drawString("SCORE: " + score, 20, 39);
        g.drawString("SCORE: " + score, 20, 41);
        g.setColor(Color.BLACK);
        g.drawString("SCORE: " + score, 20, 40);

        g.setColor(Color.GRAY);
        g.drawString("LIVES: " + lives, 21, 75);
        g.drawString("LIVES: " + lives, 19, 75);
        g.drawString("LIVES: " + lives, 20, 74);
        g.drawString("LIVES: " + lives, 20, 76);
        g.setColor(Color.BLACK);
        g.drawString("LIVES: " + lives, 20, 75);

        g.setColor(Color.WHITE);
        g.fillRoundRect(20, 420, 104, 20, 10, 10);

        g.setColor(Color.BLUE);
        g.fillRoundRect(22, 422, ((shieldEnergy / 10)), 16, 5, 5);

        if (hasPowerUp)
        {
            g.setColor(Color.WHITE);
            g.fillRoundRect(150, 420, 104, 20, 10, 10);

            g.setColor(Color.GREEN);
            g.fillRoundRect(152, 422, ((powerUpEnergy / 10)), 16, 5, 5);
        }

        if (gameOver)
        {
            f = new Font("Consolas", Font.BOLD, 48);
            g.setFont(f);
            g.setColor(Color.GRAY);
            g.drawString("GAME OVER", 201, 240);
            g.drawString("GAME OVER", 199, 240);
            g.drawString("GAME OVER", 200, 239);
            g.drawString("GAME OVER", 200, 241);
            g.setColor(Color.BLACK);
            g.drawString("GAME OVER", 200, 240);
        }
    }

    private void render()
    {
        if (gameStarted)
        {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, resolutionX, resolutionY);

            // Draw the stars!
            for (Sprite star : stars)
            {
                if (star.x < 0)
                {
                    star.terminate();
                    stars.remove(star);
                }
                else g.drawImage(star.getFrame(), star.x, star.y, null);
            }

            // This is where we render the next frame
            for (Sprite sprite : friendlyEntities)
            {
                g.drawImage(sprite.getFrame(), sprite.x, sprite.y, null);
            }

            for (Sprite sprite : friendlyProjectiles)
            {
                if (sprite.x > resolutionX || sprite.y > resolutionY)
                {
                    sprite.terminate();
                    friendlyProjectiles.remove(sprite);
                }
                else g.drawImage(sprite.getFrame(), sprite.x, sprite.y, null);
            }

            for (Sprite sprite : hostileEntities)
            {
                if (sprite.x + 80 < 0)
                {
                    sprite.terminate();
                    hostileEntities.remove(sprite);
                }
                else
                {
                    g.drawImage(sprite.getFrame(), sprite.x, sprite.y, null);
                }
            }

            for (Sprite sprite : hostileProjectile)
            {
                if (sprite.x + 20 < 0)
                {
                    sprite.terminate();
                    hostileProjectile.remove(sprite);
                }
                else
                {
                    g.drawImage(sprite.getFrame(), sprite.x, sprite.y, null);
                }
            }

            for (Sprite sprite : powerUps)
            {
                if (sprite.x + 20 < 0)
                {
                    sprite.terminate();
                    powerUps.remove(sprite);
                }
                else
                {
                    g.drawImage(sprite.getFrame(), sprite.x, sprite.y, null);
                }
            }

            g.drawImage(playerShip.getFrame(), playerShip.x, playerShip.y, null);

            if (drawLazer)
            {
                g.drawImage(lazer.getFrame(), playerShip.x, playerShip.y, null);

                if (hasLaser)
                {
                    for (Sprite sprite : laserStars)
                    {
                        if (sprite.x > resolutionX || sprite.y > resolutionY)
                        {
                            sprite.terminate();
                            laserStars.remove(sprite);
                        }
                        else g.drawImage(sprite.getFrame(), sprite.x, playerShip.y + sprite.y, null);
                    }
                }
            }

            this.renderUI();
        }
        else
        {
            g.drawImage(title, 0, 0, null);
            if (flash)
            {
                g.setColor(Color.BLACK);
                g.fillRect(310, 360, 290, 30);
            }
        }

        strategy.show();
    }

    public void loadLevel()
    {
        // Load the level data
        new Level();

        // Init Objects
        friendlyEntities = new ConcurrentLinkedQueue<>();
        friendlyProjectiles = new ConcurrentLinkedQueue<>();
        hostileEntities = new ConcurrentLinkedQueue<>();
        hostileProjectile = new ConcurrentLinkedQueue<>();
        powerUps = new ConcurrentLinkedQueue<>();
        stars = new ConcurrentLinkedQueue<>();
        laserStars = new ConcurrentLinkedQueue<>();

        playerShip = new Sprite("sprites/ship.png", cache);
        playerShip.loadAdditionalAnimations("ship-shield", cache);
        playerShip.setActiveAnimation("east", 1000);

        lazer = new Sprite("lazerstart", cache);
        lazer.loadAdditionalAnimations("lazer", cache);

        playerShip.x = 75;
        playerShip.y = (resolutionY / 2) - (playerShip.getFrame().getHeight() / 2);

        c.requestFocus();
        c.requestFocusInWindow();
    }

    public void masterLoop()
    {
        running = true;

        if (fullScreen)
        {
            while (running)
            {
                if (gameStarted) this.renderStars();
                if (!dead && gameStarted)
                {
                    score += 1;
                    this.input();
                    this.logic();
                }
                this.render();
            }
        }
        else
        {
            Timer t = new Timer();
            t.schedule(new InputTimer(), 0, 1000 / 60);

            while (running)
            {
                this.render();
            }
        }
    }

    private void renderStars()
    {
        if (hasLaser) this.renderLaserStars();
        double d = Math.random();

        if (d > 0.65d)
        {
            Sprite s = new Sprite("sprites/star.png", cache);
            s.x = resolutionX;
            s.y = (int) (Math.random() * (resolutionY - 64));
            s.setActiveAnimation("east", 1000);
            int speed = Math.random() > 0.5d ? -8 : -5;

            s.moveSprite(speed, 0, 5);

            stars.add(s);
        }
    }

    private void renderLaserStars()
    {
        double d = Math.random();

        if (d > 0.40d)
        {
            Sprite s = new Sprite("sprites/b-star.png", cache);
            s.x = playerShip.x + 91;
            s.y = (int) ((Math.random() * (50)));
            s.setActiveAnimation("east", 1000);
            int speed = Math.random() > 0.5d ? 12 : 7;

            s.moveSprite(speed, 0, 5);

            laserStars.add(s);
        }
    }

    private void logic()
    {
        // This is where we spawn enemies etc..
        double d = Math.random();
        double delta = score > 10000 ? 0.93d : 0.96d;
        if (score > 50000) delta = 0.90d;

        if (hasPowerUp)
        {
            if (powerUpEnergy > 0) powerUpEnergy -= 3;
            else
            {
                hasPowerUp = false;
                tripleShot = false;
                rapidFire = false;
                nonStop = false;
                if (hasLaser)
                {
                    drawLazer = false;
                    sounds.stopSound("laser");
                    hasLaser = false;
                }
                //shipSpeed = 3;

                if (extraShield)
                {
                    extraShield = false;
                    if (shieldSoundOn) sounds.playSound("shield-off", 2);
                    shieldSoundOn = false;
                    sounds.stopSound("shield");
                }
            }
        }

        if (d > delta)
        {
            double type = Math.random();

            Sprite s;
            if (type > 0.95d)
            {
                // Spawn hostile
                s = new Sprite("sprites/enemy-2.png", cache);
                s.x = resolutionX;
                s.y = (int) (Math.random() * (resolutionY - 64));
                s.setActiveAnimation("east", 1000);
                s.moveSprite(-3, 0, 1000 / 59);
                s.enemyType = 1;
            }
            else
            {

                // Spawn hostile
                s = new Sprite("sprites/enemy.png", cache);
                s.x = resolutionX;
                s.y = (int) (Math.random() * (resolutionY - 64));
                s.setActiveAnimation("east", 1000);
                int speed = Math.random() > 0.5d ? -5 : -3;

                double angleChance = Math.random();
                if (angleChance < 0.8d) s.moveSprite(speed, 0, 1000 / 60);
                else if (angleChance < 0.9d) s.moveSprite(speed, 1, 1000 / 60);
                else s.moveSprite(speed, -1, 1000 / 60);

            }

            hostileEntities.add(s);
        }

        // Enemy shots!
        for (Sprite enemy : hostileEntities)
        {
            if (enemy.isAlive)
            {
                if (Math.random() > 0.985d)
                {
                    if (enemy.enemyType == 1)
                    {
                        Sprite s = new Sprite("sprites/shot-e-2.png", cache);
                        s.x = enemy.x;
                        s.y = enemy.y + 25;
                        s.setActiveAnimation("east", 1000);
                        s.moveSprite(-2, 0, 5);
                        Sprite s2 = new Sprite("sprites/shot-e-2.png", cache);
                        s2.x = enemy.x;
                        s2.y = enemy.y + 25;
                        s2.setActiveAnimation("east", 1000);
                        s2.moveSprite(-2, 1, 5);
                        Sprite s3 = new Sprite("sprites/shot-e-2.png", cache);
                        s3.x = enemy.x;
                        s3.y = enemy.y + 25;
                        s3.setActiveAnimation("east", 1000);
                        s3.moveSprite(-2, -1, 5);

                        hostileProjectile.add(s);
                        hostileProjectile.add(s2);
                        hostileProjectile.add(s3);

                    }
                    else
                    {

                        Sprite s = new Sprite("sprites/shot-e.png", cache);
                        s.x = enemy.x;
                        s.y = enemy.y + 25;
                        s.setActiveAnimation("east", 1000);
                        s.moveSprite(-2, 0, 5);

                        hostileProjectile.add(s);
                    }
                }
            }
        }

        // Check Collisions!!!!
        Polygon p = new Polygon();
        p.addPoint(playerShip.x + 5, playerShip.y + 11);
        p.addPoint(playerShip.x + 26, playerShip.y + 11);
        p.addPoint(playerShip.x + 61, playerShip.y + 34);
        p.addPoint(playerShip.x + 28, playerShip.y + 48);
        p.addPoint(playerShip.x + 5, playerShip.y + 48);

        if (hasLaser)
        {
            laserShape = new Rectangle(playerShip.x + 91, playerShip.y + 4, 640, 60);
        }

        if (!dead)
        {
            for (Sprite powerup : powerUps)
            {
                Ellipse2D r2 = new Ellipse2D.Double(powerup.x, powerup.y, 25, 25);
                Area newArea = new Area((Shape) r2.clone());
                newArea.intersect(new Area(p));
                if (!newArea.isEmpty())
                {
                    tripleShot = false;
                    rapidFire = false;
                    nonStop = false;
                    extraShield = false;
                    hasLaser = false;
                    drawLazer = false;
                    sounds.stopSound("laser");

                    if (shieldSoundOn)
                    {
                        sounds.playSound("shield-off" ,1);
                        shieldSoundOn = false;
                        sounds.stopSound("shield");
                    }

                    powerUps.remove(powerup);
                    double whichPowerup = Math.random();

                    if (whichPowerup < 0.33d) tripleShot = true;
                    else if (whichPowerup < 0.66d)
                    {
                        lazer.setActiveAnimation("east", 5);
                        Timer t = new Timer();
                        t.schedule(new EnableLazer(), 550);

                        drawLazer = true;
                        sounds.playSound("laser", 20);
                    }
                    else
                    {
                        extraShield = true;
                        if (!shieldOn) sounds.playSound("shield", 20);
                        shieldSoundOn = true;
                    }

                    powerUpEnergy = 1000;
                    hasPowerUp = true;
                    sounds.playSound("power", 2);
                }
            }

            for (Sprite hostileShots : hostileProjectile)
            {

                Ellipse2D r2 = new Ellipse2D.Double(hostileShots.x, hostileShots.y, 15, 15);

                boolean destroyed = false;

                // Check is destroyed by laser
                if (hasLaser)
                {
                    Area laserArea = new Area((Shape) r2.clone());
                    laserArea.intersect(new Area(laserShape));
                    if (!laserArea.isEmpty())
                    {
                        destroyed = true;
                        hostileShots.stopMove();
                        hostileProjectile.remove(hostileShots);
                    }

                }

                if (!destroyed)
                {
                    Area newArea = new Area((Shape) r2.clone());
                    newArea.intersect(new Area(p));
                    if (!newArea.isEmpty())
                    {
                        if (shieldOn)
                        {
                            hostileShots.stopMove();
                            hostileShots.moveSprite(2, 0, 5);
                            friendlyProjectiles.add(hostileShots);
                            hostileProjectile.remove(hostileShots);
                            sounds.playSound("reflect", 1);
                        }
                        else
                        {
                            lostLife();
                        }
                    }
                }
            }

            for (Sprite enemy : hostileEntities)
            {
                if (enemy.isAlive)
                {
                    Rectangle r2 = new Rectangle(enemy.x + 8, enemy.y + 8, 48, 48);

                    // Check for laser smash
                    boolean destroyed = false;

                    if (hasLaser)
                    {
                        Area laserArea = new Area((Shape) r2.clone());
                        laserArea.intersect(new Area(laserShape));

                        if (!laserArea.isEmpty())
                        {
                            destroyed = true;
                            killEnemy(enemy, null);
                        }
                    }

                    if (!destroyed)
                    {

                        Area newArea = new Area((Shape) r2.clone());

                        newArea.intersect(new Area(p));
                        if (!newArea.isEmpty())
                        {
                            if (shieldOn)
                            {
                                killEnemy(enemy, null);
                            }
                            else
                            {
                                lostLife();
                            }
                        }

                        for (Sprite shot : friendlyProjectiles)
                        {

                            Ellipse2D r3 = new Ellipse2D.Double(shot.x, shot.y, 15, 15);
                            newArea = new Area((Shape) r3.clone());

                            newArea.intersect(new Area(r2));
                            if (!newArea.isEmpty())
                            {
                                killEnemy(enemy, shot);
                            }
                        }
                    }
                }
            }
        }
    }


    private class EnableLazer extends TimerTask
    {
        @Override
        public void run()
        {
            hasLaser = true;
            lazer.setActiveAnimation("lazer", 15);
        }
    }

    private void lostLife()
    {
        sounds.playSound("explode", 2);
        hasLaser = false;
        drawLazer = false;
        if (shieldSoundOn) sounds.stopSound("shield");
        sounds.stopSound("laser");
        playerShip.setActiveAnimation("die", 50);
        lives--;
        dead = true;
        Timer t = new Timer();
        t.schedule(new ResetTimer(), 2500);
    }

    private void killEnemy(Sprite enemy, Sprite killedBy)
    {
        sounds.playSound("explode", 1);

        score += 250;

        if (killedBy != null)
        {
            if (Math.random() > 0.965d)
            {
                Sprite newPowerUp = new Sprite("sprites/powerup-1.png", cache);
                newPowerUp.rotated = true;
                newPowerUp.moveSprite(-2, 0, 1000 / 60);
                newPowerUp.x = enemy.x + 15;
                newPowerUp.y = enemy.y + 15;
                newPowerUp.setActiveAnimation("east", 10);
                powerUps.offer(newPowerUp);
            }
            if (!nonStop)
            {
                friendlyProjectiles.remove(killedBy);
                killedBy.terminate();
            }
        }

        enemy.isAlive = false;
        enemy.setActiveAnimation("die", 50);
        enemy.stopMove();

        Timer t = new Timer();
        t.schedule(new RemoveEnemyAfterDuration(enemy), 750);
    }

    private class RemoveEnemyAfterDuration extends TimerTask
    {
        private Sprite enemy;

        public RemoveEnemyAfterDuration(Sprite enemy)
        {
            this.enemy = enemy;
        }

        @Override
        public void run()
        {
            enemy.terminate();
            hostileEntities.remove(enemy);
        }
    }


    private void reset()
    {
        if (lives > 0)
        {
            hostileEntities.clear();
            hostileProjectile.clear();
            friendlyEntities.clear();
            playerShip.x = 75;
            playerShip.y = (resolutionY / 2) - (playerShip.getFrame().getHeight() / 2);
            shieldEnergy = 1000;
            playerShip.setActiveAnimation("east", 1000);
            dead = false;
            powerUpEnergy = 0;
            hasPowerUp = false;
            tripleShot = false;
            hasLaser = false;
            drawLazer = false;
            extraShield = false;
        }
        else
        {
            // Game Over
            Timer t = new Timer();
            t.schedule(new ReturnToTitle(), 3500);
            flashTimer = new Timer();
            flashTimer.schedule(new FlashTimer(), 0, 750);
            gameOver = true;
        }
    }

    public class ReturnToTitle extends TimerTask
    {

        @Override
        public void run()
        {
            gameOver = false;
            gameStarted = false;
            canFire = false;
            score = 0;
            lives = 3;
            shieldEnergy = 1000;
            powerUpEnergy = 0;
            hostileEntities.clear();
            hostileProjectile.clear();
            friendlyEntities.clear();
            powerUps.clear();
            playerShip.x = 75;
            playerShip.y = (resolutionY / 2) - (playerShip.getFrame().getHeight() / 2);
            shieldEnergy = 1000;
            playerShip.setActiveAnimation("east", 1000);
            dead = false;
            shieldOn = false;
            tripleShot = false;
            nonStop = false;
            rapidFire = false;
            extraShield = false;
            hasPowerUp = false;
            hasLaser = false;
            drawLazer = false;
            if (shieldSoundOn) sounds.stopSound("shield");

        }
    }

    private class ResetTimer extends TimerTask
    {
        @Override
        public void run()
        {
            reset();
        }
    }

    private class StartTimer extends TimerTask
    {
        @Override
        public void run()
        {
            gameStarted = true;
        }
    }

    private class InputTimer extends TimerTask
    {
        @Override
        public void run()
        {
            if (gameStarted) renderStars();
            if (!dead && gameStarted)
            {
                score += 1;
                //pollControler();
                input();
                logic();
            }
        }
    }

    public void pollControler()
    {
        float x = controller.getXAxisPosition();
        float y = controller.getYAxisPosition();
        float btn1 = controller.getButton(0);
        float btn2 = controller.getButton(1);
        rightPressed = x == 1.0f;
        leftPressed = x == -1.0f;
        downPressed = y == 1.0f;
        upPressed = y == -1.0f;
        if (btn1 == 1.0f)
        {
            if (!spacePressed)
            {
                spacePressed = true;
                canFire = true;
            }
        }
        else
        {
            spacePressed = false;
        }

        controlPressed = btn2 == 1.0f;
    }

    private void input()
    {
        int shipSpeed = 2;
        if (downPressed) playerShip.y += playerShip.y + 64 >= resolutionY ? 0 : shipSpeed;
        if (upPressed) playerShip.y -= playerShip.y <= 0 ? 0 : shipSpeed;
        if (leftPressed) playerShip.x -= playerShip.x <= 0 ? 0 : shipSpeed;
        if (rightPressed) playerShip.x += playerShip.x + 64 >= resolutionX / 2 ? 0 : shipSpeed;
        if (controlPressed || extraShield)
        {
            if (extraShield && shieldEnergy < 1000) shieldEnergy++;
            shieldOn = (shieldEnergy > 0) || extraShield;
            if (shieldOn)
            {
                if (!extraShield) shieldEnergy -= 5;
                playerShip.setActiveAnimation("ship-shield", 100);
            }
            else
            {
                playerShip.setActiveAnimation("east", 1000);
                if (shieldSoundOn)
                {
                    sounds.playSound("shield-off", 2);
                    sounds.stopSound("shield");
                    shieldSoundOn = false;
                }
            }
        }
        else
        {
            shieldOn = false;
            playerShip.setActiveAnimation("east", 1000);
            if (shieldEnergy < 1000) shieldEnergy += 1;
        }
        if (canFire || (rapidFire && spacePressed))
        {

            if (tripleShot)
            {
                sounds.playSound("triple", 1);
                Sprite northShot = new Sprite("sprites/triple-3.png", cache);
                northShot.x = playerShip.x + playerShip.getFrame().getWidth() - 12;
                northShot.y = playerShip.y + playerShip.getFrame().getHeight() / 2 - 7;
                Sprite southShot = new Sprite("sprites/triple-2.png", cache);
                southShot.x = playerShip.x + playerShip.getFrame().getWidth() - 12;
                southShot.y = playerShip.y + playerShip.getFrame().getHeight() / 2 - 7;
                northShot.setActiveAnimation("east", 1000);
                northShot.moveSprite(2, 1, 5);
                southShot.setActiveAnimation("east", 1000);
                southShot.moveSprite(2, -1, 5);
                friendlyProjectiles.add(northShot);
                friendlyProjectiles.add(southShot);
                Sprite newShot = new Sprite("sprites/triple-1.png", cache);
                newShot.x = playerShip.x + playerShip.getFrame().getWidth() - 12;
                newShot.y = playerShip.y + playerShip.getFrame().getHeight() / 2 - 6;
                newShot.setActiveAnimation("east", 1000);
                newShot.moveSprite(2, 0, 5);
                friendlyProjectiles.add(newShot);
            }
            else
            {
                sounds.playSound("shoot", 1);
                Sprite newShot = new Sprite("sprites/shot.png", cache);
                newShot.x = playerShip.x + playerShip.getFrame().getWidth() - 12;
                newShot.y = playerShip.y + playerShip.getFrame().getHeight() / 2 - 6;

                newShot.setActiveAnimation("east", 1000);
                newShot.moveSprite(2, 0, 5);
                friendlyProjectiles.add(newShot);
            }
            score += 5;

            canFire = false;
        }
    }

    protected class KeyInputHandler extends KeyAdapter
    {
        @Override
        public void keyPressed(KeyEvent e)
        {
            if (e.getKeyCode() == KeyEvent.VK_ENTER && !gameStarted)
            {
                flashTimer.cancel();
                gameStarted = true;
            }
            if (e.getKeyCode() == KeyEvent.VK_UP) upPressed = true;
            if (e.getKeyCode() == KeyEvent.VK_DOWN) downPressed = true;
            if (e.getKeyCode() == KeyEvent.VK_LEFT) leftPressed = true;
            if (e.getKeyCode() == KeyEvent.VK_RIGHT) rightPressed = true;
            if (e.getKeyCode() == KeyEvent.VK_CONTROL)
            {
                controlPressed = true;
                if (shieldEnergy > 0 && !shieldSoundOn)
                {
                    if (!shieldOn) sounds.playSound("shield", 20);
                    shieldSoundOn = true;
                }
            }
            if (e.getKeyCode() == KeyEvent.VK_SPACE)
            {
                if (!spacePressed)
                {
                    spacePressed = true;
                    canFire = true;
                }
            }

            if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
            {
                running = false;
            }
        }

        @Override
        public void keyReleased(KeyEvent e)
        {
            if (e.getKeyCode() == KeyEvent.VK_UP) upPressed = false;
            if (e.getKeyCode() == KeyEvent.VK_DOWN) downPressed = false;
            if (e.getKeyCode() == KeyEvent.VK_LEFT) leftPressed = false;
            if (e.getKeyCode() == KeyEvent.VK_RIGHT) rightPressed = false;
            if (e.getKeyCode() == KeyEvent.VK_CONTROL)
            {
                controlPressed = false;
                if (shieldSoundOn && !extraShield)
                {
                    sounds.playSound("shield-off" , 2);
                    sounds.stopSound("shield");
                    shieldSoundOn = false;
                }
            }
            if (e.getKeyCode() == KeyEvent.VK_SPACE)
            {
                spacePressed = false;
            }
        }

        @Override
        public void keyTyped(KeyEvent e)
        {

        }
    }

    private class FlashTimer extends TimerTask
    {
        @Override
        public void run()
        {
            flash = !flash;
        }
    }
}
