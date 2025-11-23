package com.car.damageanalyzerbot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

@Service
public class SimpleVisionService {

    @Value("${yandex.cloud.iam.token}")
    private String iamToken;

    @Value("${yandex.cloud.folder.id}")
    private String folderId;

    private static final String OCR_URL = "https://ocr.api.cloud.yandex.net/ocr/v1/recognizeText";



    public String analyzeImage(byte[] imageData) {
        try {
            String imageBase64 = Base64.getEncoder().encodeToString(imageData);

            String requestBody = String.format(
                    "{\"mimeType\":\"JPEG\",\"languageCodes\":[\"*\"],\"model\":\"page\",\"content\":\"%s\"}",
                    imageBase64
            );

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OCR_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + iamToken)
                    .header("x-folder-id", folderId)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return extractTextFromResponse(response.body());
            } else {
                return "Ошибка распознавания";
            }
        } catch (Exception e) {
            return "Ошибка обработки";
        }
    }

    private String extractTextFromResponse(String jsonResponse) {
        try {
            if (jsonResponse.contains("\"fullText\"")) {
                int start = jsonResponse.indexOf("\"fullText\":\"") + 12;
                int end = jsonResponse.indexOf("\"", start);
                return jsonResponse.substring(start, end).replace("\\n", "\n");
            }
            return "Текст не найден";
        } catch (Exception e) {
            return "Ошибка парсинга";
        }
    }
}