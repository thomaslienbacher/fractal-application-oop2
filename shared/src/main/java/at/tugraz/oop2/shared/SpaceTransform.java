package at.tugraz.oop2.shared;


/**
 * This class is used to transform image coords to complex plane coords
 */
public class SpaceTransform {
    double negX;
    double posX;
    double negY;
    double posY;

    double imgWidth;
    double imgHeight;

    public SpaceTransform(int imgWidth, int imgHeight, double zoom, double centerX, double centerY) {
        this.imgWidth = imgWidth;
        this.imgHeight = imgHeight;

        //TODO: use correct zoom calculation see README
        double xDist = (this.imgWidth / 2) / zoom + centerX;
        double yDist = (this.imgHeight / 2) / zoom + centerY;

        //double wcomp = Math.pow(2.0, 2.0 - zoom);
        //double hcomp = (this.imgHeight / this.imgWidth) * Math.pow(2.0, 2.0 - zoom);

        this.negX = -xDist;
        this.posX = xDist;
        this.negY = -yDist;
        this.posY = yDist;
    }

    /**
     * Transform image / screen coords to the point on the complex plane
     *
     * @param x image x in range [0, width - 1]
     * @param y image y in range [0, height - 1]
     * @return coord on complex plane
     */
    public Complex convert(int x, int y) {
        return new Complex(convertCoordX(x), convertCoordY(y));
    }

    /**
     * Converts the image x coord to the complex plane
     *
     * @param x range [0; options.width)
     * @return x on complex plane
     */
    private double convertCoordX(int x) {
        double scalar = x;

        scalar /= (imgWidth - 1);
        return MathUtils.interpolate(negX, posX, scalar);
    }

    /**
     * Converts the image y coord to the complex plane
     *
     * @param y range [0; options.height)
     * @return y on the complex plane
     */
    private double convertCoordY(int y) {
        double scalar = y;
        scalar /= (imgHeight - 1);
        return MathUtils.interpolate(negY, posY, scalar);
    }

    @Override
    public String toString() {
        return String.format("SpaceTransform{negX=%.3f, posX=%.3f, negY=%.3f, posY=%.3f}",
                negX, posX, negY, posY);
    }
}
