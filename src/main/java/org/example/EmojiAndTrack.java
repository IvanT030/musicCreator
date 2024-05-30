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

public class EmojiAndTrack implements Initializable {
    public static int defaultTrackSeconds = 16;
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
                    double newX = findAvailablePosition(closestTrack, event.getX() - closestTrack.getLayoutX(), dropped.getPrefWidth());
                    if (newX + dropped.getPrefWidth() > closestTrack.getPrefWidth()) {
                        expandTrackContainer();
                        newX = findAvailablePosition(closestTrack, newX, dropped.getPrefWidth());
                    }
                    dropped.setLayoutY((closestTrack.getPrefHeight() - dropped.getPrefHeight()) / 2); // 设置Y位置为轨道的中间
                    dropped.setLayoutX(newX); // 計算相對於軌道的x位置
                    closestTrack.getChildren().add(dropped);
                } else {
                    Pane newTrack = createTrack(trackContainer.getChildren().size() * 100 + 30, 100, defaultTrackSeconds * 100);
                    newTrack.setId("trackPane" + trackContainer.getChildren().size());
                    dropped.setLayoutY((newTrack.getPrefHeight() - dropped.getPrefHeight()) / 2);
                    dropped.setLayoutX(event.getX());
                    newTrack.getChildren().add(dropped);
                    trackContainer.getChildren().add(newTrack);
                }
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        // 添加時間軸Pane
        timeAxisPane = createTimeAxis();
        timeAxisPane.setId("timeAxisPane"); // 设置时间轴的ID
        trackContainer.getChildren().add(timeAxisPane);

        // 动态添加多個軌道
        for (int i = 0; i < 5; i++) {
            Pane track = createTrack(i * 100 + 30, 100, defaultTrackSeconds * 100); // 30作为时间轴的高度
            track.setId("trackPane" + i); // 设置轨道的ID
            trackContainer.getChildren().add(track);
        }

        // 添加时间线
        timeLine = new Line(0, 30, 0, trackContainer.getChildren().size() * 100 + 30); // 假设每个轨道高度为100
        timeLine.setStyle("-fx-stroke: red; -fx-stroke-width: 2;");
        trackContainer.getChildren().add(timeLine);

        // 添加贯穿所有轨道的时间线
        addVerticalTimeLines();
    }

    private void addVerticalTimeLines() {
        int interval = 100; // 假设每个间隔为100像素，表示1秒
        for (int i = 0; i <= defaultTrackSeconds; i++) {
            double x = i * interval;
            Line verticalLine = new Line(x, 30, x, trackContainer.getChildren().size() * 100 + 30); // 贯穿所有轨道
            verticalLine.setStyle("-fx-stroke: white; -fx-stroke-width: 1;");
            trackContainer.getChildren().add(verticalLine);
        }
    }

    private void updateVerticalTimeLines() {
        // 移除原有的垂直时间线
        trackContainer.getChildren().removeIf(node -> node instanceof Line && node != timeLine);
        addVerticalTimeLines(); // 重新添加垂直时间线
    }

    private Pane findClosestTrack(double y) {
        Pane closestTrack = null;
        double minDistance = Double.MAX_VALUE;
        for (Node node : trackContainer.getChildren()) {
            if (node instanceof Pane && !"timeAxisPane".equals(node.getId())) { // 排除时间轴Pane
                Pane track = (Pane) node;
                double distance = Math.abs(track.getLayoutY() + track.getPrefHeight() / 2 - y);
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
        timeAxis.setPrefWidth(defaultTrackSeconds * 100); // 时间轴宽度

        int interval = 100; // 假设每个间隔为100像素，表示1秒
        for (int i = 0; i <= defaultTrackSeconds; i++) { // 假设总共16秒
            Line line = new Line(i * interval, 0, i * interval, 30);
            line.setStyle("-fx-stroke: rgba(37,37,37,0.76);");
            Text text = new Text(i + "s");
            text.setLayoutX(i * interval);
            text.setLayoutY(20);
            timeAxis.getChildren().addAll(line, text);
        }
        return timeAxis;
    }

    private void updateTimeAxis() {
        timeAxisPane.getChildren().clear();
        int interval = 100;
        timeAxisPane.setPrefWidth(defaultTrackSeconds * 100);

        for (int i = 0; i <= defaultTrackSeconds; i++) {
            double x = i * interval;
            Line line = new Line(x, 0, x, 30);
            line.setStyle("-fx-stroke: rgba(37,37,37,0.76);");
            Text text = new Text(i + "s");
            text.setLayoutX(x);
            text.setLayoutY(20);
            timeAxisPane.getChildren().addAll(line, text);
        }
    }

    private double findAvailablePosition(Pane track, double startX, double width) {
        double newX = startX;
        for (Node child : track.getChildren()) {
            if (child instanceof Label) {
                Label existingLabel = (Label) child;
                double existingX = existingLabel.getLayoutX();
                double existingWidth = existingLabel.getPrefWidth();
                if (newX + width > existingX && newX < existingX + existingWidth) {
                    newX = existingX + existingWidth;
                }
            }
        }
        return newX;
    }

    private void expandTrackContainer() {
        defaultTrackSeconds += 5;
        trackContainer.setPrefWidth(defaultTrackSeconds * 100);
        updateTimeAxis();
        for (Node node : trackContainer.getChildren()) {
            if (node instanceof Pane && !"timeAxisPane".equals(node.getId())) {
                Pane track = (Pane) node;
                track.setPrefWidth(defaultTrackSeconds * 100);
            }
        }
        updateVerticalTimeLines();
    }

    public void updateTimelinePosition(double currentTime) {
        Platform.runLater(() -> {
            double x = currentTime * 100; // 假设每秒对应100像素
            timeLine.setStartX(x);
            timeLine.setEndX(x + trackContainer.getHeight() - 30); // 设置timeLine的结束Y坐标
        });
    }
}