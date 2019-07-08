package example;


import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;


import java.util.Optional;

public class CommonUiUtil {

    public static Optional<ButtonType> alert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        return alertCommon(type,message,alert);

    }
    public static Optional<ButtonType> alert(Alert.AlertType type, String message, Stage stage) {
        Alert alert = new Alert(type);
        alert.initOwner(stage);
        return alertCommon(type,message,alert);

    }
    public static Optional<ButtonType> alertCommon( Alert.AlertType type, String message,Alert alert) {
        if(null != type) {
            switch (type) {
                case INFORMATION:
                    alert.setTitle("Information");
                    break;
                case WARNING:
                    alert.setTitle("Warning");
                    break;
                case CONFIRMATION:
                    alert.setTitle("Confirmation");
                    break;
            }
        }
        alert.setHeaderText(null);
        alert.setContentText(message);

        return alert.showAndWait();

    }

}
