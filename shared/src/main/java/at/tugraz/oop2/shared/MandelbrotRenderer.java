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

        double negX;
        double posX;
        double negY;
        double posY;

        ColourModes colourMode;

        public MandelbrotTask(int renderId, ColourModes colourMode, MandelbrotRenderOptions options) {
            this.renderId = renderId;
            this.colourMode = colourMode;
            this.options = options;

            double w = options.width;
            double h = options.height;

            //TODO: use correct zoom calculation see README
            double xDist = (w / 2) / options.zoom + options.centerX;
            double yDist = (h / 2) / options.zoom + options.centerY;

            this.negX = -xDist;
            this.posX = xDist;
            this.negY = -yDist;
            this.posY = yDist;

            System.out.printf("|%04x| x: %.4f %.4f y: %.4f %.4f\n", renderId, negX, posX, negY, posY);
        }

        @Override
        public SimpleImage call() throws InvalidDepthException {
            var img = new SimpleImage(options.width, getImageHeight());

            System.out.printf("|%04x| [%d] %3d (total %3d) %s\n",
                    this.renderId, options.fragmentNumber, getImageHeight(), options.height, options);

            for (int y = 0; y < getImageHeight(); y++) {
                for (int x = 0; x < options.width; x++) {
                    double cx = convertCoordX(x);
                    double cy = convertCoordY(y * options.totalFragments + options.fragmentNumber);
                    short[] pix = getPixel(cx, cy);
                    img.setPixel(x, y, pix);
                }
            }

            return img;
        }

        /**
         * Converts the image x coord to the complex plane
         *
         * @param x range [0; options.width)
         * @return x on complex plane
         */
        private double convertCoordX(int x) {
            double scalar = x;
            //TODO: check is -1 really needed here
            scalar /= (options.width - 1);
            return MathUtils.interpolate(negX, posX, scalar);
        }

        /**
         * Converts the image y coord to the complex plane
         *
         * @param y range [0; options.height)
         * @return y on the complex plane
         */
        private double convertCoordY(int y) {
            double scalar = y;
            //TODO: check is -1 really needed here
            scalar /= (options.height - 1);
            return MathUtils.interpolate(negY, posY, scalar);
        }

        /**
         * Returns the color of the pixel
         *
         * @param x real value on complex plane
         * @param y img value complex plane
         * @return pixel array
         */
        private short[] getPixel(double x, double y) {
            var p = new Complex(x, y);

            int iterationsHeld = -1;

            var z = new Complex();

            if (p.radius() < 2) {
                for (int i = 0; i < options.iterations; i++) {
                    z = z.pow(options.power);
                    z = z.add(p);

                    if (z.radius() >= 2.0) {
                        iterationsHeld = i;
                        break;
                    }
                }
            } else {
                iterationsHeld = 0;
            }

            switch (colourMode) {
                case BLACK_WHITE -> {
                    return colorBlackWhite(iterationsHeld);
                }
                case COLOUR_FADE -> {
                    throw new UnsupportedOperationException("only black white color mode works");
                }
                default -> throw new UnsupportedOperationException("color mode not supported: " + colourMode.name());
            }
        }

        private short[] colorBlackWhite(int iterationsHeld) {
            if (iterationsHeld == -1) {
                return new short[]{0, 0, 0};
            } else {
                return new short[]{255, 255, 255};
            }
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
