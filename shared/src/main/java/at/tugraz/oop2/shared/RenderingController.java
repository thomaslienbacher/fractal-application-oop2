package at.tugraz.oop2.shared;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.Property;
import javafx.scene.canvas.Canvas;

import java.net.InetSocketAddress;
import java.util.List;

public class RenderingController {

    DoubleProperty power;
    IntegerProperty iterations;

    DoubleProperty mandelbrotX;
    DoubleProperty mandelbrotY;
    DoubleProperty mandelbrotZoom;

    DoubleProperty juliaX;
    DoubleProperty juliaY;
    DoubleProperty juliaZoom;

    Property<ColourModes> colourMode;
    Property<RenderMode> renderMode;
    IntegerProperty tasksPerWorker;
    Property<List<InetSocketAddress>> connections;
    Canvas mandelbrotCanvas; // left canvas
    Canvas juliaCanvas; // right canvas

    public RenderingController(DoubleProperty power, IntegerProperty iteration, DoubleProperty mandelbrotX,
                               DoubleProperty mandelbrotY, DoubleProperty mandelbrotZoom, DoubleProperty juliaX,
                               DoubleProperty juliaY, DoubleProperty juliaZoom, Property<ColourModes> colourMode,
                               Property<RenderMode> renderMode, IntegerProperty tasksPerWorker, Property<List<InetSocketAddress>> connections,
                               Canvas mandelbrotCanvas, Canvas juliaCanvas) {
        this.power = power;
        this.iterations = iteration;
        this.mandelbrotX = mandelbrotX;
        this.mandelbrotY = mandelbrotY;
        this.mandelbrotZoom = mandelbrotZoom;
        this.juliaX = juliaX;
        this.juliaY = juliaY;
        this.juliaZoom = juliaZoom;
        this.colourMode = colourMode;
        this.renderMode = renderMode;
        this.tasksPerWorker = tasksPerWorker;
        this.connections = connections;
        this.mandelbrotCanvas = mandelbrotCanvas;
        this.juliaCanvas = juliaCanvas;
    }


    public void startRendering() {
        Thread mandelbrot = new Thread(() -> {
            while (true) {
                var renderer = new MandelbrotRenderer(power.get(), iterations.get(), mandelbrotX.get(),
                        mandelbrotY.get(), mandelbrotZoom.get(), colourMode.getValue(), renderMode.getValue(),
                        tasksPerWorker.get(), connections.getValue(), mandelbrotCanvas);

                Thread t = new Thread(renderer, "mandelbrot-renderer");
                t.start();
                System.out.println("starting new mandelbrot renderer");

                try {
                    t.join();
                    Thread.sleep(600);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }, "render-controller-mandelbrot");
        mandelbrot.start();

        Thread julia = new Thread(() -> {
            while (true) {
                //TODO implement

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }, "render-controller-julia");
        julia.start();
    }

    public void stopRendering() {
        //TODO implement, interrupt threads or kill them via flag
    }
}
