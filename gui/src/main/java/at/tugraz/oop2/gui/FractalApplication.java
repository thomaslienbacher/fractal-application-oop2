package at.tugraz.oop2.gui;

import at.tugraz.oop2.shared.FractalLogger;
import at.tugraz.oop2.shared.SimpleImage;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
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
import lombok.Getter;

import java.util.List;
import java.util.Objects;


public class FractalApplication extends Application {

    enum ColorModes {
        BLACK_WHITE,
        COLOUR_FADE
    }

    private GridPane mainPane;
    private Canvas rightCanvas;
    private Canvas leftCanvas;
    private GridPane controlPane;

    private int iterations = 128;
    private double power = 2.0;
    private double mandelbrotX = 0.0;
    private double mandelbrotY = 0.0;
    private double mandelbrotZoom = 0.0;
    private double juliaX = 0.0;
    private double juliaY = 0.0;
    private double juliaZoom = 0.0;
    private ColorModes colourMode = ColorModes.BLACK_WHITE;


    public void setIterations(int newValue) {
        this.iterations = newValue;
    }

    public void setPower(double newValue) {
        this.power = newValue;
    }

    public void setMandelbrotX(double newValue) {
        this.mandelbrotX = newValue;
    }

    public void setMandelbrotY(double newValue) {
        this.mandelbrotY = newValue;
    }

    public void setMandelbrotZoom(double newValue) {
        this.mandelbrotZoom = newValue;
    }

    public void setJuliaX(double newValue) {
        this.juliaX = newValue;
    }

    public void setJuliaY(double newValue) {
        this.juliaY = newValue;
    }

    public void setJuliaZoom(double newValue) {
        this.juliaZoom = newValue;
    }

    public void setColourMode(ColorModes newValue) {
        this.colourMode = newValue;
    }

    public int getIterations() {
        return iterations;
    }

    public double getPower() {
        return power;
    }

    public double getMandelbrotX() {
        return mandelbrotX;
    }

    public double getMandelbrotY() {
        return mandelbrotY;
    }

    public double getMandelbrotZoom() {
        return mandelbrotZoom;
    }

    public double getJuliaX() {
        return juliaX;
    }

    public double getJuliaY() {
        return juliaY;
    }

    public double getJuliaZoom() {
        return juliaZoom;
    }

    public ColorModes getColourMode() {
        return colourMode;
    }


    @Getter
    private DoubleProperty leftHeight = new SimpleDoubleProperty();
    @Getter
    private DoubleProperty leftWidth = new SimpleDoubleProperty();
    @Getter
    private DoubleProperty rightHeight = new SimpleDoubleProperty();
    @Getter
    private DoubleProperty rightWidth = new SimpleDoubleProperty();

    private void updateSizes() {

        Bounds leftSize = mainPane.getCellBounds(0, 0);
        leftCanvas.widthProperty().set(leftSize.getWidth());
        leftCanvas.heightProperty().set(leftSize.getHeight());


        Bounds rightSize = mainPane.getCellBounds(1, 0);
        rightCanvas.widthProperty().set(rightSize.getWidth());
        rightCanvas.heightProperty().set(rightSize.getHeight());
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

        mainPane.widthProperty().addListener(observable -> updateSizes());
        mainPane.heightProperty().addListener(observable -> updateSizes());


        controlPane = new GridPane();
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
            updateSizes();
            FractalLogger.logInitializedGUI(mainPane, primaryStage, leftCanvas, rightCanvas);

        });
        Parameters params = getParameters();
        List<String> param_list = params.getRaw();
        for (String param : param_list) {
            switch (param.split("=")[0].toLowerCase()) {
                case "--iterations":
                    setIterations(Integer.parseInt(param.split("=")[1]));
                    break;
                case "--power":
                    setPower(Double.parseDouble(param.split("=")[1]));
                    break;
                case "--mandelbrotx":
                    setMandelbrotX(Double.parseDouble(param.split("=")[1]));
                    break;
                case "--mandelbroty":
                    setMandelbrotY(Double.parseDouble(param.split("=")[1]));
                    break;
                case "--mandelbrotzoom":
                    setMandelbrotZoom(Double.parseDouble(param.split("=")[1]));
                    break;
                case "--juliax":
                    setJuliaX(Double.parseDouble(param.split("=")[1]));
                    break;
                case "--juliay":
                    setJuliaY(Double.parseDouble(param.split("=")[1]));
                    break;
                case "--juliazoom":
                    setJuliaZoom(Double.parseDouble(param.split("=")[1]));
                    break;
                case "--colourmode":
                    if (Objects.equals(param.split("=")[1], ColorModes.BLACK_WHITE.name())) {
                        setColourMode(ColorModes.BLACK_WHITE);
                    } else if (Objects.equals(param.split("=")[1], ColorModes.COLOUR_FADE.name())) {
                        setColourMode(ColorModes.COLOUR_FADE);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private SimpleImage GetJuliaImage()
    {
        SimpleImage ret = new SimpleImage(rightWidth.intValue(), rightHeight.intValue());

        for(int x = 0;x < rightWidth.intValue();x++)
        {
            for(int y = 0;y < rightHeight.intValue();y++)
            {
                //ret.setPixel(x, y, GetJuliaPixel(x, y));
            }
        }

        return ret;
    }
}
