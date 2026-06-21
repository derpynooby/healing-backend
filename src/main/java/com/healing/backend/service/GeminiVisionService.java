package com.healing.backend.service;

import com.google.gson.*;
import com.healing.backend.dto.FoodScanRequest;
import com.healing.backend.model.*;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Service untuk analisis foto makanan menggunakan Gemini Vision API.
 *
 * Workflow:
 * 1. Terima gambar base64 dari Android
 * 2. Kirim ke Gemini Vision dengan prompt analisis nutrisi
 * 3. Parse response JSON dari Gemini
 * 4. Return FoodScan entity dengan data nutrisi
 *
 * Model: gemini-2.5-flash (mendukung image input)
 */
@Service
public class GeminiVisionService {

    @Value("${healing.gemini.api-key}")
    private String apiKey;

    @Value("${healing.gemini.model}")
    private String model;

    private final OkHttpClient httpClient;
    private final Gson gson = new Gson();
    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    // Disclaimer wajib sesuai spec dokumen
    public static final String NUTRITION_DISCLAIMER =
        "⚠️ Estimasi nutrisi ini dihasilkan oleh AI berdasarkan analisis visual. " +
        "Nilai aktual dapat berbeda tergantung bahan, porsi, dan metode memasak. " +
        "Konsultasikan dengan ahli gizi untuk informasi nutrisi yang akurat.";

    public GeminiVisionService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Analisis foto makanan dan return data nutrisi.
     * Sesuai spec Prompt 1: deteksi nama, berat, kalori, makro.
     */
    public FoodScan analyzeFoodImage(User user, FoodScanRequest req) throws IOException {
        String prompt = buildVisionPrompt();

        // Build Gemini Vision request body
        JsonObject body = new JsonObject();

        // System instruction
        JsonObject sysInstruction = new JsonObject();
        JsonArray sysParts = new JsonArray();
        JsonObject sysText = new JsonObject();
        sysText.addProperty("text",
            "Kamu adalah ahli nutrisi AI. Analisis foto makanan dan berikan estimasi " +
            "nutrisi yang akurat. Selalu respond dalam format JSON yang valid saja, " +
            "tanpa teks lain di luar JSON.");
        sysParts.add(sysText);
        sysInstruction.add("parts", sysParts);
        body.add("system_instruction", sysInstruction);

        // Contents dengan image + text
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        content.addProperty("role", "user");

        JsonArray parts = new JsonArray();

        // Part 1: gambar
        JsonObject imagePart = new JsonObject();
        JsonObject inlineData = new JsonObject();
        inlineData.addProperty("mime_type",
            req.getMimeType() != null ? req.getMimeType() : "image/jpeg");
        inlineData.addProperty("data", req.getImageBase64());
        imagePart.add("inline_data", inlineData);
        parts.add(imagePart);

        // Part 2: text prompt
        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", prompt);
        parts.add(textPart);

        content.add("parts", parts);
        contents.add(content);
        body.add("contents", contents);

        // Generation config — pastikan output JSON
        JsonObject genConfig = new JsonObject();
        genConfig.addProperty("temperature", 0.2);  // rendah = lebih konsisten
        genConfig.addProperty("maxOutputTokens", 1024);
        genConfig.addProperty("responseMimeType", "application/json");
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
                throw new IOException("Gemini Vision API error " + response.code() +
                        ": " + responseBody);
            }
            return parseVisionResponse(user, responseBody);
        }
    }

    /**
     * Prompt analisis nutrisi sesuai spec Prompt 1.
     */
    private String buildVisionPrompt() {
        return """
            Analisis foto makanan ini dan berikan estimasi nutrisi dalam format JSON berikut:
            
            {
              "detectedFoodName": "Nama makanan lengkap yang terdeteksi",
              "confidenceScore": 0.0-1.0,
              "estimatedWeightGram": angka dalam gram,
              "estimatedPortion": "deskripsi porsi (misal: 1 piring sedang, 1/2 porsi)",
              "totalCalories": angka kalori (kkal),
              "carbohydrates": gram karbohidrat,
              "protein": gram protein,
              "fat": gram lemak,
              "fiber": gram serat,
              "components": [
                {
                  "name": "nama komponen makanan",
                  "calories": kalori komponen,
                  "weightGram": berat gram
                }
              ],
              "healthNotes": "catatan singkat tentang nilai gizi makanan ini"
            }
            
            Jika foto tidak jelas atau bukan foto makanan, gunakan:
            {
              "detectedFoodName": "Tidak terdeteksi",
              "confidenceScore": 0.0,
              "totalCalories": 0,
              "error": "Alasan tidak bisa dianalisis"
            }
            
            Berikan nilai yang realistis untuk makanan Indonesia jika relevan.
            HANYA output JSON, tidak ada teks lain.
            """;
    }

    /**
     * Parse response JSON dari Gemini Vision ke FoodScan entity.
     */
    private FoodScan parseVisionResponse(User user, String rawResponse) {
        try {
            JsonObject root = JsonParser.parseString(rawResponse).getAsJsonObject();
            JsonArray candidates = root.getAsJsonArray("candidates");
            if (candidates == null || candidates.size() == 0) {
                return buildErrorScan(user, "Tidak ada respons dari AI Vision");
            }

            String jsonText = candidates.get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();

            // Bersihkan markdown code block jika ada
            jsonText = jsonText.replaceAll("```json", "").replaceAll("```", "").trim();

            JsonObject result = JsonParser.parseString(jsonText).getAsJsonObject();

            // Cek jika error
            if (result.has("error")) {
                return buildErrorScan(user, result.get("error").getAsString());
            }

            return FoodScan.builder()
                    .user(user)
                    .detectedFoodName(getStr(result, "detectedFoodName", "Tidak terdeteksi"))
                    .confidenceScore(getFloat(result, "confidenceScore", 0f))
                    .estimatedWeightGram(getInt(result, "estimatedWeightGram", 0))
                    .estimatedPortion(getStr(result, "estimatedPortion", "—"))
                    .totalCalories(getInt(result, "totalCalories", 0))
                    .carbohydrates(getFloat(result, "carbohydrates", 0f))
                    .protein(getFloat(result, "protein", 0f))
                    .fat(getFloat(result, "fat", 0f))
                    .fiber(getFloat(result, "fiber", 0f))
                    .loggedToMeal(false)
                    .build();

        } catch (Exception e) {
            return buildErrorScan(user, "Gagal parse respons AI: " + e.getMessage());
        }
    }

    private FoodScan buildErrorScan(User user, String reason) {
        return FoodScan.builder()
                .user(user)
                .detectedFoodName("Tidak terdeteksi")
                .confidenceScore(0f)
                .estimatedWeightGram(0)
                .totalCalories(0)
                .loggedToMeal(false)
                .build();
    }

    private String getStr(JsonObject obj, String key, String def) {
        return obj.has(key) && !obj.get(key).isJsonNull()
            ? obj.get(key).getAsString() : def;
    }

    private Float getFloat(JsonObject obj, String key, Float def) {
        try { return obj.has(key) ? obj.get(key).getAsFloat() : def; }
        catch (Exception e) { return def; }
    }

    private Integer getInt(JsonObject obj, String key, Integer def) {
        try { return obj.has(key) ? obj.get(key).getAsInt() : def; }
        catch (Exception e) { return def; }
    }
}
