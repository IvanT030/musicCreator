package org.example;
/*移動的時間不會變*/
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.*;
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
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.Parent;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class EmojiAndTrack implements Initializable{

    public static int defaultTrackSeconds = 16;
    @FXML
    private Button exportButton;
    @FXML
    private ListView<String> emojiListView;
    @FXML
    private SplitPane splitPane;
    @FXML
    private ScrollPane detailsScrollPane;
    @FXML
    private AnchorPane trackContainer;
    @FXML
    private EmojiMusicMap emojiMusicMap;
    @FXML
    private ScrollPane trackScrollPane;
    private Pane timeAxisPane;
    private Line timeLine;
    private VBox trackLabelsContainer; // 用于存放轨道标签的 VBox
    private boolean adjusting = false;
    private final String emojiOnTrackPath = "src/main/resources/musicConfig/inTrackMusicConfig.json";
    final JsonObject[] deletedData = {null};

    @Override
    public void initialize(URL location, ResourceBundle resources)  {
        Platform.runLater(() -> {
            emojiMusicMap = new EmojiMusicMap();
            for (String key : emojiMusicMap.getEmojiMusicMap().keySet()) {
                EmojiMusicMap.EmojiMusicEntry entry = emojiMusicMap.getEmojiMusicMap().get(key);
                String displayText = entry.getEmoji() + " " + key;
                emojiListView.getItems().add(displayText);
            }
            loadJsonFile();

            Tooltip saveTooltip = new Tooltip("Export Music");
            saveTooltip.setShowDelay(javafx.util.Duration.ZERO);
            exportButton.setTooltip(saveTooltip);

            trackScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            trackScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
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
                    deletedData[0] = null;
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
                String emojiLabelName = db.getString(); //emoji + " " + emojiName
                String emojiName = emojiLabelName.split(" ")[1];
                Label dropped = new Label(emojiLabelName);
                if(deletedData[0] != null){
                    if(deletedData[0].get("motify").getAsInt() == 1){
                        dropped.setPrefWidth(100 * deletedData[0].get("musicLength").getAsDouble());
                    }else{
                        dropped.setPrefWidth(100 * emojiMusicMap.getEmojiMusicMap().get(deletedData[0].get("origin").getAsString()).getMusicLength());
                    }
                }
                else {
                    dropped.setPrefWidth(100 * emojiMusicMap.getEmojiMusicMap().get(emojiName).getMusicLength()); // 可根據需要調整
                }
                dropped.setStyle("-fx-background-color: lightblue; -fx-border-color: blue; -fx-padding: 5px;");
                dropped.setPrefHeight(100); // 設置與軌道相同的高度

                //label的mouseClick事件
                dropped.setOnMouseClicked(e -> {
                    try {
                        if(e.getButton() == MouseButton.PRIMARY){
                            dropped.setStyle("-fx-background-color: lightgreen; -fx-border-color: green; -fx-padding: 5px;"); // 用不同颜色表示选中状态

                            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmlFile/standardWithDetail.fxml"));
                            Parent root = loader.load();

                            // 创建一个新的舞台并显示
                            Stage stage = (Stage) trackContainer.getScene().getWindow();
                            stage.setScene(new Scene(root));
                            stage.show();

                            detailsScrollPane = (ScrollPane) root.lookup("#detailsScrollPane");
                            if (detailsScrollPane != null) {
                                detailsScrollPane.setVisible(true);

                                // 查找并加载嵌入的 ButtonController.fxml
                                FXMLLoader buttonLoader = new FXMLLoader(getClass().getResource("/fxmlFile/ButtonController.fxml"));
                                VBox detailVBox = buttonLoader.load();

                                // 设置加载的内容到 detailsScrollPane
                                detailsScrollPane.setContent(detailVBox);

                                // 获取 AudioController 的实例
                                AudioController audioController = buttonLoader.getController();
                                if (audioController != null) {
                                    // 传递字符串给 AudioController
                                    audioController.loadAudioDetails(emojiName);
                                    System.out.println("send audioController " + emojiName);
                                } else {
                                    System.out.println("AudioController is null");
                                }
                            }
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                });

                dropped.setOnDragDetected(e -> {
                    Dragboard dbontrack = dropped.startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent content = new ClipboardContent();
                    String trackEmojiName = dropped.getText();
                    System.out.println("set on drag detected" + trackEmojiName);
                    content.putString(trackEmojiName);
                    dbontrack.setContent(content);
                    try (FileReader reader = new FileReader(emojiOnTrackPath)) {
                        JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();

                        // 删除目標数据并保存到变量
                        deletedData[0] = jsonObject.remove(trackEmojiName.split(" ")[1]).getAsJsonObject();

                        // 保存修改后的 JSON 到文件
                        writeJsonFile(emojiOnTrackPath, jsonObject);

                    } catch (IOException exp) {
                        exp.printStackTrace();
                    }
                    dbontrack.setDragView(dropped.snapshot(null, null));
                    e.consume();
                    Pane parent = (Pane) dropped.getParent();
                    parent.getChildren().remove(dropped);
                });

                dropped.setOnDragOver(e -> {
                    if (e.getGestureSource() == dropped && e.getDragboard().hasString()) {
                        dropped.setLayoutX(e.getSceneX() - dropped.getWidth() / 2);
                        dropped.setLayoutY(e.getSceneY() - dropped.getHeight() / 2);
                        e.acceptTransferModes(TransferMode.MOVE);
                    }
                    e.consume();
                });

                Pane closestTrack = findClosestTrack(event.getY());
                if (closestTrack != null) {
                    double newX = findAvailablePosition(closestTrack, event.getX() - closestTrack.getLayoutX(), dropped.getPrefWidth());
                    // 检查是否需要扩展轨道
                    if (newX + dropped.getPrefWidth() > closestTrack.getPrefWidth()) {
                        while (newX + dropped.getPrefWidth() > defaultTrackSeconds * 100) {
                            expandTrackContainer();
                        }
                        newX = findAvailablePosition(closestTrack, newX, dropped.getPrefWidth());
                    }
                    dropped.setLayoutY((closestTrack.getPrefHeight() - dropped.getPrefHeight()) / 2); // 设置Y位置为轨道的中间
                    dropped.setLayoutX(newX); // 計算相對於軌道的x位置
                    closestTrack.getChildren().add(dropped);

                    // 获取轨道的索引
                    int trackIndex = trackContainer.getChildren().indexOf(closestTrack) - 1;
                    // 创建 JSON 对象
                    JsonObject jsonObject = new JsonObject();
                    if(deletedData[0] != null){
                        jsonObject.addProperty("origin", deletedData[0].get("origin").getAsString());
                        jsonObject.addProperty("ontrack", trackIndex);
                        if(deletedData[0].get("motify").getAsInt() == 1){
                            jsonObject.addProperty("motify", "1");
                            jsonObject.addProperty("path", deletedData[0].get("path").getAsString());
                            jsonObject.addProperty("musicLength", deletedData[0].get("musicLength").getAsDouble());
                            jsonObject.addProperty("emoji", deletedData[0].get("emoji").getAsString());
                        }else{
                            jsonObject.addProperty("motify", "0");
                        }
                        jsonObject.addProperty("startTime", newX / 100);
                        deletedData[0] = null;
                    }else {
                        jsonObject.addProperty("origin", emojiName);
                        jsonObject.addProperty("ontrack", trackIndex);
                        jsonObject.addProperty("motify", "0");
                        jsonObject.addProperty("startTime", newX / 100); // 假设每个像素代表0.01秒
                    }
                    // 更新 JSON 文件
                    updateJOnTrackJsonFile(emojiName, jsonObject, dropped);
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
        trackLabelsContainer.setId("trackLabelsContainer");
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

    @FXML
    private void export() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Audio File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Audio Files", "*.wav"));
        File file = fileChooser.showSaveDialog(exportButton.getScene().getWindow());

        if (file != null) {
            try {
                String emojiMusicPath = "src/main/resources/musicConfig/emojiAndMusicConfig.json";
                String OUTPUT_PATH = file.getAbsolutePath();
                AudioMerger.mergeAudioFilesFromConfig(emojiOnTrackPath, emojiMusicPath, OUTPUT_PATH);
                System.out.println("File saved to: " + OUTPUT_PATH);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Save operation was cancelled.");
        }
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
            if (node instanceof Pane && !"timeAxisPane".equals(node.getId()) && !"trackLabelsContainer".equals(node.getId())) { // 排除时间轴Pane
                Pane track = (Pane) node;
                double distance = Math.abs(track.getLayoutY() + track.getPrefHeight() / 2 - y);
                if (distance < minDistance) {
                    minDistance = distance;
                    closestTrack = track;
                }
            }
        }
        System.out.println("findClosestTrack" + closestTrack);
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
                System.out.println("existingLabel" + existingLabel.getText());
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
        JsonObject jsonObject = null;
        try (FileReader reader = new FileReader(emojiOnTrackPath)) {
            jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (jsonObject != null) {
            for (String key : jsonObject.keySet()) {
                JsonObject trackInfo = jsonObject.getAsJsonObject(key);
                int trackIndex = trackInfo.get("ontrack").getAsInt() + 1;
                int hasModify = trackInfo.get("motify").getAsInt();
                double startTime = trackInfo.get("startTime").getAsDouble();
                double musicLength;
                String emoji;
                if (hasModify == 1) {
                    emoji = trackInfo.get("emoji").getAsString();
                    musicLength = trackInfo.get("musicLength").getAsDouble();
                } else {
                    emoji = emojiMusicMap.getEmojiMusicMap().get(trackInfo.get("origin").getAsString()).getEmoji();
                    musicLength = emojiMusicMap.getEmojiMusicMap().get(trackInfo.get("origin").getAsString()).getMusicLength();
                }

                // 檢查是否需要擴展軌道容器
                if (startTime + musicLength > defaultTrackSeconds) {
                    while (startTime + musicLength > defaultTrackSeconds) {
                        expandTrackContainer();
                    }
                }

                // 添加到對應的軌道上
                Pane track = (Pane) trackContainer.getChildren().get(trackIndex);
                Label label = new Label(emoji + " " + key);
                label.setStyle("-fx-background-color: lightblue; -fx-border-color: blue; -fx-padding: 5px;");
                label.setPrefHeight(100);
                label.setPrefWidth(100 * musicLength);
                label.setLayoutX(startTime * 100);
                label.setLayoutY((track.getPrefHeight() - label.getPrefHeight()) / 2);
                track.getChildren().add(label);

                // 添加點擊事件
                label.setOnMouseClicked(e -> {
                    try {
                        if (e.getButton() == MouseButton.PRIMARY) {
                            label.setStyle("-fx-background-color: lightgreen; -fx-border-color: green; -fx-padding: 5px;"); // 用不同颜色表示选中状态

                            //判斷有沒有已經仔入過，不然每次按一下都要載入
                            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmlFile/standardWithDetail.fxml"));
                            Parent root = loader.load();
                            Stage stage = (Stage) trackContainer.getScene().getWindow();
                            stage.setScene(new Scene(root));
                            stage.show();
                            detailsScrollPane = (ScrollPane) root.lookup("#detailsScrollPane");
                            if (detailsScrollPane != null) {
                                detailsScrollPane.setVisible(true);
                                FXMLLoader buttonLoader = new FXMLLoader(getClass().getResource("/fxmlFile/ButtonController.fxml"));
                                VBox detailVBox = buttonLoader.load();
                                detailsScrollPane.setContent(detailVBox);
                                AudioController audioController = buttonLoader.getController();
                                if (audioController != null) {
                                    audioController.loadAudioDetails(key);
                                }
                            }
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                });

                label.setOnDragDetected(event -> {
                    Dragboard db = label.startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent content = new ClipboardContent();
                    content.putString(label.getText());
                    db.setContent(content);

                    System.out.println("before deleting");
                    try (FileReader reader = new FileReader(emojiOnTrackPath)) {
                        JsonObject tempJsonObject = JsonParser.parseReader(reader).getAsJsonObject();

                        // 删除目標数据并保存到变量
                        deletedData[0] = tempJsonObject.remove(label.getText().split(" ")[1]).getAsJsonObject();

                        // 保存修改后的 JSON 到文件
                        writeJsonFile(emojiOnTrackPath, tempJsonObject);

                    } catch (IOException exp) {
                        exp.printStackTrace();
                    }
                    db.setDragView(label.snapshot(null, null));
                    event.consume();
                    track.getChildren().remove(label);
                    System.out.println("after deleting");
                });

                label.setOnDragOver(e -> {
                    if (e.getGestureSource() == label && e.getDragboard().hasString()) {
                        label.setLayoutX(e.getSceneX() - label.getWidth() / 2);
                        label.setLayoutY(e.getSceneY() - label.getHeight() / 2);
                        e.acceptTransferModes(TransferMode.MOVE);
                    }
                    e.consume();
                });
            }
        }
    }
    private void writeJsonFile(String filePath, JsonObject jsonObject) {
        try (FileWriter writer = new FileWriter(filePath)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            writer.write(gson.toJson(jsonObject));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void updateJOnTrackJsonFile(String baseKey, JsonObject newObject, Label dropped) {
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

        dropped.setText(dropped.getText().split(" ")[0] + " " + key);

        try (FileWriter writer = new FileWriter(emojiOnTrackPath)) {
            gson.toJson(jsonObject, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}