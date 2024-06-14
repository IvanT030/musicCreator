package org.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class EmojiMusicMap {
    private static final String configPath = "src/main/resources/musicConfig/emojiAndMusicConfig.json";
    private static Map<String, EmojiMusicEntry> emojiMusicMap = new HashMap<>();

    public EmojiMusicMap() {
        //System.out.println("Creating EmojiMusicMap");
        loadEmojiMusicMap();
        if (emojiMusicMap.isEmpty()) {
            initializeEmojiMusicMap();
            saveEmojiMusicMap();
        }
    }


    private void initializeEmojiMusicMap() {
        addEmojiMusicEntry("🎵", "ChimataRingtoneImmemorialMarketeers", "src/main/resources/soundEffect/ChimataRingtoneImmemorialMarketeers.wav");
        addEmojiMusicEntry("🔊", "DStyleHardcoreRingtone", "src/main/resources/soundEffect/DStyleHardcoreRingtone.wav");
        addEmojiMusicEntry("🎮", "NintendoGameCubeStartup", "src/main/resources/soundEffect/NintendoGameCubeStartup.wav");
        addEmojiMusicEntry("📞", "NokiaArabicRingtone", "src/main/resources/soundEffect/NokiaArabicRingtone.wav");
        addEmojiMusicEntry("📞", "iPhone6PlusOriginalRingtone", "src/main/resources/soundEffect/iPhone6PlusOriginalRingtone.wav");
        addEmojiMusicEntry("\uD83C\uDFB6", "shortintromusic", "src/main/resources/soundEffect/shortintromusic.wav");
        addEmojiMusicEntry("\uD83C ", "MegalovaniaRingtoneRemix", "src/main/resources/soundEffect/MegalovaniaRingtoneRemix.wav");
    }

    public static Map<String, EmojiMusicEntry> getEmojiMusicMap() {
        return emojiMusicMap;
    }

    public static void saveEmojiMusicMap() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(configPath)) {
            gson.toJson(emojiMusicMap, writer);
        } catch (IOException e) {
            System.out.println("Failed to save emoji music map: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void loadEmojiMusicMap() {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(configPath)) {
            Type mapType = new TypeToken<Map<String, EmojiMusicEntry>>(){}.getType();

            // 读取文件内容到一个字符串
            BufferedReader br = new BufferedReader(reader);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            // 检查是否为空文件
            if (sb.length() == 0) {
                emojiMusicMap = new HashMap<>();
            } else {
                emojiMusicMap = gson.fromJson(sb.toString(), mapType);
            }

        } catch (IOException e) {
            System.out.println("Failed to load emoji music map: " + e.getMessage());
            emojiMusicMap = new HashMap<>();
        }
    }

    public static void addEmojiMusicEntry(String emoji, String baseKey, String path) {
        String key = baseKey;
        int count = 1;

        while (emojiMusicMap.containsKey(key) && !emojiMusicMap.get(key).getPath().equals(path)) {
            key = baseKey + count;
            count++;
        }

        emojiMusicMap.put(key, new EmojiMusicEntry(emoji, path));
    }

    public static double getWavFileDuration(String filePath) {
        File file = new File(filePath);
        try {
            System.out.println(filePath);
            AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(file);
            if (fileFormat.getType() != AudioFileFormat.Type.WAVE) {
                throw new UnsupportedAudioFileException("Not a WAV file");
            }

            AudioFormat format = fileFormat.getFormat();
            long frames = fileFormat.getFrameLength();
            double durationInSeconds = (frames + 0.0) / format.getFrameRate();
            System.out.println("Duration: " + durationInSeconds);
            return durationInSeconds;
        } catch (UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static class EmojiMusicEntry {
        private String emoji;
        private String path;
        private double musicLength;

        public EmojiMusicEntry(String emoji, String path) {
            this.emoji = emoji;
            this.path = path;
            this.musicLength = getWavFileDuration(path);
        }

        public String getEmoji() {
            return emoji;
        }

        public String getPath() {
            return path;
        }

        public double getMusicLength() {
            return musicLength;
        }
    }
}