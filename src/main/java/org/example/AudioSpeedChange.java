package org.example;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.WaveformWriter;
import be.tarsos.dsp.resample.RateTransposer;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

public class AudioSpeedChange extends AudioController {

    private static String inputPath;
    private static String outputPath;
    private static double speedChangeFactor = 1.0f;
    public AudioSpeedChange(String inputPath, String outputPath) {
        this.inputPath = inputPath;
        this.outputPath = outputPath;
    }

    public static void processAudio() {
        try {
            speedChangeFactor = 1 / speedChangeFactor;
            File audioFile = new File(inputPath); //inputfilepath
            AudioInputStream stream = AudioSystem.getAudioInputStream(audioFile);

            AudioDispatcher dispatcher = AudioDispatcherFactory.fromPipe(audioFile.getPath(), 44100, 512, 0);
            RateTransposer rateTransposer = new RateTransposer( speedChangeFactor);
            dispatcher.addAudioProcessor(rateTransposer);

            WaveformWriter writer = new WaveformWriter(dispatcher.getFormat(), outputPath);
            dispatcher.addAudioProcessor(writer);

            dispatcher.run();

        } catch (UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
        }
    }

    public static void setSpeedFactor(double speedChangeValue){
        speedChangeFactor = speedChangeValue;
    }

}
