package at.tugraz.oop2.gui;

import at.tugraz.oop2.shared.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


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

    private Property<ColourModes> colourMode = new SimpleObjectProperty<>(ColourModes.BLACK_WHITE);

    private Property<RenderMode> renderMode = new SimpleObjectProperty<>(RenderMode.LOCAL);

    private IntegerProperty tasksPerWorker = new SimpleIntegerProperty(5);

    private Property<List<InetSocketAddress>> connections = new SimpleObjectProperty<>(new ArrayList<>(10));

    private ListView<InetSocketAddress> connectionsListView = new ListView<>();


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

        restartMandelbrotService();
        restartJuliaService();
    }

    private void restartMandelbrotService() {
        //Log call for mandelbrot
        // TODO: remove fragment number and fragment index, locally they are ignored
        FractalRenderOptions renderOptions = new MandelbrotRenderOptions(mandelbrotX.get(), mandelbrotY.get(), (int) leftCanvas.getWidth(), (int) leftCanvas.getHeight(), mandelbrotZoom.get(), power.get(), iterations.get(), colourMode.getValue(), 0, 1, renderMode.getValue());
        FractalLogger.logRenderCallGUI(renderOptions);

        if (mandelbrotRenderService != null) {
            mandelbrotRenderService.cancel();
        }
        MandelbrotRenderer mandelbrotRenderer = new MandelbrotRenderer(power.get(), iterations.get(), mandelbrotX.get(), mandelbrotY.get(), mandelbrotZoom.get(), colourMode.getValue(), renderMode.getValue(), tasksPerWorker.get(), connections.getValue(), leftCanvas);
        mandelbrotRenderer.setBounds((int) leftCanvas.getWidth(), (int) leftCanvas.getHeight());
        mandelbrotRenderService = new Service<>() {
            @Override
            protected Task<SimpleImage> createTask() {
                return mandelbrotRenderer.createTask();
            }
        };
        mandelbrotRenderService.setOnSucceeded(e -> mandelbrotRenderFinished(mandelbrotRenderService.getValue()));
        mandelbrotRenderService.start();
    }

    private void restartJuliaService() {
        //Log call for julia
        FractalRenderOptions renderOptions = new JuliaRenderOptions(juliaX.get(), juliaY.get(), (int) rightCanvas.getWidth(), (int) rightCanvas.getHeight(), juliaZoom.get(), power.get(), iterations.get(), mandelbrotX.getValue(), mandelbrotY.getValue(), colourMode.getValue(), renderMode.getValue());
        FractalLogger.logRenderCallGUI(renderOptions);

        if (juliaRenderService != null) {
            juliaRenderService.cancel();
        }
        JuliaRenderer juliaRenderer = new JuliaRenderer(power.get(), iterations.get(), juliaX.get(), juliaY.get(), juliaZoom.get(), mandelbrotX.get(), mandelbrotY.get(), colourMode.getValue(), renderMode.getValue(), tasksPerWorker.get(), connections.getValue(), rightCanvas);
        juliaRenderer.setBounds((int) rightCanvas.getWidth(), (int) rightCanvas.getHeight());
        juliaRenderService = new Service<>() {
            @Override
            protected Task<SimpleImage> createTask() {
                return juliaRenderer.createTask();
            }
        };
        juliaRenderService.setOnSucceeded(e -> juliaRenderFinished(juliaRenderService.getValue()));
        juliaRenderService.start();
    }

    public void mandelbrotRenderFinished(SimpleImage image) {
        if (image != null) {
            FractalLogger.logRenderFinishedGUI(FractalType.MANDELBROT, image);
            image.copyToCanvas(leftCanvas);
            FractalLogger.logDrawDoneGUI(FractalType.MANDELBROT);

            var pane = mainPane.getCellBounds(0, 0);
            if (image.getWidth() < pane.getWidth()) {
                updateSizes();
            }
            if (image.getHeight() < pane.getHeight()) {
                updateSizes();
            }
            if (image.getWidth() > pane.getWidth()) {
                updateSizes();
            }
            if (image.getHeight() > pane.getHeight()) {
                updateSizes();
            }
        } else //Re-draw on fail?
            updateSizes();
    }

    public void juliaRenderFinished(SimpleImage image) {
        if (image != null) {
            FractalLogger.logRenderFinishedGUI(FractalType.JULIA, image);
            image.copyToCanvas(rightCanvas);
            FractalLogger.logDrawDoneGUI(FractalType.JULIA);

            var pane = mainPane.getCellBounds(1, 0);
            if (image.getWidth() < pane.getWidth()) {
                updateSizes();
            }
            if (image.getHeight() < pane.getHeight()) {
                updateSizes();
            }
            if (image.getWidth() > pane.getWidth()) {
                updateSizes();
            }
            if (image.getHeight() > pane.getHeight()) {
                updateSizes();
            }
        } else //Re-draw on fail?
            updateSizes();
    }

    @Override
    public void start(Stage primaryStage) {
        mainPane = new GridPane();

        parseArguments();

        if (connections.getValue().isEmpty()) {
            InetSocketAddress newConnection = new InetSocketAddress("localhost", 8010);
            var currentConnections = connections.getValue();
            currentConnections.add(newConnection);
            connections.setValue(currentConnections);
        }

        leftCanvas = new Canvas();
        leftCanvas.setCursor(Cursor.HAND);

        // reset drag start position
        //TODO: dragging all the time
        leftCanvas.setOnMousePressed(mouseEvent -> {
            previousMandelbrotX = mouseEvent.getX();
            previousMandelbrotY = mouseEvent.getY();
        });
        leftCanvas.setOnMouseReleased(mouseEvent -> {
            var x = mouseEvent.getX();
            var y = mouseEvent.getY();
            var dx = x - previousMandelbrotX;
            var dy = y - previousMandelbrotY;

            var transform = new SpaceTransform(leftWidth.intValue(), leftHeight.intValue(), mandelbrotZoom.get(), mandelbrotX.get(), mandelbrotY.get());

            var transX = transform.dragDistanceX(dx);
            var transY = transform.dragDistanceY(dy);
            // use minus because we move camera
            mandelbrotX.setValue(mandelbrotX.getValue() - transX);
            mandelbrotY.setValue(mandelbrotY.getValue() - transY);
            FractalLogger.logDragGUI(mandelbrotX.getValue(), mandelbrotY.getValue(), FractalType.MANDELBROT);
        });
        leftCanvas.setOnScroll(event -> {
            mandelbrotZoom.setValue(mandelbrotZoom.getValue() + (event.getDeltaY() * 0.02));
            FractalLogger.logZoomGUI(mandelbrotZoom.getValue(), FractalType.MANDELBROT);
            restartMandelbrotService();
        });

        mainPane.setGridLinesVisible(true);
        mainPane.add(leftCanvas, 0, 0);

        rightCanvas = new Canvas();
        rightCanvas.setCursor(Cursor.HAND);

        //TODO: dragging all the time
        rightCanvas.setOnMousePressed(mouseEvent -> {
            previousJuliaX = mouseEvent.getX();
            previousJuliaY = mouseEvent.getY();
        });

        rightCanvas.setOnMouseDragged(mouseEvent -> {
            var x = mouseEvent.getX();
            var y = mouseEvent.getY();
            var dx = x - previousJuliaX;
            var dy = y - previousJuliaY;

            var transform = new SpaceTransform(rightWidth.intValue(), rightHeight.intValue(), juliaZoom.get(), juliaX.get(), juliaY.get());

            var transX = transform.dragDistanceX(dx);
            var transY = transform.dragDistanceY(dy);

            // use minus because we move camera
            juliaX.setValue(juliaX.getValue() - transX);
            juliaY.setValue(juliaY.getValue() - transY);
            FractalLogger.logDragGUI(juliaX.getValue(), juliaY.getValue(), FractalType.JULIA);
        });
        rightCanvas.setOnScroll(event -> {
            System.out.println(event.getDeltaX() + " " + event.getDeltaY());
            juliaZoom.setValue(juliaZoom.getValue() + (event.getDeltaY() * 0.02));
            FractalLogger.logZoomGUI(juliaZoom.getValue(), FractalType.JULIA);
            restartJuliaService();
        });
        mainPane.add(rightCanvas, 1, 0);

        ColumnConstraints cc1 = new ColumnConstraints(100, 100, -1, Priority.ALWAYS, HPos.CENTER, true);
        ColumnConstraints cc2 = new ColumnConstraints(100, 100, -1, Priority.ALWAYS, HPos.CENTER, true);
        ColumnConstraints cc3 = new ColumnConstraints(400, 400, 400, Priority.ALWAYS, HPos.CENTER, true);

        mainPane.getColumnConstraints().addAll(cc1, cc2, cc3);


        RowConstraints rc1 = new RowConstraints(400, 400, -1, Priority.ALWAYS, VPos.CENTER, true);

        mainPane.getRowConstraints().addAll(rc1);

        leftHeight.bind(leftCanvas.heightProperty());
        leftWidth.bind(leftCanvas.widthProperty());
        rightHeight.bind(rightCanvas.heightProperty());
        rightWidth.bind(rightCanvas.widthProperty());

        mainPane.layoutBoundsProperty().addListener(observable -> updateSizes());
        //mainPane.widthProperty().addListener(observable -> updateSizes());
        //mainPane.heightProperty().addListener(observable -> updateSizes());

        setupControlPane();
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


            FractalLogger.logArgumentsGUI(mandelbrotX, mandelbrotY, mandelbrotZoom, power, iterations, juliaX, juliaY, juliaZoom, colourMode);

            updateSizes();
        });

        primaryStage.setOnCloseRequest(this::onWindowClose);
    }

    public void setupControlPane() {
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
                    restartMandelbrotService();
                    restartJuliaService();
                }
            } catch (NumberFormatException ignored) {
            }
        });

        TextField powerTextField = new TextField(Double.toString(power.get()));
        powerTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                if (!Objects.equals(newValue, oldValue)) {
                    power.set(Double.parseDouble(newValue));
                    restartMandelbrotService();
                    restartJuliaService();
                }
            } catch (NumberFormatException ignored) {
            }
        });

        TextField mandelbrotXTextField = new TextField(Double.toString(mandelbrotX.get()));
        mandelbrotXTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                if (!Objects.equals(newValue, oldValue)) {
                    mandelbrotX.set(Double.parseDouble(newValue));
                    restartMandelbrotService();
                    restartJuliaService();
                }
            } catch (NumberFormatException ignored) {
            }
        });

        TextField mandelbrotYTextField = new TextField(Double.toString(mandelbrotY.get()));
        mandelbrotYTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                if (!Objects.equals(newValue, oldValue)) {
                    mandelbrotY.set(Double.parseDouble(newValue));
                    restartMandelbrotService();
                    restartJuliaService();
                }
            } catch (NumberFormatException ignored) {
            }
        });

        TextField mandelbrotZoomTextField = new TextField(Double.toString(mandelbrotZoom.get()));
        mandelbrotZoomTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                if (!Objects.equals(newValue, oldValue)) {
                    mandelbrotZoom.set(Double.parseDouble(newValue));
                    restartMandelbrotService();
                }
            } catch (NumberFormatException ignored) {
            }
        });

        TextField juliaXTextField = new TextField(Double.toString(juliaX.get()));
        juliaXTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                if (!Objects.equals(newValue, oldValue)) {
                    juliaX.set(Double.parseDouble(newValue));
                    restartJuliaService();
                }
            } catch (NumberFormatException ignored) {
            }
        });

        TextField juliaYTextField = new TextField(Double.toString(juliaY.get()));
        juliaYTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                if (!Objects.equals(newValue, oldValue)) {
                    juliaY.set(Double.parseDouble(newValue));
                    restartJuliaService();
                }
            } catch (NumberFormatException ignored) {
            }
        });

        TextField juliaZoomTextField = new TextField(Double.toString(juliaZoom.get()));
        juliaZoomTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                if (!Objects.equals(newValue, oldValue)) {
                    juliaZoom.set(Double.parseDouble(newValue));
                    restartJuliaService();
                }
            } catch (NumberFormatException ignored) {
            }
        });

        ComboBox<ColourModes> colourModeField = new ComboBox<>(FXCollections.observableArrayList(ColourModes.values()));
        colourModeField.getSelectionModel().select(colourMode.getValue());
        colourModeField.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            colourMode.setValue(newValue);
            restartMandelbrotService();
            restartJuliaService();
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

        iterationsTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                try {
                    Integer.parseInt(iterationsTextField.getText());
                } catch (NumberFormatException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "The entered value is not in the right format", ButtonType.CLOSE);
                    alert.showAndWait();
                    iterationsTextField.setText("128");
                    iterations.setValue(128);
                    restartMandelbrotService();
                    restartJuliaService();
                }

            }
        });

        powerTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                try {
                    Double.parseDouble(powerTextField.getText());
                } catch (NumberFormatException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "The entered value is not in the right format", ButtonType.CLOSE);
                    alert.showAndWait();
                    powerTextField.setText("2.0");
                    power.setValue(2.0);
                    restartMandelbrotService();
                    restartJuliaService();
                }

            }
        });
        mandelbrotXTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                try {
                    Double.parseDouble(mandelbrotXTextField.getText());
                } catch (NumberFormatException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "The entered value is not in the right format", ButtonType.CLOSE);
                    alert.showAndWait();
                    mandelbrotXTextField.setText("0.0");
                    mandelbrotX.setValue(0.0);
                    restartMandelbrotService();
                    restartJuliaService();
                }

            }
        });
        mandelbrotYTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                try {
                    Double.parseDouble(mandelbrotYTextField.getText());
                } catch (NumberFormatException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "The entered value is not in the right format", ButtonType.CLOSE);
                    alert.showAndWait();
                    mandelbrotYTextField.setText("0.0");
                    mandelbrotY.setValue(0.0);
                    restartMandelbrotService();
                    restartJuliaService();
                }

            }
        });
        mandelbrotZoomTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                try {
                    Double.parseDouble(mandelbrotZoomTextField.getText());
                } catch (NumberFormatException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "The entered value is not in the right format", ButtonType.CLOSE);
                    alert.showAndWait();
                    mandelbrotZoomTextField.setText("0.0");
                    mandelbrotZoom.setValue(0.0);
                    restartMandelbrotService();
                }

            }
        });
        juliaXTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                try {
                    Double.parseDouble(juliaXTextField.getText());
                } catch (NumberFormatException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "The entered value is not in the right format", ButtonType.CLOSE);
                    alert.showAndWait();
                    juliaXTextField.setText("0.0");
                    juliaX.setValue(0.0);
                    restartJuliaService();
                }

            }
        });
        juliaYTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                try {
                    Double.parseDouble(juliaYTextField.getText());
                } catch (NumberFormatException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "The entered value is not in the right format", ButtonType.CLOSE);
                    alert.showAndWait();
                    juliaYTextField.setText("0.0");
                    juliaY.setValue(0.0);
                    restartJuliaService();
                }

            }
        });
        juliaZoomTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                try {
                    Double.parseDouble(juliaZoomTextField.getText());
                } catch (NumberFormatException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "The entered value is not in the right format", ButtonType.CLOSE);
                    alert.showAndWait();
                    juliaZoomTextField.setText("0.0");
                    juliaZoom.setValue(0.0);
                    restartJuliaService();
                }

            }
        });
        tasksPerWorkerTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                try {
                    Integer.parseInt(tasksPerWorkerTextField.getText());
                } catch (NumberFormatException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "The entered value is not in the right format", ButtonType.CLOSE);
                    alert.showAndWait();
                    tasksPerWorkerTextField.setText("5");
                    tasksPerWorker.setValue(5);
                }

            }
        });


        mandelbrotX.addListener((observable, oldValue, newValue) -> {
            if (!Objects.equals(oldValue, newValue)) {
                mandelbrotXTextField.setText(newValue.toString());
            }
        });
        mandelbrotY.addListener((observable, oldValue, newValue) -> {
            if (!Objects.equals(oldValue, newValue)) {
                mandelbrotYTextField.setText(newValue.toString());
            }
        });
        mandelbrotZoom.addListener((observable, oldValue, newValue) -> {
            if (!Objects.equals(oldValue, newValue)) {
                mandelbrotZoomTextField.setText(newValue.toString());
            }
        });
        juliaX.addListener((observable, oldValue, newValue) -> {
            if (!Objects.equals(oldValue, newValue)) {
                juliaXTextField.setText(newValue.toString());
            }
        });
        juliaY.addListener((observable, oldValue, newValue) -> {
            if (!Objects.equals(oldValue, newValue)) {
                juliaYTextField.setText(newValue.toString());
            }
        });
        juliaZoom.addListener((observable, oldValue, newValue) -> {
            if (!Objects.equals(oldValue, newValue)) {
                juliaZoomTextField.setText(newValue.toString());
            }
        });

        var connectionsButton = new Button("Connection Editor");

        connectionsButton.setOnAction(event -> showConnectionsWindow());

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
        controlPane.add(connectionsButton, 1, 11);
        controlPane.add(new Label("~~~"), 1, 12);

        //min, preferred, max
        ColumnConstraints controlLabelColConstraint = new ColumnConstraints(195, 195, 200, Priority.ALWAYS, HPos.CENTER, true);
        ColumnConstraints controlControlColConstraint = new ColumnConstraints(195, 195, 195, Priority.ALWAYS, HPos.CENTER, true);
        controlPane.getColumnConstraints().addAll(controlLabelColConstraint, controlControlColConstraint);
    }

    public void parseArguments() {
        Parameters params = getParameters();
        List<String> param_list = params.getRaw();
        for (String param : param_list) {
            switch (param.split("=")[0].toLowerCase()) {
                case "--iterations":
                    try {
                        iterations.set(Integer.parseInt(param.split("=")[1]));
                    } catch (NumberFormatException ignored) {
                    }
                    break;
                case "--power":
                    try {
                        power.set(Double.parseDouble(param.split("=")[1]));
                    } catch (NumberFormatException ignored) {
                    }
                    break;
                case "--mandelbrotx":
                    try {
                        mandelbrotX.set(Double.parseDouble(param.split("=")[1]));
                    } catch (NumberFormatException ignored) {
                    }
                    break;
                case "--mandelbroty":
                    try {
                        mandelbrotY.set(Double.parseDouble(param.split("=")[1]));
                    } catch (NumberFormatException ignored) {
                    }
                    break;
                case "--mandelbrotzoom":
                    try {
                        mandelbrotZoom.set(Double.parseDouble(param.split("=")[1]));
                    } catch (NumberFormatException ignored) {
                    }
                    break;
                case "--juliax":
                    try {
                        juliaX.set(Double.parseDouble(param.split("=")[1]));
                    } catch (NumberFormatException ignored) {
                    }
                    break;
                case "--juliay":
                    try {
                        juliaY.set(Double.parseDouble(param.split("=")[1]));
                    } catch (NumberFormatException ignored) {
                    }
                    break;
                case "--juliazoom":
                    try {
                        juliaZoom.set(Double.parseDouble(param.split("=")[1]));
                    } catch (NumberFormatException ignored) {
                    }
                    break;
                case "--colourmode":
                    if (Objects.equals(param.split("=")[1], ColourModes.BLACK_WHITE.name())) {
                        colourMode.setValue(ColourModes.BLACK_WHITE);
                    } else if (Objects.equals(param.split("=")[1], ColourModes.COLOUR_FADE.name())) {
                        colourMode.setValue(ColourModes.COLOUR_FADE);
                    }
                    break;

                case "--tasksperworker":
                    try {
                        tasksPerWorker.set(Integer.parseInt(param.split("=")[1]));
                    } catch (NumberFormatException ignored) {
                    }
                    break;
                case "--rendermode":
                    if (Objects.equals(param.split("=")[1], RenderMode.LOCAL.name())) {
                        renderMode.setValue(RenderMode.LOCAL);
                    } else if (Objects.equals(param.split("=")[1], RenderMode.DISTRIBUTED.name())) {
                        renderMode.setValue(RenderMode.DISTRIBUTED);
                    }
                    break;
                case "--connection":
                    for (String connection : param.split("=")[1].split(",")) {
                        try {
                            InetSocketAddress newConnection = new InetSocketAddress(connection.split(":")[0], Integer.parseInt(connection.split(":")[1]));
                            var currentConnections = connections.getValue();
                            currentConnections.add(newConnection);
                            connections.setValue(currentConnections);
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void onWindowClose(WindowEvent event) {
        windowClosed = true;

        if (mandelbrotRenderService != null) {
            mandelbrotRenderService.cancel();
        }

        if (juliaRenderService != null) {
            juliaRenderService.cancel();
        }
    }

    public void showConnectionsWindow() {
        Stage connectionsWindow = new Stage();
        connectionsWindow.setTitle("Connections");

        ListView<InetSocketAddress> connectionsListView = new ListView<>();
        ObservableList<InetSocketAddress> inetSocketAddressObservableList = FXCollections.observableArrayList(connections.getValue());
        connectionsListView.setItems(inetSocketAddressObservableList);

        Button addButton = new Button("Add");
        addButton.setOnAction(event -> addConnection(connectionsListView));

        Button deleteButton = new Button("Delete");
        deleteButton.setOnAction(event -> deleteSelectedConnection(connectionsListView));

        Button changeButton = new Button("Change");
        changeButton.setOnAction(event -> changeSelectedConnection(connectionsListView));

        Button testButton = new Button("Test Connection");
        testButton.setOnAction(event -> {
            InetSocketAddress selectedConnection = connectionsListView.getSelectionModel().getSelectedItem();
            if (selectedConnection != null) {
                try (Socket socket = new Socket()) {
                    socket.connect(selectedConnection);
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Connection Test");
                    alert.setHeaderText(null);
                    alert.setContentText("Connection successful!");
                    alert.showAndWait();
                } catch (IOException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Connection Test");
                    alert.setHeaderText(null);
                    alert.setContentText("Connection failed!");
                    alert.showAndWait();
                }
            }
        });

        HBox buttonBox = new HBox(addButton, deleteButton, changeButton, testButton);
        buttonBox.setSpacing(4.0);

        BorderPane root = new BorderPane();
        root.setCenter(connectionsListView);
        root.setBottom(buttonBox);

        Scene scene = new Scene(root, 400, 600);
        connectionsWindow.setScene(scene);
        connectionsWindow.show();
    }

    public void addConnection(ListView<InetSocketAddress> connectionsListView) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Connection");
        dialog.setHeaderText("Enter the host string and port for the new connection");
        dialog.setContentText("<Host>:<Port>");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                String[] hostPort = result.get().split(":");
                String host = hostPort[0];
                int port = Integer.parseInt(hostPort[1]);
                List<InetSocketAddress> connectionsList = connections.getValue();
                connectionsList.add(new InetSocketAddress(host, port));
                connections.setValue(connectionsList);
                connectionsListView.setItems(FXCollections.observableArrayList(connections.getValue()));
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Host:Port Input");
                alert.setHeaderText(null);
                alert.setContentText("Wrong Format!");
                alert.showAndWait();
            }
        }
    }

    public void deleteSelectedConnection(ListView<InetSocketAddress> connectionsListView) {
        InetSocketAddress selectedConnection = connectionsListView.getSelectionModel().getSelectedItem();
        if (selectedConnection != null) {
            List<InetSocketAddress> connectionsList = connections.getValue();
            connectionsList.remove(selectedConnection);
            connections.setValue(connectionsList);
            connectionsListView.setItems(FXCollections.observableArrayList(connections.getValue()));
        }
    }

    public void changeSelectedConnection(ListView<InetSocketAddress> connectionsListView) {
        InetSocketAddress selectedConnection = connectionsListView.getSelectionModel().getSelectedItem();
        if (selectedConnection != null) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Change selected Connection");
            dialog.setHeaderText("Enter the host string and port for the changed connection");
            dialog.setContentText("<Host>:<Port>");

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                try {
                    String[] hostPort = result.get().split(":");
                    String host = hostPort[0];
                    int port = Integer.parseInt(hostPort[1]);

                    List<InetSocketAddress> connectionsList = connections.getValue();
                    connectionsList.remove(selectedConnection);

                    connectionsList.add(new InetSocketAddress(host, port));
                    connections.setValue(connectionsList);
                    connectionsListView.setItems(FXCollections.observableArrayList(connections.getValue()));
                } catch (Exception e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Host:Port Input");
                    alert.setHeaderText(null);
                    alert.setContentText("Wrong Format!");
                    alert.showAndWait();
                }
            }
        }
    }
}
