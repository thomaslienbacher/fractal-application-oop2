package at.tugraz.oop2.gui;

import at.tugraz.oop2.shared.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;
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

    private IntegerProperty iterations = new SimpleIntegerProperty(128);

    private DoubleProperty power = new SimpleDoubleProperty(2.0);

    private DoubleProperty mandelbrotX = new SimpleDoubleProperty(0.0);

    private DoubleProperty mandelbrotY = new SimpleDoubleProperty(0.0);

    private DoubleProperty mandelbrotZoom = new SimpleDoubleProperty(0.0);

    private DoubleProperty juliaX = new SimpleDoubleProperty(0.0);

    private DoubleProperty juliaY = new SimpleDoubleProperty(0.0);

    private DoubleProperty juliaZoom = new SimpleDoubleProperty(0.0);

    //TODO: default value is BLACK_WHITE !
    private Property<ColourModes> colourMode = new SimpleObjectProperty<>(ColourModes.BLACK_WHITE);

    private Property<RenderMode> renderMode = new SimpleObjectProperty<>(RenderMode.LOCAL);

    private IntegerProperty tasksPerWorker = new SimpleIntegerProperty(5);

    private Property<List<InetSocketAddress>> connections = new SimpleObjectProperty<>(new ArrayList<>(10));

    private DoubleProperty leftHeight = new SimpleDoubleProperty();

    private DoubleProperty leftWidth = new SimpleDoubleProperty();

    private DoubleProperty rightHeight = new SimpleDoubleProperty();

    private DoubleProperty rightWidth = new SimpleDoubleProperty();

    private boolean windowClosed = false;
    double previousMandelbrotX = 0;
    double previousMandelbrotY = 0;
    double previousJuliaX = 0;
    double previousJuliaY = 0;

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
    public void start(Stage primaryStage) {
        mainPane = new GridPane();

        parseArguments();
        leftCanvas = new Canvas();
        leftCanvas.setCursor(Cursor.HAND);


        leftCanvas.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                double x = event.getX();
                double y = event.getY();

                double deltaX = x - previousMandelbrotX;
                double deltaY = y - previousMandelbrotY;

                // update the previous coordinates for the next drag event
                previousMandelbrotX = x;
                previousMandelbrotY = y;

                if (deltaX > 0) {
                    mandelbrotX.setValue(mandelbrotX.getValue() - 0.07);
                } else if (deltaX < 0) {
                    mandelbrotX.setValue(mandelbrotX.getValue() + 0.07);
                }

                if (deltaY > 0) {
                    mandelbrotY.setValue(mandelbrotY.getValue() - 0.11);
                } else if (deltaY < 0) {
                    mandelbrotY.setValue(mandelbrotY.getValue() + 0.11);
                }
                restartServices();
            }
        });

        leftCanvas.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override
            public void handle(ScrollEvent event) {
                if (event.getDeltaY() > 0) {
                    mandelbrotZoom.setValue(mandelbrotZoom.getValue() + 0.02);
                } else if (event.getDeltaY() < 0) {
                    mandelbrotZoom.setValue(mandelbrotZoom.getValue() - 0.02);
                }
                restartServices();
            }
        });


        mainPane.setGridLinesVisible(true);
        mainPane.add(leftCanvas, 0, 0);

        rightCanvas = new Canvas();
        rightCanvas.setCursor(Cursor.HAND);
        rightCanvas.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                double x = event.getX();
                double y = event.getY();

                double deltaX = x - previousJuliaX;
                double deltaY = y - previousJuliaY;

                // update the previous coordinates for the next drag event
                previousJuliaX = x;
                previousJuliaY = y;

                if (deltaX > 0) {
                    juliaX.setValue(juliaX.getValue() - 0.07);
                } else if (deltaX < 0) {
                    juliaX.setValue(juliaX.getValue() + 0.07);
                }

                if (deltaY > 0) {
                    juliaY.setValue(juliaY.getValue() - 0.11);
                } else if (deltaY < 0) {
                    juliaY.setValue(juliaY.getValue() + 0.11);
                }
                restartServices();
            }
        });

        rightCanvas.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override
            public void handle(ScrollEvent event) {
                if (event.getDeltaY() > 0) {
                    juliaZoom.setValue(juliaZoom.getValue() + 0.02);
                } else if (event.getDeltaY() < 0) {
                    juliaZoom.setValue(juliaZoom.getValue() - 0.02);
                }
                restartServices();
            }
        });
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
        controlPane.setVgap(4.0);
        controlPane.add(new Label("Iterations"), 0, 0);
        controlPane.add(new Label("Power"), 0, 1);
        controlPane.add(new Label("Mandelbrot X center"), 0, 2);
        controlPane.add(new Label("Mandelbrot Y center"), 0, 3);
        controlPane.add(new Label("Mandelbrot zoom"), 0, 4);
        controlPane.add(new Label("Julia X center"), 0, 5);
        controlPane.add(new Label("Julia Y center"), 0, 6);
        controlPane.add(new Label("Julia zoom"), 0, 7);
        controlPane.add(new Label("ColorMode"), 0, 8); // colour = cringe
        controlPane.add(new Label("RenderMode"), 0, 9);
        controlPane.add(new Label("Tasks per Worker"), 0, 10);
        controlPane.add(new Label("Connection Editor"), 0, 11);
        controlPane.add(new Label("Connected Workers"), 0, 12);

        TextField iterationsTextField = new TextField(Integer.toString(iterations.get()));
        iterationsTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                if (!Objects.equals(newValue, oldValue)) {
                    iterations.set(Integer.parseInt(newValue));
                    restartServices();
                }
            } catch (NumberFormatException ignored) {
            }
        });

        TextField powerTextField = new TextField(Double.toString(power.get()));
        powerTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                if (!Objects.equals(newValue, oldValue)) {
                    power.set(Double.parseDouble(newValue));
                    restartServices();
                }
            } catch (NumberFormatException ignored) {
            }
        });

        TextField mandelbrotXTextField = new TextField(Double.toString(mandelbrotX.get()));
        mandelbrotXTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                if (!Objects.equals(newValue, oldValue)) {
                    mandelbrotX.set(Double.parseDouble(newValue));
                    restartServices();
                }
            } catch (NumberFormatException ignored) {
            }
        });

        TextField mandelbrotYTextField = new TextField(Double.toString(mandelbrotY.get()));
        mandelbrotYTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                if (!Objects.equals(newValue, oldValue)) {
                    mandelbrotY.set(Double.parseDouble(newValue));
                    restartServices();
                }
            } catch (NumberFormatException ignored) {
            }
        });

        TextField mandelbrotZoomTextField = new TextField(Double.toString(mandelbrotZoom.get()));
        mandelbrotZoomTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                if (!Objects.equals(newValue, oldValue)) {
                    mandelbrotZoom.set(Double.parseDouble(newValue));
                    restartServices();
                }
            } catch (NumberFormatException ignored) {
            }
        });

        TextField juliaXTextField = new TextField(Double.toString(juliaX.get()));
        juliaXTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                if (!Objects.equals(newValue, oldValue)) {
                    juliaX.set(Double.parseDouble(newValue));
                    restartServices();
                }
            } catch (NumberFormatException ignored) {
            }
        });

        TextField juliaYTextField = new TextField(Double.toString(juliaY.get()));
        juliaYTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                if (!Objects.equals(newValue, oldValue)) {
                    juliaY.set(Double.parseDouble(newValue));
                    restartServices();
                }
            } catch (NumberFormatException ignored) {
            }
        });

        TextField juliaZoomTextField = new TextField(Double.toString(juliaZoom.get()));
        juliaZoomTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                if (!Objects.equals(newValue, oldValue)) {
                    juliaZoom.set(Double.parseDouble(newValue));
                    restartServices();
                }
            } catch (NumberFormatException ignored) {
            }
        });

        ComboBox<ColourModes> colourModeField = new ComboBox<>(FXCollections.observableArrayList(ColourModes.values()));
        colourModeField.getSelectionModel().select(colourMode.getValue());
        colourModeField.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            colourMode.setValue(newValue);
            restartServices();
        });

        ComboBox<RenderMode> renderModeField = new ComboBox<>(FXCollections.observableArrayList(RenderMode.values()));
        renderModeField.getSelectionModel().selectFirst();
        renderModeField.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            renderMode.setValue(newValue);
        });

        TextField tasksPerWorkerTextField = new TextField(Integer.toString(tasksPerWorker.get()));
        tasksPerWorkerTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                if (!Objects.equals(newValue, oldValue)) {
                    tasksPerWorker.set(Integer.parseInt(newValue));
                }
            } catch (NumberFormatException ignored) {
            }
        });


        mandelbrotX.addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                // update the text of the text field with the new value of the zoom property
                mandelbrotXTextField.setText(String.format("%.2f", newValue));
            }
        });
        mandelbrotY.addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                // update the text of the text field with the new value of the zoom property
                mandelbrotYTextField.setText(String.format("%.2f", newValue));
            }
        });
        mandelbrotZoom.addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                // update the text of the text field with the new value of the zoom property
                mandelbrotZoomTextField.setText(String.format("%.2f", newValue));
            }
        });
        juliaX.addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                // update the text of the text field with the new value of the zoom property
                juliaXTextField.setText(String.format("Zoom: %.2f", newValue));
            }
        });
        juliaY.addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                // update the text of the text field with the new value of the zoom property
                juliaYTextField.setText(String.format("%.2f", newValue));
            }
        });
        juliaZoom.addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                // update the text of the text field with the new value of the zoom property
                juliaZoomTextField.setText(String.format("%.2f", newValue));
            }
        });

        controlPane.add(iterationsTextField, 1, 0);
        controlPane.add(powerTextField, 1, 1);
        controlPane.add(mandelbrotXTextField, 1, 2);
        controlPane.add(mandelbrotYTextField, 1, 3);
        controlPane.add(mandelbrotZoomTextField, 1, 4);
        controlPane.add(juliaXTextField, 1, 5);
        controlPane.add(juliaYTextField, 1, 6);
        controlPane.add(juliaZoomTextField, 1, 7);
        controlPane.add(colourModeField, 1, 8);
        controlPane.add(renderModeField, 1, 9);
        controlPane.add(tasksPerWorkerTextField, 1, 10);
        controlPane.add(new Button("Connection Editor"), 1, 11);
        controlPane.add(new Label("~~~"), 1, 12);

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
