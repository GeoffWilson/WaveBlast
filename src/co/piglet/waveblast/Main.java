package co.piglet.waveblast;

import java.awt.*;

public class Main implements Runnable {
    public void run() {

    }

    public static void main(String[] args) throws AWTException {
        boolean fullScreen = false;
        int x = 1280;
        int y = 720;

        try {
            if (args.length > 0) {
                fullScreen = args[0].equals("fullscreen");
                x = Integer.parseInt(args[1]);
                y = Integer.parseInt(args[2]);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        new Main(fullScreen, x, y);

    }

    public Main() {

    }

    public Main(boolean fullScreen, int x, int y) throws AWTException {
        // Do pre-init here
        GameEngine g = new GameEngine(fullScreen, x, y);

        g.loadLevel();
        g.masterLoop();

        //System.exit(0);
    }
}
