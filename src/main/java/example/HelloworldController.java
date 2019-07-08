package example;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.DirectoryChooserBuilder;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import de.felixroske.jfxsupport.FXMLController;
import javafx.event.Event;
import javafx.fxml.FXML;

import java.io.File;
import java.net.URL;
import java.time.Instant;
import java.util.ResourceBundle;

@FXMLController
public class HelloworldController implements Initializable {

    @Autowired
    HitachiWaferUtil hitachiWaferUtil;
    @FXML
    private Label helloLabel;

    @FXML
    private TextField nameField;
    @FXML
    private TextArea text;
    @FXML
    ChoiceBox angleNum;

    public TextArea getText() {
        return text;
    }

    public void setText(TextArea text) {
        this.text = text;
    }

    @FXML
    public void exportclick(Event event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("选择文件");
//        FileChooser fileChooser = new FileChooser();
        Stage stage = new Stage();
        final File selectedDirectory = chooser.showDialog(stage);
        if (selectedDirectory != null) {
            nameField.setText(selectedDirectory.getAbsolutePath());
        }

    }

    @FXML
    private void setHelloText(final Event event) {
        String angle = (String) angleNum.getValue();
        System.out.println(angle);
        String text = nameField.getText();
        angle = angle.trim();
        if (StringUtils.isEmpty(angle)) {
            angle = "0";
        }
        if (angle.equals("0") || angle.equals("90") || angle.equals("180") || angle.equals("270")) {

        } else {
            helloLabel.setText("angle  不正确，只能是0，90，180，270");
            return;
        }
        text = text.trim();
        if (StringUtils.isEmpty(text)) {
            helloLabel.setText("请填写文件路径！！！");
            return;
        }
        System.out.println("文件路径为：" + text);
        File file = new File(text);

        try {
            if (!file.exists()) {
                CommonUiUtil.alert(Alert.AlertType.INFORMATION, "请填写正确文件路径！！！");
                return;
            }
            hitachiWaferUtil.waferParse(file, angle, text, true);
            CommonUiUtil.alert(Alert.AlertType.INFORMATION, "wafer 文件转换成功");
        } catch (Exception e) {
            e.printStackTrace();
            CommonUiUtil.alert(Alert.AlertType.ERROR, "wafer 文件转换失败!!!");
        }


    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        nameField.setEditable(false);
        ObservableList<String> options = FXCollections.observableArrayList("0", "90", "180", "270");
        angleNum.setItems(options);
        angleNum.setValue("0");
    }
}
