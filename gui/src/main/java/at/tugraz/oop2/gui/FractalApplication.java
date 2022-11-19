package at.tugraz.oop2.gui;

import at.tugraz.oop2.shared.ColourModes;
import at.tugraz.oop2.shared.FractalLogger;
import at.tugraz.oop2.shared.SimpleImage;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.*;
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

    private GridPane mainPane;
    private Canvas rightCanvas;
    private Canvas leftCanvas;
    private GridPane controlPane;

    @Getter
    private IntegerProperty iterations = new SimpleIntegerProperty(128);
    @Getter
    private DoubleProperty power = new SimpleDoubleProperty(2.0);
    @Getter
    private DoubleProperty mandelbrotX = new SimpleDoubleProperty(0.0);
    @Getter
    private DoubleProperty mandelbrotY = new SimpleDoubleProperty(0.0);
    @Getter
    private DoubleProperty mandelbrotZoom = new SimpleDoubleProperty(0.0);
    @Getter
    private DoubleProperty juliaX = new SimpleDoubleProperty(0.0);
    @Getter
    private DoubleProperty juliaY = new SimpleDoubleProperty(0.0);
    @Getter
    private DoubleProperty juliaZoom = new SimpleDoubleProperty(0.0);
    @Getter
    private Property<ColourModes> colourMode = new SimpleObjectProperty<ColourModes>(ColourModes.BLACK_WHITE);

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

        parseArguments();
        FractalLogger.logInitializedGUI(mainPane, primaryStage, leftCanvas, rightCanvas);
        FractalLogger.logArgumentsGUI(mandelbrotX, mandelbrotY, mandelbrotZoom, power,
                iterations, juliaX, juliaY, juliaZoom, colourMode);
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

    private SimpleImage getJuliaImage() {
        SimpleImage ret = new SimpleImage(rightWidth.intValue(), rightHeight.intValue());

        for (int x = 0; x < rightWidth.intValue(); x++) {
            for (int y = 0; y < rightHeight.intValue(); y++) {
                //ret.setPixel(x, y, GetJuliaPixel(x, y));
            }
        }

        return ret;
    }
}
