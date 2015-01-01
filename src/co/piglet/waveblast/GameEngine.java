package co.piglet.waveblast;

import sun.java2d.pipe.hw.ExtendedBufferCapabilities;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Version 1.0 Rendering engine for WaveBlast
 */
public class GameEngine {

    // These are used to render the UI
    private int resolutionX;
    private int resolutionY;

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
    public static long ticks = 0;

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

    // Score Stuff
    private int extraLifeScore = 17500;
    private float extraLifeMultiple = 1.25F;
    private int highScore = 10000;
    private int multi = 1;
    private int bounceBacks = 0;

    // Render Queues
    private Sprite playerShip;
    private ConcurrentLinkedQueue<Sprite> hostileEntities;
    private ConcurrentLinkedQueue<Sprite> hostileProjectile;
    private ConcurrentLinkedQueue<Sprite> friendlyEntities;
    private ConcurrentLinkedQueue<Sprite> friendlyProjectiles;
    private ConcurrentLinkedQueue<Sprite> powerUps;
    private ConcurrentLinkedQueue<Sprite> stars;
    private ConcurrentLinkedQueue<Sprite> laserStars;
    private ConcurrentLinkedQueue<Sprite> terrainSprites;
    private BufferedImage title;
    private BufferedImage uiShield;
    private BufferedImage uiPower;
    private BufferedImage uiLife;

    // Debug shapes
    private Rectangle laserShape = new Rectangle(0, 0, 0, 0);

    public GameEngine(boolean fullScreen, int x, int y) throws AWTException {

        controller = new ControllerSupport();

        cache = new ImageCache();

        sounds = new SoundBank();
        this.loadSoundEffects();

        resolutionX = x;
        resolutionY = y;

        Frame container = new Frame("WaveBlast Reloaded 1.0");
        container.setIgnoreRepaint(true);

        GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice graphicsDevice = graphicsEnvironment.getDefaultScreenDevice();

        if (fullScreen) {
            container.setUndecorated(true);
            if (graphicsDevice.isFullScreenSupported()) {
                graphicsDevice.setFullScreenWindow(container);
            } else {
                System.out.println("Full screen is not supported on your system :(");
            }
        }
        if (graphicsDevice.isDisplayChangeSupported()) {
            int colorDepth = 32;
            graphicsDevice.setDisplayMode(new DisplayMode(this.resolutionX, this.resolutionY, colorDepth, DisplayMode.REFRESH_RATE_UNKNOWN));
        }

        //container.setPreferredSize(new Dimension(resolutionX, resolutionY));
        container.setLayout(new BorderLayout());
        container.setBounds(0, 0, resolutionX, resolutionY);
        container.setResizable(false);
        container.setVisible(true);
        container.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
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

        ExtendedBufferCapabilities ebc = new ExtendedBufferCapabilities(strategy.getCapabilities());
        ebc = ebc.derive(ExtendedBufferCapabilities.VSyncType.VSYNC_ON);

        c.createBufferStrategy(2, ebc);
        strategy = c.getBufferStrategy();

        // Create graphics object from the buffer strategy
        g = (Graphics2D) strategy.getDrawGraphics();

        HashMap<RenderingHints.Key, Object> renderingHints = new HashMap<>();
        renderingHints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);
        renderingHints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        //renderingHints.put(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // Enable AA for vectors
        g.setRenderingHints(renderingHints);

