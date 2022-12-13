package at.tugraz.oop2.shared;

public enum ColourModes {
    BLACK_WHITE,
    COLOUR_FADE,
    GRAY_SCALE;

    /**
     * Returns the color of the pixel according to the color mode
     *
     * @param iterationsHeld iterations the pixel held or -1 for infinity
     * @param maxIterations  maximum iterations in a calculation
     * @return pixel array in RGB format
     */
    public short[] getPixel(int iterationsHeld, int maxIterations) {
        switch (this) {
            case BLACK_WHITE -> {
                return colorBlackWhite(iterationsHeld);
            }
            case COLOUR_FADE -> {
                return colorInterpolatedRedBlue(iterationsHeld, maxIterations);
            }
            case GRAY_SCALE -> {
                return colorGrayscale(iterationsHeld, maxIterations);
            }
            default -> throw new UnsupportedOperationException("color mode not supported: " + this.name());
        }
    }

    private short[] colorBlackWhite(int iterationsHeld) {
        if (iterationsHeld == -1) {
            return new short[]{0, 0, 0};
        } else {
            return new short[]{255, 255, 255};
        }
    }

    private short[] colorInterpolatedRedBlue(int iterationsHeld, int maxIterations) {
        if (iterationsHeld == -1) {
            return new short[]{0, 0, 0};
        }

        double s = (double) iterationsHeld / (double) maxIterations;
        short r = MathUtils.interpolate((short) 0, (short) 255, s);
        short b = MathUtils.interpolate((short) 255, (short) 0, s);
        return new short[]{r, 0, b};
    }

    private short[] colorGrayscale(int iterationsHeld, int maxIterations) {
        if (iterationsHeld == -1) {
            return new short[]{255, 255, 255};
        }

        double s = (double) iterationsHeld / (double) maxIterations;
        short r = MathUtils.interpolate((short) 0, (short) 255, s);
        return new short[]{r, r, r};
    }
}
