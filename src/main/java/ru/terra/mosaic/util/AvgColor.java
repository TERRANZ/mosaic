package ru.terra.mosaic.util;

/**
 * Date: 21.07.15
 * Time: 16:32
 */
public class AvgColor implements Cloneable {
    public Float r = new Float(0);
    public Float g = new Float(0);
    public Float b = new Float(0);
    public Float a = new Float(0);

    public AvgColor() {
    }

    public AvgColor(Float r, Float g, Float b, Float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public AvgColor(double r, double g, double b, double a) {
        this.r = new Float(r);
        this.g = new Float(g);
        this.b = new Float(b);
        this.a = new Float(a);
    }

    public AvgColor cl() {
        return new AvgColor(r, g, b, a);
    }
}
