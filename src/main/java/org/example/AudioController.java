package org.example;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import com.google.gson.JsonParser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.stage.Stage;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AudioController implements Initializable {

    @FXML
    private Slider speedSlider;
    @FXML
    private TextField speedTextField;
    private static String detailKey;
    private AudioSpeedChange audioSpeedChange;
    private String originMusicPath;
    private String destinationMusicPath;
    private float speedFactor;
    private final String emojiOnTrackPath = "src/main/resources/musicConfig/inTrackMusicConfig.json";
    private final String emojiMusicPath = "src/main/resources/musicConfig/emojiAndMusicConfig.json";

    @FXML

    public void initialize(URL location, ResourceBundle resources) {
        speedSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            speedTextField.setText(String.format("%.1f", newValue.doubleValue()));
        });

        // 设置Slider监听器
        speedSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            speedFactor = newValue.floatValue();
            audioSpeedChange.setSpeedFactor(speedFactor);
            try {
                if (speedFactor >= speedSlider.getMin() && speedFactor <= speedSlider.getMax()) {
                    speedSlider.setValue(speedFactor);
                }
            } catch (NumberFormatException e) {
                // Handle invalid input
                if (speedFactor < speedSlider.getMin()) {
                    speedSlider.setValue(speedSlider.getMin());
                }
                if (speedFactor > speedSlider.getMax()) {
                    speedSlider.setValue(speedSlider.getMax());
                }
                speedTextField.setText(String.format("%.1f", speedSlider.getValue()));
            }
        });

        // 确保文本框显示初始滑条值
        speedTextField.setText(String.format("%.1f", speedSlider.getValue()));

        // 为文本框添加回车键事件处理
        speedTextField.setOnAction(event -> {
            try {
                float value = Float.parseFloat(speedTextField.getText());
                if (value < speedSlider.getMin()) {
                    value = (float) speedSlider.getMin();
                }
                if (value > speedSlider.getMax()) {
                    value = (float) speedSlider.getMax();
                }
                speedSlider.setValue(value);
            } catch (NumberFormatException e) {
                // 如果输入的不是一个有效的数字，恢复为当前滑条的值
                speedTextField.setText(String.format("%.1f", speedSlider.getValue()));
            }
        });
    }

    public void loadAudioDetails(String detailKey) {
        this.detailKey = detailKey;
        System.out.println("Loading audio details from " + detailKey);

        try (FileReader reader1 = new FileReader(emojiOnTrackPath);
             FileReader reader2 = new FileReader(emojiMusicPath)) {

            JsonObject jsonObject = JsonParser.parseReader(reader1).getAsJsonObject();
            JsonObject jsonObject2 = JsonParser.parseReader(reader2).getAsJsonObject();

            if (jsonObject.has(detailKey)) {
                JsonObject trackInfo = jsonObject.getAsJsonObject(detailKey);
                int hasModify = trackInfo.get("motify").getAsInt();

                if (hasModify == 1) {
                    destinationMusicPath = trackInfo.get("path").getAsString();
                    originMusicPath = destinationMusicPath;
                } else {
                    String originKey = trackInfo.get("origin").getAsString();

                    if (jsonObject2.has(originKey)) {
                        JsonObject entry2 = jsonObject2.getAsJsonObject(originKey);
                        originMusicPath = entry2.get("path").getAsString();

                        // Update trackInfo
                        trackInfo.addProperty("motify", 1);
                        destinationMusicPath = "src/main/resources/soundEffectOnTrack/" + detailKey + ".wav";

                    } else {
                        System.out.println(detailKey + "load not found in emojiMusicPath JSON data.");
                    }
                }
            } else {
                System.out.println("loadAudioDetails not found intrackmusicconfig JSON data.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeBackJsonfile() {
        try (FileReader reader1 = new FileReader(emojiOnTrackPath);
             FileReader reader2 = new FileReader(emojiMusicPath)) {

            JsonObject jsonObject = JsonParser.parseReader(reader1).getAsJsonObject();
            JsonObject jsonObject2 = JsonParser.parseReader(reader2).getAsJsonObject();

            if (jsonObject.has(detailKey)) {
                JsonObject trackInfo = jsonObject.getAsJsonObject(detailKey);
                trackInfo.addProperty("motify", 1);
                trackInfo.addProperty("path", destinationMusicPath);
                trackInfo.addProperty("musicLength", getWavFileDuration(destinationMusicPath));

                String originKey = trackInfo.get("origin").getAsString();

                if (jsonObject2.has(originKey)) {
                    JsonObject entry2 = jsonObject2.getAsJsonObject(originKey);
                    String emoji = entry2.get("emoji").getAsString();
                    trackInfo.addProperty("emoji", emoji);
                }

                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String prettyJson = gson.toJson(jsonObject);

                try (FileWriter writer = new FileWriter(emojiOnTrackPath)) {
                    writer.write(prettyJson);
                }
            } else {
                System.out.println("writeBackJsonfile not found in intrackmusicconfig JSON data.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public double getWavFileDuration(String filePath) {
        File file = new File(filePath);
        try {
            AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(file);
            if (fileFormat.getType() != AudioFileFormat.Type.WAVE) {
                throw new UnsupportedAudioFileException("Not a WAV file");
            }

            AudioFormat format = fileFormat.getFormat();
            long frames = fileFormat.getFrameLength();
            double durationInSeconds = (frames + 0.0) / format.getFrameRate();

            return durationInSeconds;
        } catch (UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
            return -1;
        }
    }
    private void processAudioFile(String inputPath, String outputPath) {
        AudioSpeedChange audioSpeedChange = new AudioSpeedChange(inputPath, outputPath);
        audioSpeedChange.setSpeedFactor((float) speedSlider.getValue());
        audioSpeedChange.processAudio();
    }

    @FXML
    private void handleConfirm() {
        System.out.println("processAudioFile");
        processAudioFile(originMusicPath, destinationMusicPath);
        writeBackJsonfile();
    }

    @FXML
    private void handleCancel() {
        // 将滑条的值重置为原始速度

        speedSlider.setValue(1.0);
    }

}