package at.tugraz.oop2.shared;

public enum ColourModes {
    BLACK_WHITE,
    COLOUR_FADE;

    /**
     * Returns the color of the pixel according to the color mode
     *
     * @param iterationsHeld iterations the pixel held or -1 for infinity
     * @param maxIterations  maximum iterations in a calculation
     * @return
     */
    public short[] getPixel(int iterationsHeld, int maxIterations) {
        switch (this) {
            case BLACK_WHITE -> {
                return colorBlackWhite(iterationsHeld);
            }
            case COLOUR_FADE -> {
                throw new UnsupportedOperationException("only black white color mode works");
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
}
