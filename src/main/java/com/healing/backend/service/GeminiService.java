package com.healing.backend.service;

import com.google.gson.*;
import com.healing.backend.model.*;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Sync spec: Backend wajib memproses message dari user
 * menggunakan API Gemini dan mengembalikan jawabannya dalam ChatResponse.
 * API key AMAN di server — tidak pernah dikirim ke Android.
 */
@Service
public class GeminiService {

    @Value("${healing.gemini.api-key}")
    private String apiKey;

    @Value("${healing.gemini.model}")
    private String model;

    private final OkHttpClient httpClient;
    private final Gson gson = new Gson();
    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    public GeminiService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public String buildSystemPrompt(User user, int caloriesConsumed, int caloriesBurned,
                                    int xpThisWeek, List<MealLog> todayMeals) {
        StringBuilder sb = new StringBuilder();
        sb.append("Kamu adalah Hana, AI health coach personal dari aplikasi Healing. ");
        sb.append("Berbicara dalam Bahasa Indonesia yang hangat seperti teman dekat. ");
        sb.append("Gunakan emoji secukupnya. Jawab singkat, max 3-4 kalimat. ");
        sb.append("Jangan pernah judgemental soal makanan favorit atau kemalasan user.\n\n");

        if (user != null) {
            int target    = user.getRecommendedCalories();
            int remaining = Math.max(0, target - caloriesConsumed);

            sb.append("=== DATA USER HARI INI ===\n");
            sb.append("Nama: ").append(user.getName() != null ? user.getName() : "Kamu").append("\n");
            sb.append("Target kalori: ").append(target).append(" kcal\n");
            sb.append("Dikonsumsi: ").append(caloriesConsumed).append(" kcal\n");
            sb.append("Sisa: ").append(remaining).append(" kcal\n");
            sb.append("Dibakar: ").append(caloriesBurned).append(" kcal\n");
            sb.append("XP minggu ini: ").append(xpThisWeek).append("\n");
            sb.append("Level: ").append(user.getCurrentLevel())
              .append(" (").append(user.getLevelTitle()).append(")\n");

            if (user.getFavoriteFoods() != null && !user.getFavoriteFoods().isBlank())
                sb.append("Makanan favorit: ").append(user.getFavoriteFoods()).append("\n");
            if (user.getHobbies() != null && !user.getHobbies().isBlank())
                sb.append("Hobi: ").append(user.getHobbies()).append("\n");
            if (user.getGoal() != null)
                sb.append("Goal: ").append(user.getGoal()).append("\n");

            if (todayMeals != null) {
                long cheatCount = todayMeals.stream()
                        .filter(m -> Boolean.TRUE.equals(m.getIsCheatTreat())).count();
                if (cheatCount > 0)
                    sb.append("Cheat & Treat hari ini: ").append(cheatCount).append(" item\n");
            }
            sb.append("========================\n");
        }
        return sb.toString();
    }

    public String chat(String systemPrompt, List<ChatMessage> history, String userMessage)
            throws IOException {

        JsonArray contents = new JsonArray();

        // History (max 20 pesan terakhir)
        int start = Math.max(0, history.size() - 20);
        for (int i = start; i < history.size(); i++) {
            ChatMessage msg = history.get(i);
            // Gemini pakai "model" bukan "assistant"
            String geminiRole = msg.isUser() ? "user" : "model";
            contents.add(buildContent(geminiRole, msg.getContent()));
        }
        contents.add(buildContent("user", userMessage));

        // System instruction
        JsonObject systemInstruction = new JsonObject();
        JsonArray sysParts = new JsonArray();
        JsonObject sysText = new JsonObject();
        sysText.addProperty("text", systemPrompt);
        sysParts.add(sysText);
        systemInstruction.add("parts", sysParts);

        // Generation config
        JsonObject genConfig = new JsonObject();
        genConfig.addProperty("maxOutputTokens", 400);
        genConfig.addProperty("temperature", 0.75);

        JsonObject body = new JsonObject();
        body.add("system_instruction", systemInstruction);
        body.add("contents", contents);
        body.add("generationConfig", genConfig);

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                   + model + ":generateContent?key=" + apiKey;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(gson.toJson(body), JSON_TYPE))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("Gemini API error " + response.code() + ": " + responseBody);
            }

            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray candidates = json.getAsJsonArray("candidates");
            if (candidates == null || candidates.size() == 0)
                throw new IOException("Tidak ada respons dari Gemini");

            JsonArray parts = candidates.get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts");

            StringBuilder result = new StringBuilder();
            for (JsonElement part : parts) {
                if (part.getAsJsonObject().has("text"))
                    result.append(part.getAsJsonObject().get("text").getAsString());
            }
            return result.toString().trim();
        }
    }

    private JsonObject buildContent(String role, String text) {
        JsonObject content = new JsonObject();
        content.addProperty("role", role);
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", text);
        parts.add(part);
        content.add("parts", parts);
        return content;
    }
}
