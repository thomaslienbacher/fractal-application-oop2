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

public class JuliaRenderer extends Service<SimpleImage> {

    private static final boolean DEBUG_PRINT = false;

    static class JuliaTask implements Callable<SimpleImage> {

        JuliaRenderOptions options;
        int renderId;

        ColourModes colourMode;

        SpaceTransform transform;

        public JuliaTask(int renderId, ColourModes colourMode, JuliaRenderOptions options) {
            this.renderId = renderId;
            this.colourMode = colourMode;
            this.options = options;
            this.transform = new SpaceTransform(options.width, options.height, options.zoom, options.centerX, options.centerY);

            if (DEBUG_PRINT) {
                System.out.printf("|%04x| %s\n", renderId, transform);
            }
        }

        @Override
        public SimpleImage call() throws InvalidDepthException {
            var img = new SimpleImage(options.width, getImageHeight());

            if (DEBUG_PRINT) {
                System.out.printf("|%04x| [%d] %3d (total %3d) %s\n", this.renderId, options.fragmentNumber, getImageHeight(), options.height, options);
            }

            for (int y = 0; y < getImageHeight(); y++) {
                for (int x = 0; x < options.width; x++) {
                    var p = transform.convert(x, y * options.totalFragments + options.fragmentNumber);
                    int iterationsHeld = calcIterations(p);
                    img.setPixel(x, y, colourMode.getPixel(iterationsHeld, options.iterations));
                }
            }

            return img;
        }

        private int calcIterations(Complex c) {
            if (c.radiusSquared() > 4) {
                return 0;
            }

            var z = new Complex(c);
            var addConst = new Complex(options.getConstantX(), options.getConstantY());

            for (int i = 0; i < options.iterations; i++) {
                z = z.pow(options.power);
                z = z.add(addConst);

                if (z.radiusSquared() >= 4.0) {
                    return i;
                }
            }

            return -1;
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

    double constantX;

    double constantY;

    public JuliaRenderer(double power, int iterations, double x, double y, double zoom, double constantX, double constantY, ColourModes colourMode, RenderMode renderMode, int tasksPerWorker, List<InetSocketAddress> connections, Canvas canvas) {
        this.power = power;
        this.iterations = iterations;
        this.x = x;
        this.y = y;
        this.zoom = zoom;
        this.constantX = constantX;
        this.constantY = constantY;
        this.colourMode = colourMode;
        this.renderMode = renderMode;
        this.tasksPerWorker = tasksPerWorker;
        this.connections = connections;
    }

    public void setBounds(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public Task<SimpleImage> createTask() {
        return new Task<SimpleImage>() {
            @Override
            protected SimpleImage call() throws Exception {
                return renderLocal();
            }
        };
    }

    //Renders local, blocks until finished
    private SimpleImage renderLocal() {
        // this function is almost the same as in MandelbrotRenderer, maybe some deduplication would be good
        int renderId = (int) (Math.random() * Short.MAX_VALUE);

        if (DEBUG_PRINT) {
            System.out.printf("Rendering Julia locally |%04x|\n", renderId);
        }

        ExecutorService executor = null;
        try {
            // lets utilize all the power
            int nproc = Runtime.getRuntime().availableProcessors();
            executor = Executors.newFixedThreadPool(nproc);

            int nTasks = nproc;
            var tasks = new ArrayList<JuliaTask>();

            for (int i = 0; i < nTasks; i++) {
                var opts = new JuliaRenderOptions(x, y, width, height, zoom, power, iterations, constantX, constantY, colourMode, i, nTasks, renderMode);
                tasks.add(new JuliaRenderer.JuliaTask(renderId, colourMode, opts));
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
            if (executor != null) executor.shutdown();
        }
        return null;
    }
}
