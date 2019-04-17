package garage;

import garage.Platform.Platform;
import garage.Vehicles.Vehicle;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.LinkedList;

import static java.lang.Thread.sleep;

public class UserMode {

    public static int GRID_WIDTH = 8;
    public static int GRID_HEIGHT = 10;

    public static GridPane userModeGridPane = new GridPane();
    public static ParkingRegulation parkingRegulation = new ParkingRegulation();
    public static int amountOfCarsMoving;

    public static void startUserMode(ObservableList<Platform> platforms) {
        amountOfCarsMoving =0;
        Stage userModeStage = new Stage();
        initializeGridPane();

        Label helpText = new Label("Minimum amount of vehicles:");
        helpText.setStyle("-fx-text-fill: white;");
        helpText.setPadding(new Insets(0, 0, 0, 10));

        TextField minimumVehicleAmount = new TextField();
        minimumVehicleAmount.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                minimumVehicleAmount.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
        minimumVehicleAmount.setOnKeyPressed(Event -> {
            if (Event.getCode().equals(KeyCode.ENTER)) {
                try {
                    int enteredVehicles = Integer.parseInt(minimumVehicleAmount.getText());
                    if ((enteredVehicles <= (platforms.size() * 28)) && enteredVehicles > 0) {
                        minimumVehicleAmount.setDisable(true);
                        for (Platform platform :
                                platforms) {
                            enteredVehicles -= platform.getPlatformVehicles().size();
                        }
                        setupPlatforms(new LinkedList<>(platforms), enteredVehicles);
                    } else {
                        Alert tooManyOrTooFewVehicles = new Alert(Alert.AlertType.ERROR);
                        tooManyOrTooFewVehicles.setContentText("Not enough parking spots for entered amount. Please try again.");
                        tooManyOrTooFewVehicles.showAndWait();
                    }
                } catch (NumberFormatException e) {
                    Alert numberTooLarge = new Alert(Alert.AlertType.ERROR);
                    numberTooLarge.setContentText("Number overflow.");
                    numberTooLarge.showAndWait();
                }
            }
        });
        minimumVehicleAmount.setPrefSize(50, 50);

        BorderPane layout = new BorderPane();

        ChoiceBox<Platform> platformChoiceBox = new ChoiceBox<>(platforms);
        platformChoiceBox.setPrefSize(250, 50);
        platformChoiceBox.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    newValue.synchroniseView();
                });
        platformChoiceBox.disableProperty().bind(minimumVehicleAmount.disabledProperty().not());

        Button startSimulation = new Button("Start simulation");
        startSimulation.setPrefSize(250, 50);
        startSimulation.setOnAction(Event -> {
            startSimulation(platformChoiceBox.getItems());
            startSimulation.disableProperty().unbind();
            startSimulation.disableProperty().setValue(true);
            Thread serializer = new Thread(() ->{
                while(amountOfCarsMoving>0) {
                    try {
                        sleep(10_000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                try (FileOutputStream fos = new FileOutputStream(new File(Administrator.appFilesPath + "garage.ser"));
                     ObjectOutputStream writePlatforms = new ObjectOutputStream(fos)) {
                    writePlatforms.writeObject(new LinkedList<>(Administrator.Garage));
                } catch (IOException ignored) {

                }
            });serializer.start();
        });
        startSimulation.disableProperty().bind(minimumVehicleAmount.disabledProperty().not());

        Button addRandomVehicle = new Button("Add random vehicle");
        addRandomVehicle.setPrefSize(250, 50);
        addRandomVehicle.setOnAction(Event -> {
            Vehicle vehicle = Administrator.getRandomVehicle();
            Platform currentlyChosen = platformChoiceBox.getItems().get(0);
            currentlyChosen.getPlatformPlace(1, 0).vehicle = vehicle;
            currentlyChosen.getPlatformPlace(1, 0).setVehicleLabel();
            Traveler movingVehicle = new Traveler(currentlyChosen.getPlatformPlace(1, 0), currentlyChosen);
            platformChoiceBox.getItems().get(0).traversalNodes.add(movingVehicle);
            movingVehicle.start();
            amountOfCarsMoving++;
            Thread freezeButton = new Thread(() -> {
                if (addRandomVehicle.disableProperty().isBound()) {
                    addRandomVehicle.disableProperty().unbind();
                }
                javafx.application.Platform.runLater(() -> addRandomVehicle.setDisable(true));
                try {
                    sleep(5000);
                } catch (InterruptedException ignored) {

                }
                javafx.application.Platform.runLater(() -> addRandomVehicle.setDisable(false));
            });
            freezeButton.start();
        });
        addRandomVehicle.disableProperty().bind(minimumVehicleAmount.disabledProperty().not());


        HBox upperMenu = new HBox(helpText, minimumVehicleAmount, startSimulation, addRandomVehicle);
        upperMenu.setSpacing(20);
        upperMenu.setPadding(new Insets(20, 5, 10, 5));
        upperMenu.setAlignment(Pos.CENTER);

        VBox lowerMenu = new VBox(10);
        lowerMenu.setAlignment(Pos.CENTER);
        lowerMenu.setSpacing(10);
        lowerMenu.setPadding(new Insets(0, 0, 10, 0));
        lowerMenu.getChildren().addAll(platformChoiceBox);

        layout.setTop(upperMenu);
        layout.setCenter(userModeGridPane);
        layout.setBottom(lowerMenu);
        layout.setStyle("-fx-background-color: #000030");

        userModeStage.setScene(new Scene(layout, 768, 768));
        userModeStage.setTitle("User mode");
        userModeStage.getIcons().add(Administrator.appIcon);
        userModeStage.show();

    }

    private static void initializeGridPane() {
        userModeGridPane = new GridPane();
        userModeGridPane.setAlignment(Pos.CENTER);
        userModeGridPane.gridLinesVisibleProperty().set(true);
        for (int i = 0; i < GRID_HEIGHT; i++) {
            for (int j = 0; j < GRID_WIDTH; j++) {
                Label place = new Label();
                if ((i >= 2 && (j == 0 || j == 7)) || ((i >= 2 && i <= 7) && (j == 3 || j == 4))) {
                    place.setStyle("-fx-background-color: rgb(120, 120, 120);" +
                            "-fx-text-fill: #000000;" +
                            "-fx-text-align: center;" +
                            "-fx-border-width:1px;" +
                            "-fx-border-style:solid;");
                } else {
                    place.setStyle("-fx-text-fill: white;" +
                            "-fx-background-color: white;" +
                            "-fx-text-fill: black;" +
                            "-fx-text-align: center;" +
                            "-fx-border-width:1px;" +
                            "-fx-border-style:solid;");
                }
                place.setPrefSize(75, 50);
                place.setAlignment(Pos.CENTER);
                place.setFont(Font.font("Euphemia", FontWeight.BOLD, 35));
                userModeGridPane.addRow(i, place);
            }
        }
    }

    private static void startSimulation(ObservableList<Platform> platforms) {
        for (Platform platform :
                platforms) {
            platform.startSimulation();
        }
    }

    private static void setupPlatforms(LinkedList<Platform> platforms, Integer leftToGenerate) {
        LinkedList<Vehicle> minimumFulfillment = new LinkedList<>();
        if (leftToGenerate > 0) {
            while (leftToGenerate-- > 0) {
                minimumFulfillment.add(Administrator.getRandomVehicle());
            }
        }
        for (Platform platform : platforms) {
            platform.setupPlatform(minimumFulfillment);
        }
    }
}