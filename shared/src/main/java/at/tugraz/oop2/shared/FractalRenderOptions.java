package at.tugraz.oop2.shared;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public abstract class FractalRenderOptions implements Serializable {
    protected double centerX;
    protected double centerY;
    protected int width;
    protected int height;
    protected double zoom;
    protected double power;
    protected int iterations;
    protected FractalType type;
    protected ColourModes mode;
    protected long requestId;
    protected int totalFragments;
    protected int fragmentNumber;
    private RenderMode renderMode;
}
