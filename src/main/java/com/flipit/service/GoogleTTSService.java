package com.flipit.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class GoogleTTSService {
    public File generateAudioFile(String text) throws Exception {
        File tempFile = File.createTempFile("flipit_tts_", ".mp3");
        tempFile.deleteOnExit();

        List<String> chunks = splitText(text, 150);

        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            for (String chunk : chunks) {
                if (chunk.trim().isEmpty()) continue;

                String encoded = URLEncoder.encode(chunk, "UTF-8");
                String urlStr = "https://translate.google.com/translate_tts?ie=UTF-8"
                        + "&q=" + encoded + "&tl=en" + "&client=gtx";

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestProperty("User-Agent", "Mozilla/5.0");

                if (conn.getResponseCode() == 200) {
                    try (InputStream in = conn.getInputStream()) {
                        byte[] buffer = new byte[4096];
                        int n;
                        while ((n = in.read(buffer)) != -1) {
                            out.write(buffer, 0, n);
                        }
                    }
                } else {
                    System.err.println("Google TTS API returned HTTP " + conn.getResponseCode() + " for chunk: " + chunk);
                }
            }
        }
        return tempFile;
    }

    // split into chunks
    private List<String> splitText(String text, int maxLength) {
        List<String> chunks = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentChunk = new StringBuilder();

        for (String word : words) {
            if (currentChunk.length() + word.length() + 1 > maxLength) {
                chunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder();
            }
            currentChunk.append(word).append(" ");
        }
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        return chunks;
    }
}