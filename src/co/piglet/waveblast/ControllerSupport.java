package co.piglet.waveblast;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Version;

/**
 * Provides support for the standard 360 controller
 * A = 0
 * B = 1
 * X = 2
 * Y = 3
 */

public class ControllerSupport
{
    private final int A_BUTTON = 0;
    private final int B_BUTTON = 1;
    private final int C_BUTTON = 2;
    private final int X_BUTTON = 3;
    private final int Y_BUTTON = 4;
    private final int Z_BUTTON = 5;
    private final int L_BUTTON = 6;
    private final int R_BUTTON = 7;
    private final int START_BUTTON = 8;
    private Controller xboxController;
    private Component dPad;
    private Component xAxis;
    private Component yAxis;
    private Component[] buttons;

    public ControllerSupport()
    {
        xboxController = getXboxController();
        if (xboxController == null)
        {
            System.out.println("No Xbox controller detected, disabling game pad support");
        }
        else
        {
            System.out.println("360 Controller Found");

            dPad = xboxController.getComponent(Component.Identifier.Axis.POV);
            xAxis = xboxController.getComponent(Component.Identifier.Axis.X);
            yAxis = xboxController.getComponent(Component.Identifier.Axis.Y);
            buttons = new Component[10];
            buttons[A_BUTTON] = xboxController.getComponent(Component.Identifier.Button._0);
            buttons[B_BUTTON] = xboxController.getComponent(Component.Identifier.Button._1);
            buttons[C_BUTTON] = xboxController.getComponent(Component.Identifier.Button._2);
            buttons[X_BUTTON] = xboxController.getComponent(Component.Identifier.Button._3);
            buttons[Y_BUTTON] = xboxController.getComponent(Component.Identifier.Button._4);
            buttons[Z_BUTTON] = xboxController.getComponent(Component.Identifier.Button._5);
            buttons[L_BUTTON] = xboxController.getComponent(Component.Identifier.Button._6);
            buttons[R_BUTTON] = xboxController.getComponent(Component.Identifier.Button._7);
            buttons[START_BUTTON] = xboxController.getComponent(Component.Identifier.Button._8);
        }
    }

    private Controller getXboxController()
    {
        // Detect if we have an Xbox controller present

        System.out.println("Checking Controller Support....");
        ControllerEnvironment ce = ControllerEnvironment.getDefaultEnvironment();
        Controller[] cs = ce.getControllers();

        for (Controller c : cs)
        {
            if (c.getName().contains("playsega"))
            {
                if (c.getType() == Controller.Type.GAMEPAD)
                {
                    return c;
                }
            }
        }

        return null;
    }

    public float getDPadPosition()
    {
        if (xboxController != null)
        {
            xboxController.poll();
            return dPad.getPollData();
        }
        return 0.0f;
    }

    public float getXAxisPosition()
    {
        if (xboxController != null)
        {
            xboxController.poll();
            return xAxis.getPollData();
        }
        return 0.0f;
    }

    public float getYAxisPosition()
    {
        if (xboxController != null)
        {
            xboxController.poll();
            return yAxis.getPollData();
        }
        return 0.0f;
    }

    public float getButton(int button)
    {
        xboxController.poll();
        return buttons[button].getPollData();
    }
}