        try {

            Properties prop = new Properties();
            prop.load(new FileReader(new File("config.prop")));
            highScore = Integer.parseInt(prop.getProperty("highscore"));

            flashTimer = new Timer();
            flashTimer.schedule(new FlashTimer(), 0, 500);
            title = ImageIO.read(new FileInputStream("sprites/title.png"));
            uiPower = ImageIO.read(new FileInputStream("sprites/powerup-1.png"));
            uiShield = ImageIO.read(new FileInputStream("sprites/ship-shield-5.png"));
            uiLife = ImageIO.read(new FileInputStream("sprites/ship.png"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadSoundEffects() {
        sounds.addSound("explode", new SoundEffect("audio/pop.vgz", 1.2d));
        sounds.addSound("shoot", new SoundEffect("audio/pewpew.vgz", 0.22d));
        sounds.addSound("triple", new SoundEffect("audio/triple.vgz", 0.22d));
        sounds.addSound("shield", new SoundEffect("audio/shieldfx.vgz", 2.0d));
        sounds.addSound("shield-off", new SoundEffect("audio/shielddrop.vgz", 0.25d));
        sounds.addSound("power", new SoundEffect("audio/powerup.vgz", 0.6d));
        sounds.addSound("reflect", new SoundEffect("audio/reflect.vgz", 2.0d));
        sounds.addSound("laser", new SoundEffect("audio/lazer.vgz", 2.0d));
    }

    private void renderUI() {

        Font f = new Font("Consolas", Font.BOLD, 22);

        // This is the score, it goes in the top left
        g.setFont(f);
        g.setColor(Color.GRAY);
        g.drawString("SCORE: " + score + " (x " + multi + ")", 21, 40);
        g.drawString("SCORE: " + score + " (x " + multi + ")", 19, 40);
        g.drawString("SCORE: " + score + " (x " + multi + ")", 20, 39);
        g.drawString("SCORE: " + score + " (x " + multi + ")", 20, 41);
        g.setColor(Color.WHITE);
        g.drawString("SCORE: " + score + " (x " + multi + ")", 20, 40);

        // This is the top score, it goes in the top right
        g.setColor(Color.GRAY);
        g.drawString("HIGH: " + highScore, resolutionX - 189, 40);
        g.drawString("HIGH: " + highScore, resolutionX - 191, 40);
        g.drawString("HIGH: " + highScore, resolutionX - 190, 39);
        g.drawString("HIGH: " + highScore, resolutionX - 190, 41);
        g.setColor(Color.WHITE);
        g.drawString("HIGH: " + highScore, resolutionX - 190, 40);

        // This goes bottom left
        g.setColor(Color.CYAN);
        g.fillRoundRect(50, resolutionY - 50, 104, 20, 10, 10);

        g.setColor(Color.BLUE);
        g.fillRoundRect(52, resolutionY - 48, ((shieldEnergy / 10)), 16, 5, 5);

        g.drawImage(uiShield, 15, resolutionY - 55, 32, 32, null);

        if (hasPowerUp) {
            g.setColor(Color.GREEN);
            g.fillRoundRect(50, resolutionY - 80, 104, 20, 10, 10);

            g.setColor(new Color(0, 100, 0));
            g.fillRoundRect(52, resolutionY - 78, ((powerUpEnergy / 10)), 16, 5, 5);

            g.drawImage(uiPower, 18, resolutionY - 85, 25, 25, null);
        }

        if (lives <= 5) {
            int x = resolutionX - 60;
            for (int i = 0; i < lives; i++) {
                g.drawImage(uiLife, x, resolutionY - 55, 32, 32, null);
                x -= 40;
            }
        } else {
            g.drawImage(uiLife, resolutionX - 100, resolutionY - 55, 32, 32, null);
            g.setColor(Color.GRAY);
            g.drawString(String.valueOf(lives), resolutionX - 61, resolutionY - 33);
            g.drawString(String.valueOf(lives), resolutionX - 59, resolutionY - 33);
            g.drawString(String.valueOf(lives), resolutionX - 60, resolutionY - 33);
            g.drawString(String.valueOf(lives), resolutionX - 60, resolutionY - 33);
            g.setColor(Color.WHITE);
            g.drawString(String.valueOf(lives), resolutionX - 60, resolutionY - 33);
        }

        if (gameOver) {
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

    private void render() {

        g = (Graphics2D) strategy.getDrawGraphics();

        if (gameStarted) {

            g.setColor(Color.BLACK);
            g.fillRect(0, 0, resolutionX, resolutionY);

            // Draw the stars!
            for (Sprite star : stars) {
                if (star.x < 0) {
                    star.terminate();
                    stars.remove(star);
                } else g.drawImage(star.getFrame(), star.x, star.y, null);
            }

            for (Sprite sprite : terrainSprites) {
                int terrainWidth = sprite.getFrame().getWidth();
                if (terrainWidth - resolutionX - (-1 * sprite.x) <= resolutionX) {
                    g.drawImage(sprite.getFrame(), sprite.x + terrainWidth, sprite.y, null);
                }
                g.drawImage(sprite.getFrame(), sprite.x, sprite.y, null);
                if (terrainWidth == (-1 * sprite.x) && sprite.x < 0) {
                    sprite.x = 0;
                }
            }

            // This is where we render the next frame
            for (Sprite sprite : friendlyEntities) {
                g.drawImage(sprite.getFrame(), sprite.x, sprite.y, null);
            }

            for (Sprite sprite : friendlyProjectiles) {
                if (sprite.x > resolutionX || sprite.y > resolutionY) {
                    sprite.terminate();
                    friendlyProjectiles.remove(sprite);
                } else g.drawImage(sprite.getFrame(), sprite.x, sprite.y, null);
            }

            for (Sprite sprite : hostileEntities) {
                if (sprite.x + 80 < 0) {
                    sprite.terminate();
                    hostileEntities.remove(sprite);
                } else {
                    g.drawImage(sprite.getFrame(), sprite.x, sprite.y, null);
                }
            }

            for (Sprite sprite : hostileProjectile) {
                if (sprite.x + 20 < 0) {
                    sprite.terminate();
                    hostileProjectile.remove(sprite);
                } else {
                    g.drawImage(sprite.getFrame(), sprite.x, sprite.y, null);
                }
            }

            for (Sprite sprite : powerUps) {
                if (sprite.x + 20 < 0) {
                    sprite.terminate();
                    powerUps.remove(sprite);
                } else {
                    g.drawImage(sprite.getFrame(), sprite.x, sprite.y, null);
                }
            }

            g.drawImage(playerShip.getFrame(), playerShip.x, playerShip.y, null);

            if (drawLazer) {
                g.drawImage(lazer.getFrame(), playerShip.x, playerShip.y, null);

                if (hasLaser) {
                    for (Sprite sprite : laserStars) {
                        if (sprite.x > resolutionX || sprite.y > resolutionY) {
                            sprite.terminate();
                            laserStars.remove(sprite);
                        } else g.drawImage(sprite.getFrame(), sprite.x, playerShip.y + sprite.y, null);
                    }
                }
            }

            this.renderUI();

        } else {

            g.setColor(Color.BLACK);
            g.fillRect(0, 0, resolutionX, resolutionY);
            g.drawImage(title, 320, 100, null);

            if (flash) {
                g.setColor(Color.BLACK);
                g.fillRect(630, 460, 290, 30);
            }


        }

        strategy.show();
        g.dispose();
    }

    public void loadLevel() {
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
        terrainSprites = new ConcurrentLinkedQueue<>();


        playerShip = new Sprite("sprites/ship.png", cache);
        playerShip.loadAdditionalAnimations("ship-shield", cache);
        playerShip.setActiveAnimation("east", 1000);

        lazer = new Sprite("lazerstart", cache);
        lazer.loadAdditionalAnimations("lazer", cache);

        playerShip.x = 75;
        playerShip.y = (resolutionY / 2) - (playerShip.getFrame().getHeight() / 2);

        Sprite topTerrain = new Sprite("sprites/top.png", cache);
        topTerrain.tileable = true;
        Sprite bottomTerrain = new Sprite("sprites/bottom.png", cache);
        bottomTerrain.tileable = true;

        topTerrain.moveSprite(-1, 0);
        topTerrain.setActiveAnimation("east", 1000);
        bottomTerrain.moveSprite(-1, 0);
        bottomTerrain.setActiveAnimation("east", 1000);

        bottomTerrain.y = resolutionY - 128;

        terrainSprites.add(topTerrain);
        terrainSprites.add(bottomTerrain);

        c.requestFocus();
        c.requestFocusInWindow();
    }

    public void masterLoop() {

        running = true;

        while (running) {
            if (gameStarted) this.renderStars();
            this.move();
            if (!dead && gameStarted) {
                score += 1;
                this.input();
                this.logic();
            }
            this.render();
        }

    }

    private void renderStars() {

        if (hasLaser) this.renderLaserStars();
        double d = Math.random();

        if (d > 0.65d) {
            Sprite s = new Sprite("sprites/star.png", cache);
            s.x = resolutionX;
            s.y = (int) (Math.random() * (resolutionY - 64));
            s.setActiveAnimation("east", 1000);
            int speed = Math.random() > 0.5d ? -8 : -5;

            s.moveSprite(speed, 0);

            stars.add(s);
        }

    }

    private void renderLaserStars() {

        double d = Math.random();

        if (d > 0.40d) {
            Sprite s = new Sprite("sprites/b-star.png", cache);
            s.x = playerShip.x + 91;
            s.y = (int) ((Math.random() * (50)));
            s.setActiveAnimation("east", 1000);
            int speed = Math.random() > 0.5d ? 12 : 7;

            s.moveSprite(speed, 0);

            laserStars.add(s);
        }

    }

    private void move() {
        // Move everything
        hostileEntities.forEach(Sprite::move);
        hostileProjectile.forEach(Sprite::move);
        friendlyEntities.forEach(Sprite::move);
        friendlyProjectiles.forEach(Sprite::move);
        powerUps.forEach(Sprite::move);
        stars.forEach(Sprite::move);
        laserStars.forEach(Sprite::move);
        terrainSprites.forEach(Sprite::move);
    }

    private void logic() {

        // This is where we spawn enemies etc..
        double d = Math.random();
        double delta = score > 10000 ? 0.93d : 0.96d;
        if (score > 50000) delta = 0.90d;

        checkScores();

        if (hasPowerUp) {
            if (powerUpEnergy > 0) powerUpEnergy -= 3;
            else {
                hasPowerUp = false;
                tripleShot = false;
                rapidFire = false;
                nonStop = false;
                if (hasLaser) {
                    drawLazer = false;
                    sounds.stopSound("laser");
                    hasLaser = false;
                }
                if (extraShield) {
                    extraShield = false;
                    if (shieldSoundOn) sounds.playSound("shield-off", 2);
                    shieldSoundOn = false;
                    sounds.stopSound("shield");
                }
            }
        }

        if (d > delta) {

            double type = Math.random();
            int minY = 96;
            int maxY = resolutionY - 128;

            Sprite s;

            if (type > 0.95d) {

                if (Math.random() > 0.50) {
                    s = new Sprite("sprites/turret2.png", cache);
                    s.x = resolutionX;
                    s.y = 96;
                    s.setActiveAnimation("east", 1000);
                    s.moveSprite(-1, 0);
                    s.enemyType = EnemyTypes.TURRET_NORTH;
                } else {
                    s = new Sprite("sprites/turret.png", cache);
                    s.x = resolutionX;
                    s.y = resolutionY - 178;
                    s.setActiveAnimation("east", 1000);
                    s.moveSprite(-1, 0);
                    s.enemyType = EnemyTypes.TURRET_SOUTH;
                }

            } else if (type > 0.90d) {

                // Green Triple Shot Ship
                s = new Sprite("sprites/new-enemy-3.png", cache);
                s.x = resolutionX;
                s.y = (int) (Math.random() * (resolutionY - 64));
                s.setActiveAnimation("east", 1000);
                s.moveSprite(-4, 0);
                s.enemyType = EnemyTypes.TRIPLE_SHOT;

            } else {

                // Regular Ship
                s = new Sprite(type <= 0.485 ? "sprites/new-enemy-1.png" : "sprites/new-enemy-2.png", cache);
                s.x = resolutionX;
                s.y = (int) (Math.random() * (resolutionY - 64));
                s.setActiveAnimation("east", 1000);
                int speed = Math.random() > 0.5d ? -5 : -4;

                double angleChance = Math.random();
                if (angleChance < 0.8d){
                    s.enemyType = EnemyTypes.STANDARD;
                    s.moveSprite(speed, 0);
                }
                else if (angleChance < 0.9d) {
                    s.enemyType = EnemyTypes.BOUNCER;
                    s.moveSprite(-2, 2);
                }
                else {
                    s.enemyType = EnemyTypes.BOUNCER;
                    s.moveSprite(-2, -2);
                }

            }

            if (s.y < minY) s.y = minY;
            if (s.y > maxY) s.y = 300;

            hostileEntities.add(s);

        }

        // Enemy shots!
        for (Sprite enemy : hostileEntities) {
            if (enemy.isAlive) {
                if (Math.random() > 0.985d) {
                    switch (enemy.enemyType) {

                        case TURRET_NORTH: {
                            Sprite s2 = new Sprite("sprites/shot-e-3.png", cache);
                            s2.x = enemy.x;
                            s2.y = enemy.y + 25;
                            s2.setActiveAnimation("east", 1000);
                            s2.moveSprite(-8, 1);
                            hostileProjectile.add(s2);
                            break;
                        }

                        case TURRET_SOUTH: {
                            Sprite s2 = new Sprite("sprites/shot-e-3.png", cache);
                            s2.x = enemy.x;
                            s2.y = enemy.y + 25;
                            s2.setActiveAnimation("east", 1000);
                            s2.moveSprite(-8, -1);
                            hostileProjectile.add(s2);
                            break;
                        }

                        case TRIPLE_SHOT:  {
                            Sprite s = new Sprite("sprites/shot-e-2.png", cache);
                            s.x = enemy.x;
                            s.y = enemy.y + 25;
                            s.setActiveAnimation("east", 1000);
                            s.moveSprite(-8, 0);
                            Sprite s2 = new Sprite("sprites/shot-e-2.png", cache);
                            s2.x = enemy.x;
                            s2.y = enemy.y + 25;
                            s2.setActiveAnimation("east", 1000);
                            s2.moveSprite(-8, 1);
                            Sprite s3 = new Sprite("sprites/shot-e-2.png", cache);
                            s3.x = enemy.x;
                            s3.y = enemy.y + 25;
                            s3.setActiveAnimation("east", 1000);
                            s3.moveSprite(-8, -1);

                            hostileProjectile.add(s);
                            hostileProjectile.add(s2);
                            hostileProjectile.add(s3);

                            break;
                        }
                        default: {

                            Sprite s = new Sprite("sprites/shot-e.png", cache);
                            s.x = enemy.x;
                            s.y = enemy.y + 25;
                            s.setActiveAnimation("east", 1000);
                            s.moveSprite(-8, 0);

                            hostileProjectile.add(s);
                            break;
                        }
                    }
                }
            }
        }

        // Check Collisions!!!!
        Polygon playerShape = new Polygon();
        playerShape.addPoint(playerShip.x + 5, playerShip.y + 11);
        playerShape.addPoint(playerShip.x + 26, playerShip.y + 11);
        playerShape.addPoint(playerShip.x + 61, playerShip.y + 34);
        playerShape.addPoint(playerShip.x + 28, playerShip.y + 48);
        playerShape.addPoint(playerShip.x + 5, playerShip.y + 48);

        if (hasLaser) {
            laserShape = new Rectangle(playerShip.x + 91, playerShip.y + 4, 1280, 60);
        }

        if (!dead) {

            for (Sprite powerUp : powerUps) {

                Ellipse2D powerUpShape = new Ellipse2D.Double(powerUp.x, powerUp.y, 25, 25);

                Area collisionArea = new Area((Shape) powerUpShape.clone());
                collisionArea.intersect(new Area(playerShape));

                if (!collisionArea.isEmpty()) {

                    tripleShot = false;
                    rapidFire = false;
                    nonStop = false;
                    extraShield = false;
                    hasLaser = false;
                    drawLazer = false;
                    sounds.stopSound("laser");

                    if (shieldSoundOn) {
                        sounds.playSound("shield-off", 1);
                        shieldSoundOn = false;
                        sounds.stopSound("shield");
                    }

                    powerUps.remove(powerUp);

                    double whichPowerUp = Math.random();

                    if (whichPowerUp < 0.33d){
                        tripleShot = true;
                    } else if (whichPowerUp < 0.66d) {
                        lazer.setActiveAnimation("east", 5);
                        Timer t = new Timer();
                        t.schedule(new EnableLaser(), 550);
                        drawLazer = true;
                        sounds.playSound("laser", 20);
                    } else {
                        extraShield = true;
                        if (!shieldOn) sounds.playSound("shield", 20);
                        shieldSoundOn = true;
                    }

                    powerUpEnergy = 1000;
                    hasPowerUp = true;
                    sounds.playSound("power", 2);
                }
            }

            ArrayList<Rectangle2D> terrainAreas = new ArrayList<>();

            for (Sprite terrain : terrainSprites) {

                Rectangle2D terrainShape = new Rectangle2D.Double(0, terrain.y, resolutionX, terrain.getFrame().getHeight());
                terrainAreas.add(terrainShape);

                Area collisionArea = new Area((Shape) terrainShape.clone());
                collisionArea.intersect(new Area(playerShape));
                if (!collisionArea.isEmpty()) {
                    lostLife();
                }

            }

            for (Sprite hostileShots : hostileProjectile) {

                Ellipse2D shotShape = new Ellipse2D.Double(hostileShots.x, hostileShots.y, 15, 15);

                boolean destroyed = false;

                // Check is destroyed by laser
                if (hasLaser) {
                    Area laserArea = new Area((Shape) shotShape.clone());
                    laserArea.intersect(new Area(laserShape));
                    if (!laserArea.isEmpty()) {
                        destroyed = true;
                        hostileProjectile.remove(hostileShots);
                    }
                }

                if (!destroyed) {

                    Area collisionArea = new Area((Shape) shotShape.clone());
                    collisionArea.intersect(new Area(playerShape));
                    if (!collisionArea.isEmpty()) {
                        if (shieldOn) {
                            hostileShots.moveSprite(6, 0);
                            friendlyProjectiles.add(hostileShots);
                            hostileProjectile.remove(hostileShots);
                            hostileShots.isBounceBack = true;
                            sounds.playSound("reflect", 1);
                        } else {
                            lostLife();
                        }
                    }
                }
            }

            for (Sprite enemy : hostileEntities) {

                if (enemy.isAlive) {

                    Rectangle enemyShape = new Rectangle(enemy.x + 8, enemy.y + 8, 48, 48);

                    // Check for laser smash
                    boolean destroyed = false;

                    if (hasLaser) {

                        Area laserArea = new Area((Shape) enemyShape.clone());
                        laserArea.intersect(new Area(laserShape));

                        if (!laserArea.isEmpty()) {
                            destroyed = true;
                            killEnemy(enemy, null);
                        }
                    }

                    if (!destroyed) {

                        Area collisionArea = new Area((Shape) enemyShape.clone());
                        collisionArea.intersect(new Area(playerShape));

                        if (!collisionArea.isEmpty()) {
                            if (shieldOn) {
                                killEnemy(enemy, null);
                            } else {
                                lostLife();
                            }
                        }

                        for (Rectangle2D terrain : terrainAreas) {
                            collisionArea = new Area((Shape)terrain.clone() );
                            collisionArea.intersect(new Area(enemyShape));
                            if (!collisionArea.isEmpty()) {
                                enemy.moveSprite(enemy.incX, -1 * enemy.incY);
                            }
                        }

                        for (Sprite shot : friendlyProjectiles) {

                            Ellipse2D projectileShape = new Ellipse2D.Double(shot.x, shot.y, 15, 15);

                            collisionArea = new Area((Shape) projectileShape.clone());
                            collisionArea.intersect(new Area(enemyShape));

                            if (!collisionArea.isEmpty()) {
                                killEnemy(enemy, shot);
                                if (shot.isBounceBack) bounceBacks++;
                                if (bounceBacks >= 5) {
                                    bounceBacks = 0;
                                    multi += 1;
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    private class EnableLaser extends TimerTask {

        @Override
        public void run() {
            hasLaser = true;
            lazer.setActiveAnimation("lazer", 15);
        }

    }

    private void lostLife() {

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

    private void killEnemy(Sprite enemy, Sprite killedBy) {

        sounds.playSound("explode", 1);
        score += 250 * multi;

        if (killedBy != null) {

            if (Math.random() > 0.965d) {
                Sprite newPowerUp = new Sprite("sprites/powerup-1.png", cache);
                newPowerUp.rotated = true;
                newPowerUp.moveSprite(-3, 0);
                newPowerUp.x = enemy.x + 15;
                newPowerUp.y = enemy.y + 15;
                newPowerUp.setActiveAnimation("east", 10);
                powerUps.offer(newPowerUp);
            }

            if (!nonStop) {
                friendlyProjectiles.remove(killedBy);
                killedBy.terminate();
            }

        }

        enemy.isAlive = false;
        enemy.setActiveAnimation("die", 50);

        Timer t = new Timer();
        t.schedule(new RemoveEnemyAfterDuration(enemy), 750);

    }

    private class RemoveEnemyAfterDuration extends TimerTask {
        private Sprite enemy;

        public RemoveEnemyAfterDuration(Sprite enemy) {
            this.enemy = enemy;
        }

        @Override
        public void run() {
            enemy.terminate();
            hostileEntities.remove(enemy);
        }
    }


    private void reset() {
        if (lives > 0) {
            hostileEntities.clear();
            hostileProjectile.clear();
            friendlyEntities.clear();
            playerShip.x = 75;
            playerShip.y = (resolutionY / 2) - (playerShip.getFrame().getHeight() / 2);
            shieldEnergy = 1000;
            playerShip.setActiveAnimation("east", 1000);
            dead = false;
            multi = 1;
            powerUpEnergy = 0;
            hasPowerUp = false;
            tripleShot = false;
            hasLaser = false;
            drawLazer = false;
            extraShield = false;
        } else {
            // Game Over
            Timer t = new Timer();
            t.schedule(new ReturnToTitle(), 3500);
            flashTimer = new Timer();
            flashTimer.schedule(new FlashTimer(), 0, 750);
            gameOver = true;
        }
    }

    public class ReturnToTitle extends TimerTask {

        @Override
        public void run() {
            gameOver = false;
            gameStarted = false;
            canFire = false;
            score = 0;
            lives = 3;
            multi = 1;
            extraLifeScore = 25000;
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

            Properties prop = new Properties();
            prop.setProperty("highscore", String.valueOf(highScore));
            try {
                prop.store(new FileWriter(new File("config.prop")), "");
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
    }

    private class ResetTimer extends TimerTask {
        @Override
        public void run() {
            reset();
        }
    }

    private void checkScores() {

        if (score >= highScore) {
            highScore = score;
        }

        if (score >= extraLifeScore) {
            lives++;
            extraLifeScore += (extraLifeScore * extraLifeMultiple);
            sounds.playSound("powerup", 10);
        }

    }

    private void input() {

        int shipSpeed = 4;

        if (downPressed) playerShip.y += playerShip.y + 64 >= resolutionY ? 0 : shipSpeed;
        if (upPressed) playerShip.y -= playerShip.y <= 0 ? 0 : shipSpeed;
        if (leftPressed) playerShip.x -= playerShip.x <= 0 ? 0 : shipSpeed;
        if (rightPressed) playerShip.x += playerShip.x + 64 >= resolutionX / 2 ? 0 : shipSpeed;

        if (controlPressed || extraShield) {
            if (extraShield && shieldEnergy < 1000) shieldEnergy++;
            shieldOn = (shieldEnergy > 0) || extraShield;
            if (shieldOn) {
                if (!extraShield) shieldEnergy -= 5;
                playerShip.setActiveAnimation("ship-shield", 100);
            } else {
                playerShip.setActiveAnimation("east", 1000);
                if (shieldSoundOn) {
                    sounds.playSound("shield-off", 2);
                    sounds.stopSound("shield");
                    shieldSoundOn = false;
                }
            }
        } else {
            shieldOn = false;
            playerShip.setActiveAnimation("east", 1000);
            if (shieldEnergy < 1000) shieldEnergy += 1;
        }

        if (canFire || (rapidFire && spacePressed)) {

            if (tripleShot) {
                sounds.playSound("triple", 1);
                Sprite northShot = new Sprite("sprites/triple-3.png", cache);
                northShot.x = playerShip.x + playerShip.getFrame().getWidth() - 12;
                northShot.y = playerShip.y + playerShip.getFrame().getHeight() / 2 - 7;
                Sprite southShot = new Sprite("sprites/triple-2.png", cache);
                southShot.x = playerShip.x + playerShip.getFrame().getWidth() - 12;
                southShot.y = playerShip.y + playerShip.getFrame().getHeight() / 2 - 7;
                northShot.setActiveAnimation("east", 1000);
                northShot.moveSprite(8, 1);
                southShot.setActiveAnimation("east", 1000);
                southShot.moveSprite(8, -1);
                friendlyProjectiles.add(northShot);
                friendlyProjectiles.add(southShot);
                Sprite newShot = new Sprite("sprites/triple-1.png", cache);
                newShot.x = playerShip.x + playerShip.getFrame().getWidth() - 12;
                newShot.y = playerShip.y + playerShip.getFrame().getHeight() / 2 - 6;
                newShot.setActiveAnimation("east", 1000);
                newShot.moveSprite(8, 0);
                friendlyProjectiles.add(newShot);
            } else {
                sounds.playSound("shoot", 1);
                Sprite newShot = new Sprite("sprites/shot.png", cache);
                newShot.x = playerShip.x + playerShip.getFrame().getWidth() - 12;
                newShot.y = playerShip.y + playerShip.getFrame().getHeight() / 2 - 6;

                newShot.setActiveAnimation("east", 1000);
                newShot.moveSprite(8, 0);
                friendlyProjectiles.add(newShot);
            }
            score += 5;

            canFire = false;
        }
    }

    protected class KeyInputHandler extends KeyAdapter {

        @Override
        public void keyPressed(KeyEvent e) {

            if (e.getKeyCode() == KeyEvent.VK_ENTER && !gameStarted) {
                flashTimer.cancel();
                gameStarted = true;
            }

            if (e.getKeyCode() == KeyEvent.VK_UP) upPressed = true;
            if (e.getKeyCode() == KeyEvent.VK_DOWN) downPressed = true;
            if (e.getKeyCode() == KeyEvent.VK_LEFT) leftPressed = true;
            if (e.getKeyCode() == KeyEvent.VK_RIGHT) rightPressed = true;
            if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
                controlPressed = true;
                if (shieldEnergy > 0 && !shieldSoundOn) {
                    if (!shieldOn) sounds.playSound("shield", 20);
                    shieldSoundOn = true;
                }
            }
            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                if (!spacePressed) {
                    spacePressed = true;
                    canFire = true;
                }
            }

            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                running = false;
                System.exit(0);
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_UP) upPressed = false;
            if (e.getKeyCode() == KeyEvent.VK_DOWN) downPressed = false;
            if (e.getKeyCode() == KeyEvent.VK_LEFT) leftPressed = false;
            if (e.getKeyCode() == KeyEvent.VK_RIGHT) rightPressed = false;
            if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
                controlPressed = false;
                if (shieldSoundOn && !extraShield) {
                    sounds.playSound("shield-off", 2);
                    sounds.stopSound("shield");
                    shieldSoundOn = false;
                }
            }
            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                spacePressed = false;
            }
        }

        @Override
        public void keyTyped(KeyEvent e) {

        }
    }

    private class FlashTimer extends TimerTask {
        @Override
        public void run() {
            flash = !flash;
        }
    }
}
