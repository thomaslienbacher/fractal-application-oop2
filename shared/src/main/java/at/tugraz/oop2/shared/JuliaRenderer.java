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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class JuliaRenderer extends Service<SimpleImage> {

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
        ExecutorService executor = null;
        try {

            // lets utilize all the power
            int nproc = Runtime.getRuntime().availableProcessors();
            executor = Executors.newFixedThreadPool(nproc);

            int nTasks = 1;
            var tasks = new ArrayList<JuliaRenderer.JuliaTask>();

            for (int i = 0; i < nTasks; i++) {
                //Todo change constantX and constantY (I dont know what they are so set to 0 for now)
                var opts = new JuliaRenderOptions(x, y, width, height, zoom, power, iterations, 0, 0,
                        colourMode, i, nTasks, renderMode);
                tasks.add(new JuliaRenderer.JuliaTask(opts));
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
            if (executor != null)
                executor.shutdown();
        }
        return null;
    }


    static class JuliaTask implements Callable<SimpleImage> {

        JuliaRenderOptions options;

        public JuliaTask(JuliaRenderOptions options) {
            this.options = options;
        }

        @Override
        public SimpleImage call() throws InvalidDepthException {
            //TODO: fix interleaving problem
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
            short r = (short) (y % 255);
            short g = (short) (x % 255);
            short b = (short) (x + y % 255);
            return new short[]{r, g, b};
            //return new short[]{0, 255, 255};
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
