package org.example;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

public class AudioClipper {

    public static void main(String[] args) {
        String inputFilePath = "src\\main\\java\\org\\example\\Test.wav";
        String outputFilePath = "src\\main\\java\\org\\example\\output4.wav";
        float startSeconds = 10.0f;  // 剪掉開始的秒數
        float endSeconds = 20.0f;    // 剪掉結束的秒數

        try {
            File inputFile = new File(inputFilePath);
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(inputFile);
            AudioFormat format = audioInputStream.getFormat();

            // 轉換秒數到字節數
            long startByte = (long) (startSeconds * format.getFrameRate() * format.getFrameSize());
            long endByte = (long) (endSeconds * format.getFrameRate() * format.getFrameSize());

            // 跳過開始部分
            audioInputStream.skip(startByte);

            // 計算裁剪部分的長度
            long clipLength = endByte - startByte;

            // 讀取剪裁部分的音頻數據
            byte[] buffer = new byte[(int) clipLength];
            audioInputStream.read(buffer, 0, (int) clipLength);

            // 創建新的AudioInputStream
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);
            AudioInputStream clippedAudioInputStream = new AudioInputStream(byteArrayInputStream, format, buffer.length / format.getFrameSize());

            // 將剪裁後的音頻寫入文件
            File outputFile = new File(outputFilePath);
            AudioSystem.write(clippedAudioInputStream, AudioFileFormat.Type.WAVE, outputFile);

            System.out.println("音頻文件剪裁成功！");
        } catch (UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
        }
    }
}
