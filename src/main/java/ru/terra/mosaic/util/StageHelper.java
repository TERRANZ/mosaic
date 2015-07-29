package ru.terra.mosaic.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Date: 28.04.14
 * Time: 13:27
 */
public class StageHelper {
    public static <T extends AbstractWindow> Pair<Stage, T> openWindow(String fxmlFileName, String title) {
        String fxmlFile = C.FXML + fxmlFileName;
        URL location = StageHelper.class.getResource(fxmlFile);
        Parent root = null;
        FXMLLoader fxmlLoader = new FXMLLoader();
        try {
            root = fxmlLoader.load(location.openStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        Stage stage = new Stage();
        ((T) fxmlLoader.getController()).setCurrStage(stage);
        stage.setTitle(title);
        stage.setScene(new Scene(root));
        stage.getScene().getStylesheets().addAll(C.STYLE + "style.css");
        stage.show();

        return new Pair<>(stage, (T) fxmlLoader.getController());
    }
}
