package ru.terra.mosaic.util;

import javafx.fxml.Initializable;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;

/**
 * Date: 10.07.14
 * Time: 10:19
 */
@Getter
@Setter
public abstract class AbstractWindow implements Initializable {
    protected Stage currStage;
}
