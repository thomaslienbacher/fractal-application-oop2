package at.tugraz.oop2.shared;


/**
 * This class is used to transform image coords to complex plane coords
 */
public class SpaceTransform {

    double scaleX;
    double scaleY;
    double offsetX;
    double offsetY;

    public SpaceTransform(int imgWidth, int imgHeight, double zoom, double centerX, double centerY) {
        double wcomp = Math.pow(2.0, 2.0 - zoom);
        double hcomp = ((double) imgHeight / (double) imgWidth) * Math.pow(2.0, 2.0 - zoom);

        this.scaleX = wcomp / ((double) imgWidth - 1);
        this.scaleY = hcomp / ((double) imgHeight - 1);
        this.offsetX = centerX - wcomp / 2.0;
        this.offsetY = centerY - hcomp / 2.0;
    }

    /**
     * Transform image / screen coords to the point on the complex plane
     *
     * @param x image x in range [0, width - 1]
     * @param y image y in range [0, height - 1]
     * @return coord on complex plane
     */
    public Complex convert(int x, int y) {
        return new Complex(x * scaleX + offsetX, y * scaleY + offsetY);
    }

    public double dragDistanceX(double pixelDist) {
        return pixelDist * scaleX;
    }

    public double dragDistanceY(double pixelDist) {
        return pixelDist * scaleY;
    }

    @Override
    public String toString() {
        return String.format("SpaceTransform{scaleX=%.3f, scaleY=%.3f, offsetX=%.3f, offsetY=%.3f}",
                scaleX, scaleY, offsetX, offsetY);
    }
}
