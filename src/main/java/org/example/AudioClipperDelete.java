package org.example;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class AudioClipperDelete {

    public static void main(String[] args) {
        String inputFilePath = "src\\main\\java\\org\\example\\Test.wav";
        String outputFilePath = "src\\main\\java\\org\\example\\output5.wav";
        float startSeconds = 10.0f;  // 要剪掉的開始秒數
        float endSeconds = 20.0f;    // 要剪掉的結束秒數

        try {
            File inputFile = new File(inputFilePath);
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(inputFile);
            AudioFormat format = audioInputStream.getFormat();

            // 轉換秒數到字節數
            long startByte = (long) (startSeconds * format.getFrameRate() * format.getFrameSize());
            long endByte = (long) (endSeconds * format.getFrameRate() * format.getFrameSize());

            // 用於保存結果的緩衝區
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // 讀取開始部分
            byte[] buffer = new byte[1024];
            long bytesRead = 0;
            while (bytesRead < startByte) {
                int bytesToRead = (int) Math.min(buffer.length, startByte - bytesRead);
                int read = audioInputStream.read(buffer, 0, bytesToRead);
                if (read == -1) break;
                outputStream.write(buffer, 0, read);
                bytesRead += read;
            }

            // 跳過要剪掉的部分
            audioInputStream.skip(endByte - startByte);

            // 讀取剩餘部分
            int read;
            while ((read = audioInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }

            // 創建新的AudioInputStream
            byte[] audioBytes = outputStream.toByteArray();
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(audioBytes);
            AudioInputStream resultAudioInputStream = new AudioInputStream(byteArrayInputStream, format, audioBytes.length / format.getFrameSize());

            // 將結果寫入新文件
            File outputFile = new File(outputFilePath);
            AudioSystem.write(resultAudioInputStream, AudioFileFormat.Type.WAVE, outputFile);

            System.out.println("音頻文件裁剪成功！");
        } catch (UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
        }
    }
}
