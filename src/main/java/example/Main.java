package example;

import org.springframework.boot.autoconfigure.SpringBootApplication;

import de.felixroske.jfxsupport.AbstractJavaFxApplicationSupport;

import java.util.Arrays;

@SpringBootApplication
public class Main extends AbstractJavaFxApplicationSupport {

    public static void main(String[] args) {
        launch(Main.class, HelloworldView.class, new SplashScreenCofig(), args);
    }
}
