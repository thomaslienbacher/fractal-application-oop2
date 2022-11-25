package at.tugraz.oop2.shared;

import at.tugraz.oop2.shared.exception.InvalidDepthException;
import javafx.scene.canvas.Canvas;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JuliaRenderer implements Runnable {

    double power;
    int iterations;
    double x;
    double y;
    double zoom;
    ColourModes colourMode;
    RenderMode renderMode;
    int tasksPerWorker;
    List<InetSocketAddress> connections;
    Canvas canvas;

    public JuliaRenderer(double power, int iterations, double x, double y, double zoom, ColourModes colourMode, RenderMode renderMode, int tasksPerWorker, List<InetSocketAddress> connections, Canvas canvas) {
        this.power = power;
        this.iterations = iterations;
        this.x = x;
        this.y = y;
        this.zoom = zoom;
        this.colourMode = colourMode;
        this.renderMode = renderMode;
        this.tasksPerWorker = tasksPerWorker;
        this.connections = connections;
        this.canvas = canvas;
    }

    @Override
    public void run() {
        if (renderMode == RenderMode.LOCAL) {
            RenderLocal();
        } else if (renderMode == RenderMode.DISTRIBUTED) {
            throw new RuntimeException("not implemented");
        }
    }

    //Renders local, blocks until finished
    private void RenderLocal()
    {
        int width = (int) canvas.getWidth();
        int height = (int) canvas.getHeight();

        // lets utilize all the power
        int nproc = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(nproc);

        int nTasks = 1;
        var tasks = new ArrayList<JuliaRenderer.JuliaTask>();

        for (int i = 0; i < nTasks; i++) {
            //Todo change constantX and constantY (I dont know what they are so set to 0 for now)
            var opts = new JuliaRenderOptions(x, y, width, height, zoom, power, iterations, 0, 0,
                    colourMode, i, nTasks, renderMode);
            tasks.add(new JuliaRenderer.JuliaTask(opts));
        }

        try {
            var results = executor.invokeAll(tasks);
            var images = results.stream().map((a) -> {
                try {
                    return a.get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).toList();

            var completeImage = new SimpleImage(images);
            completeImage.copyToCanvas(canvas);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static class JuliaTask implements Callable<SimpleImage> {

        JuliaRenderOptions options;

        public JuliaTask(JuliaRenderOptions options) {
            this.options = options;
        }

        @Override
        public SimpleImage call() throws InvalidDepthException {
            /*System.out.printf("[%d] from inkl %d to exkl %d (total  %d) " +
                            "%s\n",
                    options.fragmentNumber, getHeightStart(), getHeightEnd(), options.height,
                    options.toString());*/

            int imgHeight = getHeightEnd() - getHeightStart();
            var img = new SimpleImage(options.width, imgHeight);

            for (int y = 0; y < imgHeight; y++) {
                for (int x = 0; x < options.width; x++) {
                    short[] pix = getPixel(x, y);
                    img.setPixel(x, y, pix);
                }
            }

            return img;
        }

        private short[] getPixel(int x, int y) {
            //TODO Maths
            //short s = (short) (y % 255);
            //short t = (short) (x % 255);
            //return new short[]{s, t, (short) Math.abs(s - t)};
            return new short[]{0, 255, 255};
        }

        private int getHeightStart() {
            int perTask = options.height / options.totalFragments;
            return perTask * options.fragmentNumber;
        }

        private int getHeightEnd() {
            int perTask = options.height / options.totalFragments;

            if (options.fragmentNumber == options.totalFragments - 1) {
                return options.height;
            }

            return perTask * (options.fragmentNumber + 1);
        }
    }

}
