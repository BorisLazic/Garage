package garage;

import javafx.application.Application;
import javafx.stage.Stage;

import static java.lang.Thread.sleep;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

//        Button administratorMode = new Button("Administrator mode");
//        administratorMode.setPrefSize(250,50);
//        administratorMode.setOnAction(Event -> {
            Administrator administrator = new Administrator();
            administrator.showAdminMode(primaryStage);
//        });

//
//        Button userMode = new Button("User mode");
//        userMode.setPrefSize(250,50);
//        userMode.setOnAction(Event -> {
//            primaryStage.close();
//            UserMode.startUserMode(new ArrayList<>());
//        });
//
//        VBox vbox = new VBox(administratorMode,userMode);
//        vbox.setAlignment(Pos.BASELINE_CENTER);
//        vbox.setSpacing(50);
//        vbox.setPadding(new Insets(125,0,0,0));
//        BorderPane layout = new BorderPane();
//        layout.setCenter(vbox);
//        layout.setStyle("-fx-background-color: #000030");-
//
//        Scene scene = new Scene(layout,640,480);
//        primaryStage.setScene(scene);
//        primaryStage.setTitle("Garage");
//        primaryStage.show();

    }

    public static void main(String[] args) {
        launch(args);
    }
}
