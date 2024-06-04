package org.example;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import org.json.JSONObject;

public class AudioMerger {

    public static void main(String[] args) {
        String inTrackMusicConfigPath = "path/to/inTrackMusicConfig.json";
        String emojiAndMusicConfigPath = "path/to/emojiAndMusicConfig.json";
        String outputPath = "src/main/resources/soundEffect/final_output.wav";

        try {
            mergeAudioFilesFromConfig(inTrackMusicConfigPath, emojiAndMusicConfigPath, outputPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void mergeAudioFilesFromConfig(String inTrackMusicConfigPath, String emojiAndMusicConfigPath, String outputPath) throws IOException {
        try {
            String inTrackMusicConfigContent = new String(Files.readAllBytes(Paths.get(inTrackMusicConfigPath)));
            String emojiAndMusicConfigContent = new String(Files.readAllBytes(Paths.get(emojiAndMusicConfigPath)));

            JSONObject inTrackMusicConfig = new JSONObject(inTrackMusicConfigContent);
            JSONObject emojiAndMusicConfig = new JSONObject(emojiAndMusicConfigContent);

            mergeAudioFiles(inTrackMusicConfig, emojiAndMusicConfig, outputPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void mergeAudioFiles(JSONObject inTrackMusicConfig, JSONObject emojiAndMusicConfig, String outputPath) {
        try {
            List<AudioClip> audioClips = new ArrayList<>();

            for (String key : inTrackMusicConfig.keySet()) {
                JSONObject track = inTrackMusicConfig.getJSONObject(key);
                String origin = track.getString("origin");
                double startTime = track.getDouble("startTime");

                JSONObject originConfig = emojiAndMusicConfig.getJSONObject(origin);
                String path = originConfig.getString("path");

                AudioInputStream audioStream = AudioSystem.getAudioInputStream(new File(path));
                byte[] audioBytes = audioStream.readAllBytes();
                audioStream.close();

                AudioFormat format = audioStream.getFormat();
                audioClips.add(new AudioClip(audioBytes, startTime, format));
            }

            audioClips.sort(Comparator.comparingDouble(AudioClip::getStartTime));

            byte[] mergedBytes = mergeAudioClips(audioClips);
            AudioFormat format = audioClips.get(0).getFormat();
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(mergedBytes);
            AudioInputStream mergedAudio = new AudioInputStream(byteArrayInputStream, format, mergedBytes.length / format.getFrameSize());

            AudioSystem.write(mergedAudio, AudioFileFormat.Type.WAVE, new File(outputPath));
            System.out.println("合併完成，輸出文件路徑: " + outputPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static byte[] mergeAudioClips(List<AudioClip> audioClips) {
        try {
            int totalFrames = 0;
            AudioFormat format = audioClips.get(0).getFormat();
            int frameSize = format.getFrameSize();
            float frameRate = format.getFrameRate();
            int sampleSizeInBits = format.getSampleSizeInBits();
            boolean isBigEndian = format.isBigEndian();
            boolean isSigned = format.getEncoding() == AudioFormat.Encoding.PCM_SIGNED;

            // Calculate total frames needed
            for (AudioClip clip : audioClips) {
                totalFrames = Math.max(totalFrames, (int) (clip.getStartTime() * frameRate) + clip.getFrameLength());
            }

            // Prepare the buffer for merged audio
            byte[] mergedBytes = new byte[totalFrames * frameSize];

            // Merge audio clips
            for (AudioClip clip : audioClips) {
                int startFrame = (int) (clip.getStartTime() * frameRate);
                byte[] audioBytes = clip.getAudioBytes();
                int clipLength = audioBytes.length;

                for (int i = 0; i < clipLength; i += frameSize) {
                    int byteIndex = startFrame * frameSize + i;
                    if (byteIndex + frameSize <= mergedBytes.length) {
                        for (int j = 0; j < frameSize; j += sampleSizeInBits / 8) {
                            int sample = getSample(audioBytes, i + j, sampleSizeInBits, isSigned, isBigEndian);
                            int mergedSample = getSample(mergedBytes, byteIndex + j, sampleSizeInBits, isSigned, isBigEndian);
                            int mixedSample = mergedSample + sample;

                            // Clip the sample to the allowable range
                            mixedSample = Math.max(Math.min(mixedSample, getMaxSampleValue(sampleSizeInBits, isSigned)), getMinSampleValue(sampleSizeInBits, isSigned));

                            setSample(mergedBytes, byteIndex + j, mixedSample, sampleSizeInBits, isSigned, isBigEndian);
                        }
                    }
                }
            }

            return mergedBytes;
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    private static int getSample(byte[] buffer, int position, int sampleSizeInBits, boolean isSigned, boolean isBigEndian) {
        int sample = 0;
        if (sampleSizeInBits == 16 && position + 1 < buffer.length) {
            if (isBigEndian) {
                sample = buffer[position] << 8 | (buffer[position + 1] & 0xFF);
            } else  {
                sample = buffer[position] & 0xFF | buffer[position + 1] << 8;
            }
        } else if (sampleSizeInBits == 8) {
            sample = buffer[position];
        }

        if (isSigned && sampleSizeInBits == 16) {
            if (sample > 32767) {
                sample -= 65536;
            }
        }

        return sample;
    }

    private static void setSample(byte[] buffer, int position, int sample, int sampleSizeInBits, boolean isSigned, boolean isBigEndian) {
        if (sampleSizeInBits == 16 && position + 1 < buffer.length) {
            if (isBigEndian ) {
                buffer[position] = (byte) (sample >> 8);
                buffer[position + 1] = (byte) sample;
            } else {
                buffer[position] = (byte) sample;
                buffer[position + 1] = (byte) (sample >> 8);
            }
        } else if (sampleSizeInBits == 8) {
            buffer[position] = (byte) sample;
        }
    }

    private static int getMaxSampleValue(int sampleSizeInBits, boolean isSigned) {
        if (isSigned) {
            return (1 << (sampleSizeInBits - 1)) - 1;
        } else {
            return (1 << sampleSizeInBits) - 1;
        }
    }

    private static int getMinSampleValue(int sampleSizeInBits, boolean isSigned) {
        if (isSigned) {
            return -(1 << (sampleSizeInBits - 1));
        } else {
            return 0;
        }
    }

    static class AudioClip {
        private byte[] audioBytes;
        private double startTime;
        private AudioFormat format;

        public AudioClip(byte[] audioBytes, double startTime, AudioFormat format) {
            this.audioBytes = audioBytes;
            this.startTime = startTime;
            this.format = format;
        }

        public byte[] getAudioBytes() {
            return audioBytes;
        }

        public double getStartTime() {
            return startTime;
        }

        public AudioFormat getFormat() {
            return format;
        }

        public int getFrameLength() {
            return audioBytes.length / format.getFrameSize();
        }
    }
}
