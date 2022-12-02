package at.tugraz.oop2.shared;

public class Complex {

    double real;
    double img;

    public Complex(double real, double img) {
        this.real = real;
        this.img = img;
    }

    public Complex() {
        this(0, 0);
    }

    public Complex(Complex copy) {
        this.real = copy.real;
        this.img = copy.img;
    }
    
    public double radius() {
        return Math.sqrt(radiusSquared());
    }

    public double radiusSquared() {
        return real * real + img * img;
    }

    public Complex pow(double power) {
        // transform to polar coords
        var arg = argument();
        var rad = radius();
        rad = Math.pow(rad, power);
        arg = arg * power;

        return new Complex(rad * Math.cos(arg), rad * Math.sin(arg));
    }

    public Complex add(Complex rhs) {
        var n = new Complex(this);
        n.real += rhs.real;
        n.img += rhs.img;
        return n;
    }

    public double argument() {
        return Math.atan2(img, real);
    }

    @Override
    public String toString() {
        return "Complex{" +
                "r=" + real +
                ", i=" + img +
                '}';
    }
}
