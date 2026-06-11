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
 * Service backend untuk memanggil Gemini API.
 * API key disimpan aman di server — tidak pernah dikirim ke Android.
 */
@Service
public class GeminiService {

    @Value("${healing.gemini.api-key}")
    private String apiKey;

    @Value("${healing.gemini.model}")
    private String model;

    private final OkHttpClient httpClient;
    private final Gson gson = new Gson();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public GeminiService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Bangun system prompt personal — dijalankan di server, aman.
     */
    public String buildSystemPrompt(User user, int caloriesConsumed, int caloriesBurned,
                                    int xpThisWeek, List<MealLog> todayMeals) {
        StringBuilder sb = new StringBuilder();
        sb.append("Kamu adalah Hana, AI health coach personal dari aplikasi Healing. ");
        sb.append("Kamu berbicara dalam Bahasa Indonesia yang hangat, seperti teman dekat. ");
        sb.append("Gunakan emoji secukupnya. Jangan terlalu formal. ");
        sb.append("Jawab singkat dan natural, max 3-4 kalimat. ");
        sb.append("Jangan pernah judgemental soal makanan favorit atau kemalasan user.\n\n");

        if (user != null) {
            int target = user.getRecommendedCalories();
            sb.append("=== DATA USER HARI INI ===\n");
            sb.append("Nama: ").append(user.getName()).append("\n");
            sb.append("Target kalori: ").append(target).append(" kcal\n");
            sb.append("Kalori dikonsumsi: ").append(caloriesConsumed).append(" kcal\n");
            sb.append("Sisa kalori: ").append(Math.max(0, target - caloriesConsumed)).append(" kcal\n");
            sb.append("Kalori dibakar: ").append(caloriesBurned).append(" kcal\n");
            sb.append("XP minggu ini: ").append(xpThisWeek).append("\n");
            sb.append("Level: ").append(user.getCurrentLevel())
              .append(" (").append(user.getLevelTitle()).append(")\n");
            if (user.getFavoriteFoods() != null && !user.getFavoriteFoods().isEmpty())
                sb.append("Makanan favorit: ").append(user.getFavoriteFoods()).append("\n");
            if (user.getHobbies() != null && !user.getHobbies().isEmpty())
                sb.append("Hobi: ").append(user.getHobbies()).append("\n");
            if (todayMeals != null) {
                long cheats = todayMeals.stream().filter(m -> Boolean.TRUE.equals(m.getIsCheatTreat())).count();
                if (cheats > 0) sb.append("Cheat & Treat hari ini: ").append(cheats).append(" item\n");
            }
            sb.append("Goal: ").append(user.getGoal()).append("\n");
            sb.append("========================\n");
        }
        return sb.toString();
    }

    /**
     * Kirim pesan ke Gemini dan return respons (synchronous — dipanggil dari @Async controller).
     */
    public String chat(String systemPrompt, List<ChatMessage> history, String userMessage)
            throws IOException {

        JsonArray contents = new JsonArray();

        // History (max 20 pesan terakhir)
        int start = Math.max(0, history.size() - 20);
        for (int i = start; i < history.size(); i++) {
            ChatMessage msg = history.get(i);
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

        // Request body
        JsonObject body = new JsonObject();
        body.add("system_instruction", systemInstruction);
        body.add("contents", contents);
        body.add("generationConfig", genConfig);

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                   + model + ":generateContent?key=" + apiKey;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(gson.toJson(body), JSON))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();
            if (!response.isSuccessful()) {
                throw new IOException("Gemini API error " + response.code() + ": " + responseBody);
            }
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray candidates = json.getAsJsonArray("candidates");
            if (candidates == null || candidates.size() == 0)
                throw new IOException("No candidates in Gemini response");

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
