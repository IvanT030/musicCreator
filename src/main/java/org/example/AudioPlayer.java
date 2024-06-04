package org.example;
import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class AudioPlayer {
    public static void main(String[] args) {
        // Specify the path to the audio file
        String audioFilePath = "src\\main\\java\\org\\example\\Test.wav";

        // Create a File object for the audio file
        File audioFile = new File(audioFilePath);

        try {
            // Create an AudioInputStream from the audio file
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);

            // Get an AudioFormat object for the audio stream
            AudioFormat format = audioStream.getFormat();

            // Get a DataLine.Info object for the audio format
            DataLine.Info info = new DataLine.Info(Clip.class, format);

            // Obtain a Clip object from the AudioSystem
            Clip audioClip = (Clip) AudioSystem.getLine(info);

            // Open the audio clip with the audio stream
            audioClip.open(audioStream);

            // Add a listener to close the clip once playback is complete
            audioClip.addLineListener(new LineListener() {
                @Override
                public void update(LineEvent event) {
                    if (event.getType() == LineEvent.Type.STOP) {
                        audioClip.close();
                    }
                }
            });

            // Start playing the audio clip
            audioClip.start();

            System.out.println("Playback started.");

            // Keep the program running until the audio clip has finished playing
            while (!audioClip.isRunning()) {
                Thread.sleep(10);
            }
            while (audioClip.isRunning()) {
                Thread.sleep(10);
            }

            // Close the audio stream
            audioStream.close();

            System.out.println("Playback completed.");
        } catch (UnsupportedAudioFileException e) {
            System.err.println("The specified audio file is not supported.");
            e.printStackTrace();
        } catch (LineUnavailableException e) {
            System.err.println("Audio line for playing back is unavailable.");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Error playing the audio file.");
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.err.println("Playback interrupted.");
            e.printStackTrace();
        }
    }
}
