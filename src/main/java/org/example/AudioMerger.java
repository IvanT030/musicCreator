package org.example;

import javax.sound.sampled.*;
import java.io.*;

public class AudioMerger {

    public static void main(String[] args) {
        String audio1Path = "src/main/resources/soundEffect/Test.wav";
        String audio2Path = "src/main/resources/soundEffect/Test2.wav";
        String outputPath = "src/main/resources/soundEffect/output3.wav";
        int interval = -100000; // 間隔時間，單位是毫秒，可以是負數

        try {
            AudioInputStream audio1 = AudioSystem.getAudioInputStream(new File(audio1Path));
            AudioInputStream audio2 = AudioSystem.getAudioInputStream(new File(audio2Path));

            AudioFormat format = audio1.getFormat();

            byte[] audio1Bytes = audio1.readAllBytes();
            byte[] audio2Bytes = audio2.readAllBytes();

            // 計算間隔的字節數
            int intervalBytes = (int) (format.getFrameSize() * format.getFrameRate() * (interval / 1000.0));

            // 根據間隔調整音頻數據
            byte[] mergedBytes;
            if (intervalBytes > 0) {
                mergedBytes = new byte[audio1Bytes.length + intervalBytes + audio2Bytes.length];
                System.arraycopy(audio1Bytes, 0, mergedBytes, 0, audio1Bytes.length);
                System.arraycopy(new byte[intervalBytes], 0, mergedBytes, audio1Bytes.length, intervalBytes);
                System.arraycopy(audio2Bytes, 0, mergedBytes, audio1Bytes.length + intervalBytes, audio2Bytes.length);
            } else {
                int overlap = -intervalBytes;
                mergedBytes = new byte[audio1Bytes.length + audio2Bytes.length - overlap];
                System.arraycopy(audio1Bytes, 0, mergedBytes, 0, audio1Bytes.length);

                // 混合重疊部分
                for (int i = 0; i < overlap; i++) {
                    int sample1 = audio1Bytes[audio1Bytes.length - overlap + i];
                    int sample2 = audio2Bytes[i];
                    int mixedSample = sample1 + sample2;

                    // 限制範圍
                    if (mixedSample > Byte.MAX_VALUE) {
                        mixedSample = Byte.MAX_VALUE;
                    } else if (mixedSample < Byte.MIN_VALUE) {
                        mixedSample = Byte.MIN_VALUE;
                    }
                    mergedBytes[audio1Bytes.length - overlap + i] = (byte) mixedSample;
                }

                System.arraycopy(audio2Bytes, overlap, mergedBytes, audio1Bytes.length, audio2Bytes.length - overlap);
            }

            // 創建合併後的音頻流
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(mergedBytes);
            AudioInputStream mergedAudio = new AudioInputStream(byteArrayInputStream, format, mergedBytes.length / format.getFrameSize());

            // 寫入合併的音頻文件
            AudioSystem.write(mergedAudio, AudioFileFormat.Type.WAVE, new File(outputPath));
            System.out.println("合併完成，輸出文件路徑: " + outputPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
