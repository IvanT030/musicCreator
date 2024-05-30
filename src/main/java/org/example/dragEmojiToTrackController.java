package org.example;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Label;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.Node;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.ResourceBundle;

public class dragEmojiToTrackController implements Initializable {

    @FXML
    private ListView<String> emojiListView;
    @FXML
    private AnchorPane trackContainer;
    private EmojiMusicMap emojiMusicMap;
    private Pane timeAxisPane;
    private Line timeLine;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(() -> {
            emojiMusicMap = new EmojiMusicMap();
            for (String key : emojiMusicMap.getEmojiMusicMap().keySet()) {
                EmojiMusicMap.EmojiMusicEntry entry = emojiMusicMap.getEmojiMusicMap().get(key);
                String displayText = entry.getEmoji() + " " + key;
                emojiListView.getItems().add(displayText);
            }
            System.out.println(emojiListView.getItems());
        });

        emojiListView.setCellFactory(lv -> {
            var cell = new ListCell<String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item);
                }
            };

            cell.setOnDragDetected(event -> {
                if (!cell.isEmpty()) {
                    Dragboard db = cell.startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent content = new ClipboardContent();
                    content.putString(cell.getItem());
                    db.setContent(content);
                    event.consume();
                }
            });

            return cell;
        });

        trackContainer.setOnDragOver(event -> {
            if (event.getGestureSource() != trackContainer && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        trackContainer.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                Label dropped = new Label(db.getString());
                dropped.setStyle("-fx-background-color: lightblue; -fx-border-color: blue; -fx-padding: 5px;");
                dropped.setPrefHeight(100); // 設置與軌道相同的高度
                dropped.setPrefWidth(100); // 可根據需要調整
                Pane closestTrack = findClosestTrack(event.getY());
                if (closestTrack != null) {
                    dropped.setLayoutY(0); // 在軌道內部，y位置設為0
                    dropped.setLayoutX(event.getX() - closestTrack.getLayoutX()); // 計算相對於軌道的x位置
                    closestTrack.getChildren().add(dropped);
                }
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        // 添加時間軸Pane
        timeAxisPane = createTimeAxis();
        trackContainer.getChildren().add(timeAxisPane);

        // 動態添加多個軌道
        for (int i = 0; i < 5; i++) {
            Pane track = createTrack(i * 100 + 30, 100, 2000); // 30作為時間軸的高度
            trackContainer.getChildren().add(track);
        }

        // 添加時間線
        timeLine = new Line(0, 0, 0, trackContainer.getChildren().size() * 100); // 假設每個軌道高度為100
        timeLine.setStyle("-fx-stroke: red; -fx-stroke-width: 2;");
        trackContainer.getChildren().add(timeLine);
    }

    private Pane findClosestTrack(double y) {
        Pane closestTrack = null;
        double minDistance = Double.MAX_VALUE;
        for (Node node : trackContainer.getChildren()) {
            if (node instanceof Pane) {
                Pane track = (Pane) node;
                double distance = Math.abs(track.getLayoutY() - y);
                if (distance < minDistance) {
                    minDistance = distance;
                    closestTrack = track;
                }
            }
        }
        return closestTrack;
    }

    private Pane createTrack(double yOffset, double height, double width) {
        Pane track = new Pane();
        track.setStyle("-fx-border-color: black; -fx-background-color: #333333;");
        track.setLayoutY(yOffset); // 从顶部开始排列轨道
        track.setPrefHeight(height);
        track.setPrefWidth(width); // 假设每个轨道的宽度
        return track;
    }

    private Pane createTimeAxis() {
        Pane timeAxis = new Pane();
        timeAxis.setStyle("-fx-background-color: lightgray;");
        timeAxis.setLayoutY(0); // 时间轴位于顶部
        timeAxis.setPrefHeight(30); // 时间轴高度
        timeAxis.setPrefWidth(2000); // 时间轴宽度

        int interval = 100; // 假设每个间隔为100像素，表示1秒
        for (int i = 0; i < 20; i++) { // 假设总共20秒
            Line line = new Line(i * interval, 0, i * interval, 30);
            line.setStyle("-fx-stroke: rgba(37,37,37,0.76);");
            Text text = new Text(i + "s");
            text.setLayoutX(i * interval);
            text.setLayoutY(20);
            timeAxis.getChildren().addAll(line, text);
        }
        return timeAxis;
    }

    public void updateTimelinePosition(double currentTime) {
        Platform.runLater(() -> {
            double x = currentTime * 100; // 假设每秒对应100像素
            timeLine.setStartX(x);
            timeLine.setEndX(x);
        });
    }
}