package at.tugraz.oop2.shared;

import at.tugraz.oop2.shared.exception.InvalidDepthException;
import javafx.scene.canvas.Canvas;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MandelbrotRenderer implements Runnable {

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
    private ReentrantLock canvasLock;

    public MandelbrotRenderer(double power, int iterations, double x, double y, double zoom, ColourModes colourMode, RenderMode renderMode, int tasksPerWorker, List<InetSocketAddress> connections, Canvas canvas, ReentrantLock mandelbrotLock) {
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
        this.canvasLock = mandelbrotLock;
    }

    static class MandelbrotTask implements Callable<SimpleImage> {

        MandelbrotRenderOptions options;

        public MandelbrotTask(MandelbrotRenderOptions options) {
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
            short s = (short) (y % 255);
            short t = (short) (x % 255);
            return new short[]{s, t, (short) Math.abs(s - t)};
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


    @Override
    public void run() {

        if (renderMode == RenderMode.LOCAL) {
            canvasLock.lock();
            ExecutorService executor = null;
            try {
                int width = (int) canvas.getWidth();
                int height = (int) canvas.getHeight();
                // lets utilize all the power
                int nproc = Runtime.getRuntime().availableProcessors();
                executor = Executors.newFixedThreadPool(nproc);

                int nTasks = 1;
                var tasks = new ArrayList<MandelbrotTask>();

                for (int i = 0; i < nTasks; i++) {
                    var opts = new MandelbrotRenderOptions(x, y, width, height, zoom, power, iterations,
                            colourMode, i, nTasks, renderMode);
                    tasks.add(new MandelbrotTask(opts));
                }

                var results = executor.invokeAll(tasks);
                var images = results.stream().map((a) -> {
                    try {
                        return a.get();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).toList();

                var completeImage = new SimpleImage(images);
                executor.shutdown();

                completeImage.copyToCanvas(canvas);
                canvasLock.unlock();
            } catch (InterruptedException ie) {

            } catch (Exception e) {
                canvasLock.unlock();
                if (executor != null)
                    executor.shutdown();
                throw new RuntimeException(e);
            } finally {
                if (canvasLock.isHeldByCurrentThread())
                    canvasLock.unlock();
                if (executor != null)
                    executor.shutdown();
            }
        } else if (renderMode == RenderMode.DISTRIBUTED) {
            throw new RuntimeException("not implemented");
        }
    }
}
