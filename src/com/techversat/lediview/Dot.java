package com.techversat.lediview;


public class Dot {
    private final float x, y;
    private int color;
    private int diameter;

    /**
     * @param x horizontal coordinate.
     * @param y vertical coordinate.
     * @param color the color.
     * @param diameter dot diameter.
     */
    public Dot(float x, float y, int color, int diameter) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.diameter = diameter;
    }

    /** @return the horizontal coordinate. */
    public float getX() { return x; }

    /** @return the vertical coordinate. */
    public float getY() { return y; }

    /** @return the color. */
    public int getColor() { return color; }

    /** @return the dot diameter. */
    public int getDiameter() { return diameter; }
    
    public void setColor(int color) { this.color = color; }
    public void setDiameter(int diameter) { this.diameter = diameter; }
    //public void setPos(int xpos, int ypos) { this.xpos = xpos; this.ypos = ypos; }
}
