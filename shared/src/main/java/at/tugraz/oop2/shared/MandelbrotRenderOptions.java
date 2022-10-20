package at.tugraz.oop2.shared;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MandelbrotRenderOptions extends FractalRenderOptions {


    public MandelbrotRenderOptions(double centerX, double centerY, int width, int height, double zoom, double power, int iterations, ColourModes mode, int fragmentNumber, int totalFragments, RenderMode renderMode) {
        super(centerX, centerY, width, height, zoom, power, iterations, FractalType.MANDELBROT, mode, 0, totalFragments, fragmentNumber, renderMode);
    }

    public MandelbrotRenderOptions(double centerX, double centerY, int width, int height, double zoom, double power, int iterations, ColourModes mode, RenderMode renderMode) {
        this(centerX, centerY, width, height, zoom, power, iterations, mode, 0, 1, renderMode);
    }

}
