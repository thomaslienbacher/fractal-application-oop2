package at.tugraz.oop2.shared;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class JuliaRenderOptions extends FractalRenderOptions {
    private double constantX;
    private double constantY;

    public JuliaRenderOptions(double centerX, double centerY, int width, int height, double zoom, double power, int iterations, double constantX, double constantY, ColourModes mode, int fragmentNumber, int totalFragments, RenderMode renderMode) {
        super(centerX, centerY, width, height, zoom, power, iterations, FractalType.JULIA, mode, 0, totalFragments, fragmentNumber, renderMode);
        this.constantX = constantX;
        this.constantY = constantY;
    }

    public JuliaRenderOptions(double centerX, double centerY, int width, int height, double zoom, double power, int iterations, double constantX, double constantY, ColourModes mode, RenderMode renderMode) {
        this(centerX, centerY, width, height, zoom, power, iterations, constantX, constantY, mode, 0, 1, renderMode);
    }


}
