package garage;

import garage.Vehicles.Ambulance.AmbulanceCar;
import garage.Vehicles.Ambulance.AmbulanceVan;
import garage.Vehicles.Civil.Car;
import garage.Vehicles.Firefigher.Firetruck;
import garage.Vehicles.Civil.Motorcycle;
import garage.Vehicles.Police.PoliceCar;
import garage.Vehicles.Police.PoliceMotorcycle;
import garage.Vehicles.Police.PoliceVan;
import garage.Vehicles.Civil.Van;
import garage.Vehicles.Vehicle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.util.LinkedList;
import java.util.Random;

public class Administrator {

    private TableView<Vehicle> tableView;
    private ChoiceBox<Platform> platformChoiceBox;
    public static String appFilesPath = System.getProperty("user.home")
            + File.separator + "Desktop" + File.separator + "App" + File.separator;
    static Image appIcon = new Image(new File(appFilesPath + "Icon.png").toURI().toString());
    public static ObservableList<Platform> Garage;

    Administrator() {
        tableView = new TableView<>();
        try (FileInputStream fis = new FileInputStream(new File(appFilesPath + "garage.ser"));
             ObjectInputStream readPlatforms = new ObjectInputStream(fis)) {
            LinkedList<Platform> platformLinkedList = (LinkedList<Platform>) readPlatforms.readObject();
            ObservableList<Platform> platformObservableList = FXCollections.observableList(platformLinkedList);
            platformChoiceBox = new ChoiceBox<>(platformObservableList);
            platformChoiceBox.setValue(platformObservableList.get(0));
            platformChoiceBox.setPrefSize(250, 50);
            Garage = platformChoiceBox.getItems();
        } catch (IOException e) {
            Platform placeHolder = new Platform();
            platformChoiceBox = new ChoiceBox<>(FXCollections.observableArrayList(placeHolder));
            platformChoiceBox.setValue(placeHolder);
            platformChoiceBox.setPrefSize(250, 50);
            Garage = platformChoiceBox.getItems();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void showAdminMode(Stage primaryStage) {
        primaryStage.close();
        Stage adminStage = new Stage();
        adminStage.getIcons().add(appIcon);

        ChoiceBox<String> vehicleType = new ChoiceBox<>();
        vehicleType.getItems().addAll("Civil Motorcycle", "Civil Car", "Civil Van",
                "Police Motorcycle", "Police Car", "Police Van",
                "Ambulance Car", "Ambulance Van",
                "Firetruck");
        vehicleType.setValue("Civil Motorcycle");
        vehicleType.setPrefSize(250, 50);

        Button addVehicle = new Button("Add vehicle");
        addVehicle.setPrefSize(250, 50);
        addVehicle.setOnAction(Event -> {
            if (platformChoiceBox.getValue().getPlatformVehicles().size() != 28) {
                addVehicle(vehicleType.getValue());
            } else {
                Alert fullPlatformAlert = new Alert(Alert.AlertType.ERROR);
                fullPlatformAlert.setHeaderText("Platform is already full. You cannot add more vehicles.");
                fullPlatformAlert.setResizable(false);
                fullPlatformAlert.setContentText("Press OK to try again.");
                fullPlatformAlert.showAndWait();
            }
        });

        Button removeVehicle = new Button("Remove vehicle");
        removeVehicle.setPrefSize(250, 50);
        removeVehicle.setOnAction(Event -> javafx.application.Platform.runLater(() -> {
            Vehicle forRemoval;
            if ((forRemoval = tableView.getSelectionModel().getSelectedItem()) != null) {
                platformChoiceBox.getValue().getPlatformVehicles().remove(forRemoval);
                tableView.getItems().remove(forRemoval);
            } else {
                Alert boxAlert = new Alert(Alert.AlertType.ERROR);
                boxAlert.setHeaderText("Please select a vehicle to delete it.");
                boxAlert.setResizable(false);
                boxAlert.setContentText("Press OK to try again.");
                boxAlert.showAndWait();

            }
        }));

        Button addRandomVehicle = new Button("Add random vehicle");
        addRandomVehicle.setPrefSize(250, 50);
        addRandomVehicle.setOnAction(Event -> {
            if (platformChoiceBox.getValue().getPlatformVehicles().size() != 28) {
                Vehicle randomVehicle = getRandomVehicle();
                platformChoiceBox.getValue().getPlatformVehicles().add(randomVehicle);
                tableView.getItems().add(randomVehicle);
            } else {
                Alert fullPlatformAlert = new Alert(Alert.AlertType.ERROR);
                fullPlatformAlert.setHeaderText("Platform is already full. You cannot add more vehicles.");
                fullPlatformAlert.setResizable(false);
                fullPlatformAlert.setContentText("Press OK to try again.");
                fullPlatformAlert.showAndWait();
            }
        });

        HBox vehicleChooseHBox = new HBox(vehicleType, addVehicle, removeVehicle, addRandomVehicle);
        vehicleChooseHBox.setSpacing(20);
        vehicleChooseHBox.setAlignment(Pos.CENTER_LEFT);
        vehicleChooseHBox.setPadding(new Insets(10, 0, 0, 0));

        TableColumn<Vehicle, String> name = new TableColumn<>("Car Name");
        TableColumn<Vehicle, String> chassisNumber = new TableColumn<>("Chassis Number");
        TableColumn<Vehicle, String> engineNumber = new TableColumn<>("Engine Number");
        TableColumn<Vehicle, String> registrationNumber = new TableColumn<>("Registration Number");
        TableColumn<Vehicle, String> imageURI = new TableColumn<>("Image URI");

        name.setCellValueFactory(new PropertyValueFactory<>("name"));
        chassisNumber.setCellValueFactory(new PropertyValueFactory<>("chassisNumber"));
        engineNumber.setCellValueFactory(new PropertyValueFactory<>("engineNumber"));
        registrationNumber.setCellValueFactory(new PropertyValueFactory<>("registrationNumber"));
        imageURI.setCellValueFactory(new PropertyValueFactory<>("imageURI"));

        tableView.getColumns().addAll(name, chassisNumber, engineNumber, registrationNumber, imageURI);
        tableView.getColumns().get(0).prefWidthProperty().bind(tableView.widthProperty().divide(6).subtract(3));
        tableView.getColumns().get(1).prefWidthProperty().bind(tableView.widthProperty().divide(8).subtract(3));
        tableView.getColumns().get(2).prefWidthProperty().bind(tableView.widthProperty().divide(8).subtract(3));
        tableView.getColumns().get(3).prefWidthProperty().bind(tableView.widthProperty().divide(5).subtract(3));
        tableView.getColumns().get(4).prefWidthProperty().bind(tableView.widthProperty().divide(3));
        tableView.setItems(FXCollections.observableArrayList(platformChoiceBox.getValue().getPlatformVehicles()));
        platformChoiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            tableView.getItems().clear();
            tableView.getItems().addAll(newValue.getPlatformVehicles());
        });

        Button showVehiclePicture = new Button("Show vehicle picture");
        showVehiclePicture.setPrefSize(250, 50);
        showVehiclePicture.setOnAction(Event -> {
            Vehicle selectedVehicle = tableView.getSelectionModel().getSelectedItem();
            if (selectedVehicle == null) {
                Alert boxAlert = new Alert(Alert.AlertType.ERROR);
                boxAlert.setHeaderText("Please select a vehicle to show it's picture");
                boxAlert.setResizable(false);
                boxAlert.setContentText("Press OK to try again.");
                boxAlert.showAndWait();
            } else {
                ImageView imageView = new ImageView(new Image(new File(selectedVehicle.getImageURI()).toURI().toString()));
                BorderPane layout = new BorderPane(imageView);
                Stage imageStage = new Stage();
                imageStage.setScene(new Scene(layout, imageView.getImage().getWidth(), imageView.getImage().getHeight()));
                imageStage.showAndWait();
            }
        });

        Button startUserMode = new Button("Start user mode");
        startUserMode.setPrefSize(250, 50);
        startUserMode.setOnAction(Event -> {
            adminStage.close();
            try (FileOutputStream fos = new FileOutputStream(new File(appFilesPath + "garage.ser"));
                 ObjectOutputStream writePlatforms = new ObjectOutputStream(fos)) {
                writePlatforms.writeObject(new LinkedList<>(platformChoiceBox.getItems()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            UserMode.startUserMode(platformChoiceBox.getItems());
        });

        BorderPane bottomPane = new BorderPane();
        bottomPane.setLeft(showVehiclePicture);
        bottomPane.setCenter(platformChoiceBox);
        bottomPane.setRight(startUserMode);

        Button addPlatform = new Button("Add new platform");
        addPlatform.setPrefSize(250, 50);
        addPlatform.setOnAction(Event -> javafx.application.Platform.runLater(() ->platformChoiceBox.getItems().add(new Platform())));

        HBox addPlatformHBox = new HBox(addPlatform);
        addPlatformHBox.setAlignment(Pos.CENTER);

        VBox vBox = new VBox(vehicleChooseHBox, tableView, bottomPane, addPlatformHBox/*,printList*/);
        vBox.setSpacing(20);
        vBox.setPadding(new Insets(20));


        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(vBox);
        borderPane.setStyle("-fx-background-color: #000030");

        adminStage.setScene(new Scene(borderPane, 1024, 768));
        adminStage.setTitle("Garage");
        adminStage.show();

    }

    public static Vehicle getRandomVehicle() {
        Random random = new Random();
        int chooser = random.nextInt(10);
        if (chooser != 0) {
            switch (random.nextInt(3)) {
                case 0:
                    return new Car();
                case 1:
                    return new Van();
                case 2:
                    return new Motorcycle();
            }
        } else {
            switch (random.nextInt(3)) {
                case 0:
                    return new Firetruck();
                case 1:
                    switch (random.nextInt(3)) {
                        case 0:
                            return new PoliceCar();
                        case 1:
                            return new PoliceVan();
                        case 2:
                            return new PoliceMotorcycle();
                    }
                case 2: {
                    if (random.nextInt(2) == 0)
                        return new AmbulanceCar();
                    else
                        return new AmbulanceVan();
                }
            }
        }
        return new Car();
    }

    private void addVehicle(String vehicleType) {
        Stage vehicleStage = new Stage();

        HBox hBox1 = new HBox();
        hBox1.setPadding(new Insets(5));
        Label lb1 = new Label("Vehicle name: ");
        lb1.setStyle("-fx-text-fill: #ffffff");
        lb1.setPadding(new Insets(5));
        lb1.setPrefSize(200, 10);
        TextField vehicleName = new TextField();
        vehicleName.setPrefSize(300, 10);
        hBox1.getChildren().addAll(lb1, vehicleName);
        hBox1.setAlignment(Pos.CENTER);

        HBox hBox2 = new HBox();
        hBox2.setPadding(new Insets(5));
        Label lb2 = new Label("Chassis number: ");
        lb2.setStyle("-fx-text-fill: #ffffff");
        lb2.setPadding(new Insets(5));
        lb2.setPrefSize(200, 10);
        TextField chassisNumber = new TextField();
        chassisNumber.setPrefSize(300, 10);
        chassisNumber.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                chassisNumber.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
        hBox2.getChildren().addAll(lb2, chassisNumber);
        hBox2.setAlignment(Pos.CENTER);

        HBox hBox3 = new HBox();
        hBox3.setPadding(new Insets(5));
        Label lb3 = new Label("Engine number: ");
        lb3.setStyle("-fx-text-fill: #ffffff");
        lb3.setPadding(new Insets(5));
        lb3.setPrefSize(200, 10);
        TextField engineNumber = new TextField();
        engineNumber.setPrefSize(300, 10);
        engineNumber.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                engineNumber.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
        hBox3.getChildren().addAll(lb3, engineNumber);
        hBox3.setAlignment(Pos.CENTER);

        HBox hBox4 = new HBox();
        hBox4.setPadding(new Insets(5));
        Label lb4 = new Label("Registration number: ");
        lb4.setStyle("-fx-text-fill: #ffffff");
        lb4.setPadding(new Insets(5));
        lb4.setPrefSize(200, 10);
        TextField registrationNumber = new TextField();
        registrationNumber.setPrefSize(300, 10);
        hBox4.getChildren().addAll(lb4, registrationNumber);
        hBox4.setAlignment(Pos.CENTER);

        HBox hBox5 = new HBox();
        hBox5.setPadding(new Insets(5));
        Label imageURI = new Label();
        imageURI.setStyle("-fx-text-fill: #ffffff");
        imageURI.setPadding(new Insets(5));
        imageURI.setPrefSize(200, 10);
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Images", "*.jpg", "*.png"));
        fileChooser.setInitialDirectory(new File(appFilesPath));
        Button imageChoice = new Button("Choose your image");
        imageChoice.setOnAction(Event -> {
            File chosenFile = fileChooser.showOpenDialog(vehicleStage);
            if (chosenFile == null) {
                imageURI.setStyle("-fx-text-fill: #ff0000");
                imageURI.setText("");
            } else {
                imageURI.setStyle("-fx-text-fill: #ffffff");
                imageURI.setText(chosenFile.getAbsolutePath());
            }
        });
        imageChoice.setPrefSize(300, 10);
        hBox5.getChildren().addAll(imageURI, imageChoice);
        hBox5.setAlignment(Pos.CENTER);

        Button buttonAddVehicle = new Button("Add vehicle");
        buttonAddVehicle.setPrefSize(250, 50);

        HBox hBox6 = new HBox();
        hBox6.setPadding(new Insets(50));
        hBox6.setAlignment(Pos.CENTER);
        hBox6.getChildren().add(buttonAddVehicle);
        hBox6.setPadding(new Insets(50, 50, 50, 50));

        VBox vBox = new VBox();
        vBox.getChildren().addAll(hBox1, hBox2, hBox3, hBox4, hBox5);
        vBox.setAlignment(Pos.CENTER);

        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(vBox);
        borderPane.setStyle("-fx-background-color: #000030");

        switch (vehicleType) {
            case "Civil Motorcycle":
            case "Police Motorcycle": {
                buttonAddVehicle.setOnAction(Event -> {
                    if (vehicleType.contains("Civil")) {
                        Motorcycle toAdd = new Motorcycle(
                                vehicleName.getText(), chassisNumber.getText(),
                                engineNumber.getText(), registrationNumber.getText(),
                                imageURI.getText());
                        platformChoiceBox.getValue().getPlatformVehicles().add(toAdd);
                        tableView.getItems().add(toAdd);
                    } else {
                        PoliceMotorcycle toAdd = new PoliceMotorcycle(
                                vehicleName.getText(), chassisNumber.getText(),
                                engineNumber.getText(), registrationNumber.getText(),
                                imageURI.getText());
                        platformChoiceBox.getValue().getPlatformVehicles().add(toAdd);
                        tableView.getItems().add(toAdd);
                    }
                    vehicleStage.close();
                });
            }
            break;
            case "Civil Car":
            case "Police Car":
            case "Ambulance Car": {
                HBox hBox7 = new HBox();
                hBox7.setPadding(new Insets(5));
                Label lb6 = new Label("Door quantity: ");
                lb6.setStyle("-fx-text-fill: #ffffff");
                lb6.setPadding(new Insets(5));
                lb6.setPrefSize(200, 10);
                ChoiceBox<Integer> doorQuantity = new ChoiceBox<>();
                doorQuantity.setPrefSize(300, 10);
                doorQuantity.getItems().addAll(2, 4, 6);
                doorQuantity.setValue(4);
                hBox7.getChildren().addAll(lb6, doorQuantity);
                hBox7.setAlignment(Pos.CENTER);

                vBox.getChildren().add(hBox7);

                buttonAddVehicle.setOnAction(Event -> {
                    if (vehicleType.contains("Civil")) {
                        Car toAdd = new Car(
                                vehicleName.getText(), chassisNumber.getText(),
                                engineNumber.getText(), registrationNumber.getText(),
                                imageURI.getText(), doorQuantity.getValue());
                        platformChoiceBox.getValue().getPlatformVehicles().add(toAdd);
                        tableView.getItems().add(toAdd);
                    } else if (vehicleType.contains("Police")) {
                        PoliceCar toAdd = new PoliceCar(
                                vehicleName.getText(), chassisNumber.getText(),
                                engineNumber.getText(), registrationNumber.getText(),
                                imageURI.getText(), doorQuantity.getValue());
                        platformChoiceBox.getValue().getPlatformVehicles().add(toAdd);
                        tableView.getItems().add(toAdd);
                    } else {
                        AmbulanceCar toAdd = new AmbulanceCar(
                                vehicleName.getText(), chassisNumber.getText(),
                                engineNumber.getText(), registrationNumber.getText(),
                                imageURI.getText(), doorQuantity.getValue());
                        platformChoiceBox.getValue().getPlatformVehicles().add(toAdd);
                        tableView.getItems().add(toAdd);
                    }
                    vehicleStage.close();
                });

            }
            break;
            case "Civil Van":
            case "Police Van":
            case "Ambulance Van":
            case "Firetruck": {
                HBox hBox7 = new HBox();
                hBox7.setPadding(new Insets(5));
                Label lb6 = new Label("Carrying capacity(in KGs): ");
                lb6.setStyle("-fx-text-fill: #ffffff");
                lb6.setPadding(new Insets(5));
                lb6.setPrefSize(200, 10);
                TextField carryingCapacity = new TextField();
                carryingCapacity.textProperty().addListener((observable, oldValue, newValue) -> {
                    if (!newValue.matches("\\d*")) {
                        carryingCapacity.setText(newValue.replaceAll("[^\\d]", ""));
                    }
                });
                carryingCapacity.setPrefSize(300, 10);
                hBox7.getChildren().addAll(lb6, carryingCapacity);
                hBox7.setAlignment(Pos.CENTER);
                vBox.getChildren().add(hBox7);

                buttonAddVehicle.setOnAction(Event -> {
                    if (vehicleType.contains("Civil")) {
                        Van toAdd = new Van(
                                vehicleName.getText(), chassisNumber.getText(),
                                engineNumber.getText(), registrationNumber.getText(),
                                imageURI.getText(), Integer.parseInt(carryingCapacity.getText()));
                        platformChoiceBox.getValue().getPlatformVehicles().add(toAdd);
                        tableView.getItems().add(toAdd);
                    } else if (vehicleType.contains("Police")) {
                        PoliceVan toAdd = new PoliceVan(
                                vehicleName.getText(), chassisNumber.getText(),
                                engineNumber.getText(), registrationNumber.getText(),
                                imageURI.getText(), Integer.parseInt(carryingCapacity.getText()));
                        platformChoiceBox.getValue().getPlatformVehicles().add(toAdd);
                        tableView.getItems().add(toAdd);
                    } else if(vehicleType.contains("Ambulance")) {
                        AmbulanceVan toAdd = new AmbulanceVan(
                                vehicleName.getText(), chassisNumber.getText(),
                                engineNumber.getText(), registrationNumber.getText(),
                                imageURI.getText(), Integer.parseInt(carryingCapacity.getText()));
                        platformChoiceBox.getValue().getPlatformVehicles().add(toAdd);
                        tableView.getItems().add(toAdd);
                    }else {
                        Firetruck toAdd = new Firetruck(
                                vehicleName.getText(), chassisNumber.getText(),
                                engineNumber.getText(), registrationNumber.getText(),
                                imageURI.getText(), Integer.parseInt(carryingCapacity.getText()));
                        platformChoiceBox.getValue().getPlatformVehicles().add(toAdd);
                        tableView.getItems().add(toAdd);
                    }
                    vehicleStage.close();
                });

                buttonAddVehicle.disableProperty().bind(vehicleName.textProperty().isEmpty()
                        .or(chassisNumber.textProperty().isEmpty())
                        .or(engineNumber.textProperty().isEmpty())
                        .or(registrationNumber.textProperty().isEmpty())
                        .or(imageURI.textProperty().isEmpty())
                        .or(carryingCapacity.textProperty().isEmpty()));
            }
        }

        if (!vehicleType.contains("Van") || !vehicleType.contains("Firetruck")) {
            buttonAddVehicle.disableProperty().bind(vehicleName.textProperty().isEmpty()
                    .or(chassisNumber.textProperty().isEmpty())
                    .or(engineNumber.textProperty().isEmpty())
                    .or(registrationNumber.textProperty().isEmpty())
                    .or(imageURI.textProperty().isEmpty()));
        }

        vBox.getChildren().add(hBox6);

        vehicleStage.setScene(new Scene(borderPane, 800, 500));
        vehicleStage.setTitle("Enter values");
        vehicleStage.show();
    }

}







