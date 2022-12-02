package at.tugraz.oop2.shared;

public class MathUtils {

    /**
     * Linear interpolation
     *
     * @param a start = 0
     * @param b end = 1
     * @param i scalar between 0 and 1
     * @return interpolated value
     */
    public static double interpolate(double a, double b, double i) {
        assert i <= 1.0;
        assert i >= 0.0;
        return a + (b - a) * i;
    }

    public static short interpolate(short a, short b, double i) {
        assert i <= 1.0;
        assert i >= 0.0;
        return (short) interpolate((double) a, (double) b, i);
    }
}
