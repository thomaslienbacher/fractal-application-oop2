package at.tugraz.oop2.shared;

import at.tugraz.oop2.shared.exception.InvalidDepthException;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.canvas.Canvas;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MandelbrotRenderer extends Service<SimpleImage> {

    static class MandelbrotTask implements Callable<SimpleImage> {

        MandelbrotRenderOptions options;
        int renderId;

        ColourModes colourMode;

        SpaceTransform transform;

        public MandelbrotTask(int renderId, ColourModes colourMode, MandelbrotRenderOptions options) {
            this.renderId = renderId;
            this.colourMode = colourMode;
            this.options = options;
            this.transform = new SpaceTransform(options.width, options.height, options.zoom, options.centerX, options.centerY);
            System.out.printf("|%04x| %s\n", renderId, transform);
        }

        @Override
        public SimpleImage call() throws InvalidDepthException {
            var img = new SimpleImage(options.width, getImageHeight());

            System.out.printf("|%04x| [%d] %3d (total %3d) %s\n",
                    this.renderId, options.fragmentNumber, getImageHeight(), options.height, options);

            for (int y = 0; y < getImageHeight(); y++) {
                for (int x = 0; x < options.width; x++) {
                    var p = transform.convert(x, y * options.totalFragments + options.fragmentNumber);
                    int iterationsHeld = calcIterations(p);
                    img.setPixel(x, y, colourMode.getPixel(iterationsHeld, options.iterations));
                }
            }

            return img;
        }

        //TODO: change to early return
        private int calcIterations(Complex c) {
            int iterationsHeld = -1;

            var z = new Complex();

            if (c.radius() < 2) {
                for (int i = 0; i < options.iterations; i++) {
                    z = z.pow(options.power);
                    z = z.add(c);

                    if (z.radius() >= 2.0) {
                        iterationsHeld = i;
                        break;
                    }
                }
            } else {
                iterationsHeld = 0;
            }

            return iterationsHeld;
        }

        private int getImageHeight() {
            int h = options.height / options.totalFragments;

            if (options.fragmentNumber < options.height % options.totalFragments) {
                h++;
            }

            return h;
        }
    }

    double power;
    int iterations;
    double x;
    double y;
    double zoom;
    ColourModes colourMode;
    RenderMode renderMode;
    int tasksPerWorker;
    List<InetSocketAddress> connections;
    int width, height;

    public MandelbrotRenderer(double power, int iterations, double x, double y, double zoom, ColourModes colourMode, RenderMode renderMode, int tasksPerWorker, List<InetSocketAddress> connections, Canvas canvas) {
        this.power = power;
        this.iterations = iterations;
        this.x = x;
        this.y = y;
        this.zoom = zoom;
        this.colourMode = colourMode;
        this.renderMode = renderMode;
        this.tasksPerWorker = tasksPerWorker;
        this.connections = connections;
    }

    @Override
    public Task<SimpleImage> createTask() {
        return new Task<SimpleImage>() {
            @Override
            protected SimpleImage call() {
                return renderLocal();
            }
        };
    }

    public void setBounds(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public SimpleImage renderLocal() {
        int renderId = (int) (Math.random() * Short.MAX_VALUE);
        System.out.printf("Rendering Mandelbrot locally |%04x|\n", renderId);
        ExecutorService executor = null;
        try {
            // lets utilize all the power
            int nproc = Runtime.getRuntime().availableProcessors();
            executor = Executors.newFixedThreadPool(nproc);

            int nTasks = nproc;
            var tasks = new ArrayList<MandelbrotTask>();

            for (int i = 0; i < nTasks; i++) {
                var opts = new MandelbrotRenderOptions(x, y, width, height, zoom, power, iterations,
                        colourMode, i, nTasks, renderMode);
                tasks.add(new MandelbrotTask(renderId, colourMode, opts));
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

            return completeImage;
        } catch (CancellationException e) {

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (executor != null) {
                executor.shutdownNow();
            }
        }
        return null;
    }
}
