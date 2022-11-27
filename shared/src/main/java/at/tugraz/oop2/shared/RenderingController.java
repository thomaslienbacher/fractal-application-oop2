package at.tugraz.oop2.shared;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.Property;
import javafx.scene.canvas.Canvas;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

    private boolean exitThreads = false;
    private JuliaRenderer juliaRenderer;
    private MandelbrotRenderer mandelbrotRenderer;
    private ReentrantLock juliaLock, mandelbrotLock;

    private Thread mandelbrotThread, juliaThread;

    //Temporary debug int
    private int renderCount;

    public RenderingController(DoubleProperty power, IntegerProperty iteration, DoubleProperty mandelbrotX,
                               DoubleProperty mandelbrotY, DoubleProperty mandelbrotZoom, DoubleProperty juliaX,
                               DoubleProperty juliaY, DoubleProperty juliaZoom, Property<ColourModes> colourMode,
                               Property<RenderMode> renderMode, IntegerProperty tasksPerWorker, Property<List<InetSocketAddress>> connections,
                               Canvas mandelbrotCanvas, Canvas juliaCanvas, ReentrantLock juliaLock, ReentrantLock mandelbrotLock) {
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
        this.juliaLock = juliaLock;
        this.mandelbrotLock = mandelbrotLock;
    }

    public void render() {
        renderCount++;
        System.out.println("Rendering...");
        System.out.println("Threads: " + java.lang.Thread.activeCount() + " /Renders: " + renderCount);

        if (mandelbrotRenderer != null) {
            //mandelbrotRenderer.stop();
            mandelbrotThread.interrupt();
        }

        if (juliaRenderer != null) {
            //juliaRenderer.stop();
            juliaThread.interrupt();
        }

        //Render mandelbrot
        mandelbrotRenderer = new MandelbrotRenderer(power.get(), iterations.get(), mandelbrotX.get(),
                mandelbrotY.get(), mandelbrotZoom.get(), colourMode.getValue(), renderMode.getValue(),
                tasksPerWorker.get(), connections.getValue(), mandelbrotCanvas, mandelbrotLock);
        mandelbrotThread = new Thread(mandelbrotRenderer, "mandelbrot-renderer");
        mandelbrotThread.start();

        //Render julia
        juliaRenderer = new JuliaRenderer(power.get(), iterations.get(), juliaX.get(),
                juliaY.get(), juliaZoom.get(), colourMode.getValue(), renderMode.getValue(),
                tasksPerWorker.get(), connections.getValue(), juliaCanvas, juliaLock);

        juliaThread = new Thread(juliaRenderer, "julia-renderer");
        juliaThread.start();

    }


    public void startRendering() {
        Thread mandelbrot = new Thread(() -> {
            while (!exitThreads) {
                var renderer = new MandelbrotRenderer(power.get(), iterations.get(), mandelbrotX.get(),
                        mandelbrotY.get(), mandelbrotZoom.get(), colourMode.getValue(), renderMode.getValue(),
                        tasksPerWorker.get(), connections.getValue(), mandelbrotCanvas, mandelbrotLock);

                Thread t = new Thread(renderer, "mandelbrot-renderer");
                t.start();
                //System.out.println("starting new mandelbrot renderer");

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
            while (!exitThreads) {
                var renderer = new JuliaRenderer(power.get(), iterations.get(), juliaX.get(),
                        juliaY.get(), juliaZoom.get(), colourMode.getValue(), renderMode.getValue(),
                        tasksPerWorker.get(), connections.getValue(), juliaCanvas, juliaLock);

                Thread t = new Thread(renderer, "julia-renderer");
                t.start();
                //System.out.println("Starting new julia renderer");

                try {
                    t.join();
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }, "render-controller-julia");
        julia.start();
    }

    public void stopRendering() {
        //TODO implement, interrupt threads or kill them via flag

        //For on-demand rendering approach
        if (juliaRenderer != null)
            juliaRenderer.stop();

        if (mandelbrotRenderer != null)
            mandelbrotRenderer.stop();

        //For continuous rendering approach
        exitThreads = true;
    }
}
