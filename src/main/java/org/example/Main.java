package org.example;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.control.ListView;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Main extends Application {

    //表情符號對應的音效路徑
    private Map<String, String> emojiMusicMap;
    @Override
    public void start(Stage stage) throws IOException {
        ListView<String> emojiListView = new ListView<>();
        Parent parent = FXMLLoader.load(getClass().getResource("/fxmlFile/1.fxml"));
        stage.setScene(new Scene(parent));
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }

}