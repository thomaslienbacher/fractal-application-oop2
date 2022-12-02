package at.tugraz.oop2.gui;

import at.tugraz.oop2.shared.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class FractalApplication extends Application {

    private GridPane mainPane;
    private Canvas rightCanvas;
    private Canvas leftCanvas;
    private GridPane controlPane;

    //TODO: check if default values of properties are correct

    private IntegerProperty iterations = new SimpleIntegerProperty(128);

    private DoubleProperty power = new SimpleDoubleProperty(2.0);

    private DoubleProperty mandelbrotX = new SimpleDoubleProperty(0.0);

    private DoubleProperty mandelbrotY = new SimpleDoubleProperty(0.0);

    private DoubleProperty mandelbrotZoom = new SimpleDoubleProperty(140.0);

    private DoubleProperty juliaX = new SimpleDoubleProperty(0.0);

    private DoubleProperty juliaY = new SimpleDoubleProperty(0.0);

    private DoubleProperty juliaZoom = new SimpleDoubleProperty(1.0);

    private Property<ColourModes> colourMode = new SimpleObjectProperty<>(ColourModes.BLACK_WHITE);

    private Property<RenderMode> renderMode = new SimpleObjectProperty<>(RenderMode.LOCAL);

    private IntegerProperty tasksPerWorker = new SimpleIntegerProperty(1);

    private Property<List<InetSocketAddress>> connections = new SimpleObjectProperty<>(new ArrayList<>(10));

    private DoubleProperty leftHeight = new SimpleDoubleProperty();

    private DoubleProperty leftWidth = new SimpleDoubleProperty();

    private DoubleProperty rightHeight = new SimpleDoubleProperty();

    private DoubleProperty rightWidth = new SimpleDoubleProperty();

    private boolean windowClosed = false;

    Service<SimpleImage> mandelbrotRenderService, juliaRenderService;

    private void updateSizes() {
        if (windowClosed) {
            return;
        }

        Bounds leftSize = mainPane.getCellBounds(0, 0);

        leftCanvas.widthProperty().set(leftSize.getWidth());
        leftCanvas.heightProperty().set(leftSize.getHeight());

        leftCanvas.resize(leftSize.getWidth(), leftSize.getWidth());

        Bounds rightSize = mainPane.getCellBounds(1, 0);

        rightCanvas.widthProperty().set(rightSize.getWidth());
        rightCanvas.heightProperty().set(rightSize.getHeight());

        rightCanvas.resize(rightSize.getWidth(), rightSize.getWidth());

        restartServices();
    }

    private void restartServices() {
        if (mandelbrotRenderService != null && mandelbrotRenderService.isRunning()) {
            mandelbrotRenderService.cancel();
        }
        MandelbrotRenderer mandelbrotRenderer = new MandelbrotRenderer(power.get(), iterations.get(), mandelbrotX.get(),
                mandelbrotY.get(), mandelbrotZoom.get(), colourMode.getValue(), renderMode.getValue(),
                tasksPerWorker.get(), connections.getValue(), leftCanvas);
        mandelbrotRenderer.setBounds((int) leftCanvas.getWidth(), (int) leftCanvas.getHeight());
        mandelbrotRenderService = new Service<SimpleImage>() {
            @Override
            protected Task<SimpleImage> createTask() {
                return mandelbrotRenderer.createTask();
            }
        };
        mandelbrotRenderService.setOnSucceeded(e -> mandelbrotRenderFinished(mandelbrotRenderService.getValue()));
        mandelbrotRenderService.start();


        if (juliaRenderService != null && juliaRenderService.isRunning()) {
            juliaRenderService.cancel();
        }
        JuliaRenderer juliaRenderer = new JuliaRenderer(power.get(), iterations.get(), juliaX.get(),
                juliaY.get(), juliaZoom.get(), colourMode.getValue(), renderMode.getValue(),
                tasksPerWorker.get(), connections.getValue(), rightCanvas);
        juliaRenderer.setBounds((int) rightCanvas.getWidth(), (int) rightCanvas.getHeight());
        juliaRenderService = new Service<SimpleImage>() {
            @Override
            protected Task<SimpleImage> createTask() {
                return juliaRenderer.createTask();
            }
        };
        juliaRenderService.setOnSucceeded(e -> juliaRenderFinished(juliaRenderService.getValue()));
        juliaRenderService.start();
    }

    public void mandelbrotRenderFinished(SimpleImage image) {
        if (image != null)
            image.copyToCanvas(leftCanvas);
        else //Re-draw on fail?
            updateSizes();
    }

    public void juliaRenderFinished(SimpleImage image) {
        if (image != null)
            image.copyToCanvas(rightCanvas);
        else //Re-draw on fail?
            updateSizes();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        mainPane = new GridPane();

        leftCanvas = new Canvas();
        leftCanvas.setCursor(Cursor.HAND);

        mainPane.setGridLinesVisible(true);
        mainPane.add(leftCanvas, 0, 0);

        rightCanvas = new Canvas();
        rightCanvas.setCursor(Cursor.HAND);

        mainPane.add(rightCanvas, 1, 0);

        ColumnConstraints cc1 =
                new ColumnConstraints(100, 100, -1, Priority.ALWAYS, HPos.CENTER, true);
        ColumnConstraints cc2 =
                new ColumnConstraints(100, 100, -1, Priority.ALWAYS, HPos.CENTER, true);
        ColumnConstraints cc3 =
                new ColumnConstraints(400, 400, 400, Priority.ALWAYS, HPos.CENTER, true);

        mainPane.getColumnConstraints().addAll(cc1, cc2, cc3);


        RowConstraints rc1 =
                new RowConstraints(400, 400, -1, Priority.ALWAYS, VPos.CENTER, true);

        mainPane.getRowConstraints().addAll(rc1);

        leftHeight.bind(leftCanvas.heightProperty());
        leftWidth.bind(leftCanvas.widthProperty());
        rightHeight.bind(rightCanvas.heightProperty());
        rightWidth.bind(rightCanvas.widthProperty());

        mainPane.layoutBoundsProperty().addListener(observable -> updateSizes());
        //mainPane.widthProperty().addListener(observable -> updateSizes());
        //mainPane.heightProperty().addListener(observable -> updateSizes());

        controlPane = new GridPane();
        //min, preferred, max
        ColumnConstraints controlLabelColConstraint =
                new ColumnConstraints(195, 195, 200, Priority.ALWAYS, HPos.CENTER, true);
        ColumnConstraints controlControlColConstraint =
                new ColumnConstraints(195, 195, 195, Priority.ALWAYS, HPos.CENTER, true);
        controlPane.getColumnConstraints().addAll(controlLabelColConstraint, controlControlColConstraint);
        mainPane.add(controlPane, 2, 0);


        Scene scene = new Scene(mainPane);

        primaryStage.setTitle("Fractal Displayer");

        primaryStage.addEventHandler(WindowEvent.WINDOW_SHOWING, event -> {
            updateSizes();
        });
        primaryStage.addEventHandler(WindowEvent.WINDOW_SHOWN, event -> {
            updateSizes();
        });
        primaryStage.addEventHandler(WindowEvent.ANY, event -> {
            updateSizes();
        });

        primaryStage.setWidth(1080);
        primaryStage.setHeight(720);

        primaryStage.setScene(scene);
        primaryStage.show();

        Platform.runLater(() -> {
            FractalLogger.logInitializedGUI(mainPane, primaryStage, leftCanvas, rightCanvas);

            parseArguments();

            FractalLogger.logArgumentsGUI(mandelbrotX, mandelbrotY, mandelbrotZoom, power,
                    iterations, juliaX, juliaY, juliaZoom, colourMode);

            updateSizes();
        });

        primaryStage.setOnCloseRequest(this::onWindowClose);
    }

    void parseArguments() {
        Parameters params = getParameters();
        List<String> param_list = params.getRaw();
        for (String param : param_list) {
            switch (param.split("=")[0].toLowerCase()) {
                case "--iterations":
                    iterations.set(Integer.parseInt(param.split("=")[1]));
                    break;
                case "--power":
                    power.set(Double.parseDouble(param.split("=")[1]));
                    break;
                case "--mandelbrotx":
                    mandelbrotX.set(Double.parseDouble(param.split("=")[1]));
                    break;
                case "--mandelbroty":
                    mandelbrotY.set(Double.parseDouble(param.split("=")[1]));
                    break;
                case "--mandelbrotzoom":
                    mandelbrotZoom.set(Double.parseDouble(param.split("=")[1]));
                    break;
                case "--juliax":
                    juliaX.set(Double.parseDouble(param.split("=")[1]));
                    break;
                case "--juliay":
                    juliaY.set(Double.parseDouble(param.split("=")[1]));
                    break;
                case "--juliazoom":
                    juliaZoom.set(Double.parseDouble(param.split("=")[1]));
                    break;
                case "--colourmode":
                    if (Objects.equals(param.split("=")[1], ColourModes.BLACK_WHITE.name())) {
                        colourMode.setValue(ColourModes.BLACK_WHITE);
                    } else if (Objects.equals(param.split("=")[1], ColourModes.COLOUR_FADE.name())) {
                        colourMode.setValue(ColourModes.COLOUR_FADE);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void onWindowClose(WindowEvent event) {
        windowClosed = true;

        if (mandelbrotRenderService != null && mandelbrotRenderService.isRunning()) {
            mandelbrotRenderService.cancel();
        }

        if (juliaRenderService != null && juliaRenderService.isRunning()) {
            juliaRenderService.cancel();
        }
    }
}
