package ftb.lib.api.gui;

import org.lwjgl.input.Mouse;

/**
 * Created by LatvianModder on 18.01.2016.
 */
public class MouseLM {

    public int x, y, px, py, dx, dy;
    public int wheel, dwheel;
    public int lastClickButton;
    public boolean lastClickState;
    public int lastClickX, lastClickY;

    public void onUpdate(int mx, int my) {
        px = x;
        py = y;

        x = mx;
        y = my;

        dx = x - px;
        dy = y - py;

        dwheel = Mouse.getDWheel();
        wheel += dwheel;
    }

    public void onClicked(int b, boolean s) {
        lastClickButton = b;
        lastClickState = s;
        lastClickX = x;
        lastClickY = y;
    }

    public boolean isInside(int ax, int ay, int width, int height) {
        return x >= ax && y >= ay && x < ax + width && y < ay + height;
    }
}
