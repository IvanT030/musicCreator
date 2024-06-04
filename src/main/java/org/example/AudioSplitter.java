package org.example;
import javax.sound.sampled.*;
import java.io.*;

public class AudioSplitter {

    public static void splitAudioFile(File inputFile, File outputFile1, File outputFile2, int splitPointInSeconds) throws IOException, UnsupportedAudioFileException {
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(inputFile);
        AudioFormat format = audioInputStream.getFormat();
        long frames = audioInputStream.getFrameLength();
        double durationInSeconds = (frames + 0.0) / format.getFrameRate();

        int splitFrame = (int) (splitPointInSeconds * format.getFrameRate());

        if (splitPointInSeconds > durationInSeconds) {
            throw new IllegalArgumentException("Split point is beyond the duration of the audio file.");
        }

        AudioInputStream part1 = new AudioInputStream(audioInputStream, format, splitFrame);
        AudioInputStream part2 = new AudioInputStream(audioInputStream, format, frames - splitFrame);

        AudioSystem.write(part1, AudioFileFormat.Type.WAVE, outputFile1);
        AudioSystem.write(part2, AudioFileFormat.Type.WAVE, outputFile2);
    }

    public static void main(String[] args) {
        File inputFile = new File("src\\main\\java\\org\\example\\Test.wav");
        File outputFile1 = new File("src\\main\\java\\org\\example\\output6-1.wav");
        File outputFile2 = new File("src\\main\\java\\org\\example\\output6-2.wav");
        int splitPointInSeconds = 30; // 分割點，單位：秒

        try {
            splitAudioFile(inputFile, outputFile1, outputFile2, splitPointInSeconds);
            System.out.println("Audio file has been split successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
