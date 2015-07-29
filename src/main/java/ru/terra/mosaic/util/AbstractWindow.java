package ru.terra.mosaic.util;

import javafx.fxml.Initializable;
import javafx.stage.Stage;
import org.apache.log4j.Logger;

/**
 * Date: 10.07.14
 * Time: 10:19
 */
public abstract class AbstractWindow implements Initializable {
    protected Logger logger = Logger.getLogger(this.getClass());
    protected Stage currStage;

    public Stage getCurrStage() {
        return currStage;
    }

    public void setCurrStage(Stage currStage) {
        this.currStage = currStage;
    }
}
