package ru.terra.mosaic;

import javafx.application.Application;
import javafx.stage.Stage;
import ru.terra.mosaic.util.StageHelper;

/**
 * Date: 21.07.15
 * Time: 16:13
 */
public class Main extends Application {
    public static void main(String... args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        StageHelper.openWindow("w_main.fxml", "Mosaic");
    }
}
