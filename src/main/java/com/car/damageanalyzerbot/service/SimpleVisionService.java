package com.car.damageanalyzerbot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import org.json.JSONObject;

@Slf4j
@Service
public class SimpleVisionService {

    @Value("${yandex.cloud.iam.token}")
    private String iamToken;

    @Value("${yandex.cloud.folder.id}")
    private String folderId;

    private static final String OCR_URL = "https://ocr.api.cloud.yandex.net/ocr/v1/recognizeText";

    public String analyzeImage(byte[] imageData) {
        try {
            log.info("Starting Yandex Vision OCR analysis for image size: {} bytes", imageData.length);

            // Кодируем изображение в Base64
            String base64Image = Base64.getEncoder().encodeToString(imageData);

            // Формируем JSON запрос
            JSONObject requestBody = new JSONObject();
            requestBody.put("mimeType", "JPEG");
            requestBody.put("languageCodes", new String[]{"*"});
            requestBody.put("model", "page");
            requestBody.put("content", base64Image);

            // Создаем HTTP запрос
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OCR_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + iamToken)
                    .header("x-folder-id", folderId)
                    .header("x-data-logging-enabled", "true")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            // Отправляем запрос
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("Yandex Vision response status: {}", response.statusCode());

            if (response.statusCode() == 200) {
                String result = extractTextFromResponse(response.body());
                log.info("Successfully recognized text: {} characters", result.length());
                return formatResult(result);
            } else {
                log.error("Yandex Vision API error: {}", response.body());
                return "❌ Ошибка при анализе изображения. Код: " + response.statusCode();
            }

        } catch (Exception e) {
            log.error("Error in Yandex Vision analysis", e);
            return "❌ Ошибка: " + e.getMessage();
        }
    }

    private String extractTextFromResponse(String jsonResponse) {
        try {
            JSONObject response = new JSONObject(jsonResponse);
            JSONObject result = response.getJSONObject("result");
            JSONObject textAnnotation = result.getJSONObject("textAnnotation");

            return textAnnotation.getString("fullText");

        } catch (Exception e) {
            log.error("Error parsing Yandex Vision response", e);
            return "Не удалось распознать текст";
        }
    }

    private String formatResult(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "🔍 Текст на изображении не обнаружен";
        }

        // Форматируем результат для Telegram
        return "✅ **Распознанный текст:**\n\n" +
                "```\n" +
                text.trim() +
                "\n```";
    }
}