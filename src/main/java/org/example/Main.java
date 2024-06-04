package org.example;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Parent;
import java.io.IOException;
import java.util.Map;

public class Main extends Application {

    private static Parent parent;
    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmlFile/standard.fxml"));
        parent = loader.load();
        parent.setUserData(loader); // Store FXMLLoader in UserData

        scene = new Scene(parent);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    public static FXMLLoader getLoader() {
        if (parent != null && parent.getUserData() instanceof FXMLLoader) {
            return (FXMLLoader) parent.getUserData();
        }
        return null;
    }

    public static Parent getParent(){
        return parent;
    }

}