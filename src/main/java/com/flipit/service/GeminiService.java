package com.flipit.service;

import com.flipit.models.Card;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class GeminiService {
    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    private static final int MAX_RETRIES = 5;
    private static final long INITIAL_WAIT_MS = 10_000;
    private static final double BACKOFF_MULTIPLIER = 2.0;

    private String apiKey;
    private volatile String rateLimitStatus = null;

    public GeminiService() {
        try (InputStream in = GeminiService.class.getResourceAsStream("/config.properties")) {
            if (in == null) {
                System.err.println("WARNING: config.properties not found on classpath!");
                this.apiKey = null;
                return;
            }

            Properties props = new Properties();
            props.load(in);
            this.apiKey = props.getProperty("gemini.api.key");

            if (this.apiKey == null || this.apiKey.trim().isEmpty()) {
                System.err.println("WARNING: gemini.api.key is missing from config.properties!");
            }
        } catch (IOException e) {
            System.err.println("Failed to load config.properties for GeminiService: " + e.getMessage());
            this.apiKey = null;
        }
    }

    public List<Card> generateCards(String studyText, int cardCount) throws IOException, GeminiException {
        if (this.apiKey == null || this.apiKey.trim().isEmpty()) {
            throw new GeminiException("API Key is missing. Please add gemini.api.key to your config.properties file.");
        }

        String prompt = buildPrompt(studyText, cardCount);
        String responseBody = callApiWithRetry(prompt);
        return parseResponse(responseBody);
    }

    private String buildPrompt(String text, int count) {
        return "You are an expert educator creating multiple-choice flashcard study questions.\n\n" +
                "Generate exactly " + count + " multiple-choice flashcard questions based on the " +
                "study material below.\n\n" +
                "STRICT RULES:\n" +
                "1. Return ONLY a valid JSON array. No markdown, no code fences, no explanation.\n" +
                "2. Each element must have exactly these keys:\n" +
                "   \"question\", \"answer_a\", \"answer_b\", \"answer_c\", \"answer_d\", \"correct_answer\"\n" +
                "3. \"correct_answer\" must be exactly one of: \"A\", \"B\", \"C\", or \"D\"\n" +
                "4. Questions must test understanding, not just recall.\n" +
                "5. Wrong answers (distractors) must be plausible but clearly incorrect.\n" +
                "6. Keep each question under 200 characters. Keep each answer under 120 characters.\n\n" +
                "STUDY MATERIAL:\n---\n" + text + "\n---\n\nReturn the JSON array now:";
    }

    private String callApiWithRetry(String prompt) throws IOException, GeminiException {
        long waitMs = INITIAL_WAIT_MS;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(ENDPOINT + "?key=" + apiKey);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);
                conn.setConnectTimeout(30_000);
                conn.setReadTimeout(120_000);

                JSONObject part = new JSONObject().put("text", prompt);
                JSONObject content = new JSONObject().put("parts", new JSONArray().put(part));
                JSONObject body = new JSONObject().put("contents", new JSONArray().put(content));

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }

                int status = conn.getResponseCode();

                if (status == 200) {
                    return readStream(conn.getInputStream());
                }

                String errorBody = readStream(conn.getErrorStream());

                if (status == 429) {
                    String retryAfter = conn.getHeaderField("Retry-After");
                    if (retryAfter != null) {
                        try {
                            waitMs = Long.parseLong(retryAfter.trim()) * 1000L;
                        } catch (NumberFormatException ignored) {
                        }
                    }

                    if (attempt == MAX_RETRIES) {
                        throw new GeminiException(
                                "Rate limit reached after " + MAX_RETRIES + " attempts.\n\n" +
                                        "Please wait a minute and try again.");
                    }

                    final long displayWait = waitMs;
                    final int displayAttempt = attempt;
                    rateLimitStatus = "Rate limited — retrying in " +
                            (displayWait / 1000) + "s (attempt " + displayAttempt + "/" + MAX_RETRIES + ")";

                    Thread.sleep(waitMs);
                    waitMs = (long) (waitMs * BACKOFF_MULTIPLIER);
                    continue;
                }

                throw new GeminiException("HTTP " + status + ": " + extractErrorMessage(errorBody));

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new GeminiException("Generation was interrupted.");
            } finally {
                if (conn != null) conn.disconnect();
            }
        }
        throw new GeminiException("Failed after " + MAX_RETRIES + " attempts.");
    }

    public String getRateLimitStatus() {
        return rateLimitStatus;
    }

    public void clearRateLimitStatus() {
        rateLimitStatus = null;
    }

    private String cleanText(String text) {
        if (text == null) return "";
        return text.replaceAll("[\\x{10000}-\\x{10FFFF}]", "")
                .replaceAll("[\\x{2600}-\\x{27BF}]", "")
                .trim();
    }

    private List<Card> parseResponse(String responseBody) throws GeminiException {
        try {
            JSONObject root = new JSONObject(responseBody);
            JSONArray candidates = root.getJSONArray("candidates");
            JSONObject candidate = candidates.getJSONObject(0);
            JSONObject content = candidate.getJSONObject("content");
            JSONArray parts = content.getJSONArray("parts");
            String text = parts.getJSONObject(0).getString("text").trim();

            text = stripCodeFences(text);

            JSONArray cards = new JSONArray(text);
            List<Card> result = new ArrayList<>();

            for (int i = 0; i < cards.length(); i++) {
                JSONObject c = cards.getJSONObject(i);
                String correct = c.getString("correct_answer").trim().toUpperCase();
                if (!correct.equals("A") && !correct.equals("B") &&
                        !correct.equals("C") && !correct.equals("D")) correct = "A";

                result.add(new Card(0,
                        0, // Dummy deck_id, handled by the Dialog
                        cleanText(c.getString("question")),
                        cleanText(c.getString("answer_a")),
                        cleanText(c.getString("answer_b")),
                        cleanText(c.getString("answer_c")),
                        cleanText(c.getString("answer_d")),
                        correct));
            }

            if (result.isEmpty()) throw new GeminiException("Gemini returned an empty card list.");
            return result;

        } catch (GeminiException e) {
            throw e;
        } catch (Exception e) {
            throw new GeminiException("Could not parse Gemini response: " + e.getMessage());
        }
    }

    private String readStream(InputStream stream) throws IOException {
        if (stream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString();
    }

    private String extractErrorMessage(String body) {
        try {
            JSONObject obj = new JSONObject(body);
            if (obj.has("error")) return obj.getJSONObject("error").optString("message", body);
        } catch (Exception ignored) {
        }
        return body.length() > 300 ? body.substring(0, 300) : body;
    }

    private String stripCodeFences(String text) {
        if (text.startsWith("```")) {
            int first = text.indexOf('\n');
            if (first >= 0) text = text.substring(first + 1);
            if (text.endsWith("```")) text = text.substring(0, text.lastIndexOf("```"));
        }
        return text.trim();
    }

    public static class GeminiException extends Exception {
        public GeminiException(String msg) {
            super(msg);
        }
    }
}