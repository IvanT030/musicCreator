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
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import com.google.gson.JsonParser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import javafx.scene.control.SplitPane;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Parent;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class EmojiAndTrack implements Initializable {
    public static int defaultTrackSeconds = 16;
    @FXML
    private ListView<String> emojiListView;
    @FXML
    private SplitPane splitPane;
    @FXML
    private AnchorPane trackContainer;
    private EmojiMusicMap emojiMusicMap;
    private Pane timeAxisPane;
    private Line timeLine;
    private VBox trackLabelsContainer; // 用于存放轨道标签的 VBox
    private boolean adjusting = false;
    private final String emojiOnTrackPath = "src/main/resources/musicConfig/inTrackMusicConfig.json";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(() -> {
            emojiMusicMap = new EmojiMusicMap();
            for (String key : emojiMusicMap.getEmojiMusicMap().keySet()) {
                EmojiMusicMap.EmojiMusicEntry entry = emojiMusicMap.getEmojiMusicMap().get(key);
                String displayText = entry.getEmoji() + " " + key;
                emojiListView.getItems().add(displayText);
            }
            loadJsonFile();
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
                    String emojiName = cell.getItem().split(" ")[1];
                    content.putString(emojiName);
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
                String emojiName = db.getString();
                EmojiMusicMap.EmojiMusicEntry entry = emojiMusicMap.getEmojiMusicMap().get(emojiName);

                Label dropped = new Label(entry.getEmoji() + " " + emojiName);
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

                    // 获取轨道的索引
                    int trackIndex = trackContainer.getChildren().indexOf(closestTrack) - 1;
                    System.out.println(trackIndex);
                    // 创建 JSON 对象
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("ontrack", trackIndex);
                    jsonObject.addProperty("motify", "0");
                    jsonObject.addProperty("startTime", newX / 100); // 假设每个像素代表0.01秒
                    jsonObject.addProperty("durationTime", 2.6); // 假设持续时间

                    // 更新 JSON 文件
                    updateJOnTrackJsonFile(emojiName, jsonObject);
                }
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
        // 添加时间轴Pane
        timeAxisPane = createTimeAxis();
        timeAxisPane.setId("timeAxisPane"); // 设置时间轴的ID
        trackContainer.getChildren().add(timeAxisPane);

        // 初始化轨道标签容器
        trackLabelsContainer = new VBox();
        trackLabelsContainer.setLayoutY(30); // 设置Y坐标，避开时间轴
        trackLabelsContainer.setSpacing(30); // 设置间距
        trackContainer.getChildren().add(trackLabelsContainer);

        // 动态添加多个轨道和对应的标签
        for (int i = 0; i < 5; i++) {
            Pane track = createTrack(i * 100 + 30, 100, defaultTrackSeconds * 100); // 30作为时间轴的高度
            track.setId("trackPane" + i); // 设置轨道的ID
            trackContainer.getChildren().add(track);
            Label trackLabel = new Label("track" + (i + 1));
            trackLabelsContainer.getChildren().add(trackLabel);
        }

        // 添加时间线
        timeLine = new Line(0, 30, 0, trackContainer.getChildren().size() * 100 + 30); // 假设每个轨道高度为100
        timeLine.setStyle("-fx-stroke: red; -fx-stroke-width: 2;");
        trackContainer.getChildren().add(timeLine);

        // 添加贯穿所有轨道的时间线
        addVerticalTimeLines();

        splitPane.getDividers().get(1).positionProperty().addListener((observable, oldValue, newValue) -> {
            if (!adjusting) {
                adjustDividersFromRight(newValue.doubleValue());
            }
        });

        splitPane.getDividers().get(0).positionProperty().addListener((observable, oldValue, newValue) -> {
            if (!adjusting) {
                adjustDividersFromLeft(newValue.doubleValue());
            }
        });
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

    private final double middleSize = 0.065;
    private void adjustDividersFromRight(double newRightPosition) {
        adjusting = true;
        double newLeftPosition = newRightPosition - middleSize;

        if (newLeftPosition >= 0 && newLeftPosition <= 1) {
            splitPane.getDividers().get(0).setPosition(newLeftPosition);
        }

        adjusting = false;
    }

    private void adjustDividersFromLeft(double newLeftPosition) {
        adjusting = true;

        double newRightPosition = newLeftPosition + middleSize;

        if (newRightPosition >= 0 && newRightPosition <= 1) {
            splitPane.getDividers().get(1).setPosition(newRightPosition);
        }

        adjusting = false;
    }

    private void loadJsonFile() {
        try (FileReader reader = new FileReader(emojiOnTrackPath)) {
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
            for (String key : jsonObject.keySet()) {
                JsonObject trackInfo = jsonObject.getAsJsonObject(key);
                int trackIndex = trackInfo.get("ontrack").getAsInt();
                double startTime = trackInfo.get("startTime").getAsDouble();
                String emoji = trackInfo.get("emoji").getAsString();
                String path = trackInfo.get("path").getAsString();

                // 添加到对应的轨道上
                Pane track = (Pane) trackContainer.getChildren().get(trackIndex);
                Label label = new Label(emoji + " " + key);
                label.setStyle("-fx-background-color: lightblue; -fx-border-color: blue; -fx-padding: 5px;");
                label.setPrefHeight(100); // 設置與軌道相同的高度
                label.setPrefWidth(100); // 可根據需要調整
                label.setLayoutX(startTime * 100); // 根据时间位置设置X坐标
                label.setLayoutY((track.getPrefHeight() - label.getPrefHeight()) / 2); // 设置Y位置为轨道的中间
                track.getChildren().add(label);

                // 添加到emojiMusicMap中
                emojiMusicMap.getEmojiMusicMap().put(key, new EmojiMusicMap.EmojiMusicEntry(emoji, path));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateJOnTrackJsonFile(String baseKey, JsonObject newObject) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject jsonObject;
        try (FileReader reader = new FileReader(emojiOnTrackPath)) {
            jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            jsonObject = new JsonObject();
        }

        String key = baseKey;
        int count = 1;

        while (jsonObject.has(key)) {
            key = baseKey + count;
            count++;
        }

        jsonObject.add(key, newObject);

        try (FileWriter writer = new FileWriter(emojiOnTrackPath)) {
            gson.toJson(jsonObject, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void modifyJsonObject(JsonObject jsonObject, int trackIndex, double startTime, String emoji, String path) {
        jsonObject.addProperty("motify", "1");
        jsonObject.addProperty("ontrack", trackIndex);
        jsonObject.addProperty("startTime", startTime);
        jsonObject.addProperty("durationTime", 2.6); // 假设持续时间
        jsonObject.addProperty("emoji", emoji);
        jsonObject.addProperty("path", path);
    }

    private void loadNewFXML(String fxmlFile) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fxmlFile));
        Parent root = fxmlLoader.load();
        Stage stage = new Stage();
        stage.setScene(new Scene(root));
        stage.show();
    }
}