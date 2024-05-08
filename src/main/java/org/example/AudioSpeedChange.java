package org.example;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.io.jvm.WaveformWriter;
import be.tarsos.dsp.resample.RateTransposer;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

public class AudioSpeedChange {
    public static void main(String[] args) {
        try {
            File audioFile = new File("C:\\Users\\user\\IdeaProjects\\musicspeed\\src\\main\\java\\org\\example\\Test.wav"); //inputfilepath
            AudioInputStream stream = AudioSystem.getAudioInputStream(audioFile);

            AudioDispatcher dispatcher = AudioDispatcherFactory.fromPipe(audioFile.getPath(), 44100, 512, 0);
            float speedChangeFactor = 0.5f; // 1/0.5 times the normal speed
            RateTransposer rateTransposer = new RateTransposer(speedChangeFactor);
            dispatcher.addAudioProcessor(rateTransposer);

            String outputFile = "C:\\Users\\user\\IdeaProjects\\musicspeed\\src\\main\\java\\org\\example\\output.wav"; //outputfile path
            WaveformWriter writer = new WaveformWriter(dispatcher.getFormat(), outputFile);
            dispatcher.addAudioProcessor(writer);

            dispatcher.run();
        } catch (UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
        }
    }
}
