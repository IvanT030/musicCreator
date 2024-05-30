package org.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class EmojiMusicMap {
    private static final String configPath = "src/main/resources/emojiAndMusicConfig.json";
    private static Map<String, EmojiMusicEntry> emojiMusicMap = new HashMap<>();

    public EmojiMusicMap() {
        System.out.println("Creating EmojiMusicMap");
        loadEmojiMusicMap();
        if (emojiMusicMap.isEmpty()) {
            addEmojiMusicEntry("knockWoodDoor", "🚪", "/soundEffect/knockWoodDoor.mp3");
            addEmojiMusicEntry("doorChime", "🔔", "/soundEffect/doorChime.mp3");
            addEmojiMusicEntry("turnKey", "🔑", "/soundEffect/turnKey.mp3");
            saveEmojiMusicMap();
        }
    }

    public static Map<String, EmojiMusicEntry> getEmojiMusicMap() {
        return emojiMusicMap;
    }

    public static void saveEmojiMusicMap() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(configPath)) {
            gson.toJson(emojiMusicMap, writer);
        } catch (IOException e) {
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

    public static void addEmojiMusicEntry(String baseKey, String emoji, String path) {
        String key = baseKey;
        int count = 1;

        while (emojiMusicMap.containsKey(key) && !emojiMusicMap.get(key).getPath().equals(path)) {
            key = baseKey + count;
            count++;
        }

        emojiMusicMap.put(key, new EmojiMusicEntry(emoji, path));
    }

    public static class EmojiMusicEntry {
        private String emoji;
        private String path;

        public EmojiMusicEntry(String emoji, String path) {
            this.emoji = emoji;
            this.path = path;
        }

        public String getEmoji() {
            return emoji;
        }

        public void setEmoji(String emoji) {
            this.emoji = emoji;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }
}