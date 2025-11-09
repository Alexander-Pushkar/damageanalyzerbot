package com.car.damageanalyzerbot.service;

import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SimpleVisionService {

    public String analyzeImage(byte[] imageData) {
        try (ImageAnnotatorClient vision = ImageAnnotatorClient.create()) {

            // Подготавливаем изображение
            ByteString imgBytes = ByteString.copyFrom(imageData);
            Image image = Image.newBuilder().setContent(imgBytes).build();

            // Запрос на определение объектов
            Feature feature = Feature.newBuilder()
                    .setType(Feature.Type.LABEL_DETECTION)
                    .setMaxResults(10)
                    .build();

            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(feature)
                    .setImage(image)
                    .build();

            List<AnnotateImageRequest> requests = new ArrayList<>();
            requests.add(request);

            // Отправляем запрос к Google Vision API
            BatchAnnotateImagesResponse response = vision.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();

            // Обрабатываем ответ
            if (responses.isEmpty()) {
                return "Не удалось проанализировать изображение (Vision API)";
            }

            AnnotateImageResponse visionResponse = responses.get(0);

            if (visionResponse.hasError()) {
                return "Ошибка Vision API: " + visionResponse.getError().getMessage();
            }

            // Формируем описание из найденных объектов
            List<String> labels = visionResponse.getLabelAnnotationsList().stream()
                    .filter(label -> label.getScore() > 0.7) // Только уверенные >70%
                    .map(label -> String.format("%s (%.0f%%)",
                            label.getDescription(),
                            label.getScore() * 100))
                    .collect(Collectors.toList());

            if (labels.isEmpty()) {
                return "Не удалось определить объекты на фото";
            }

            return "На фото обнаружено:" + String.join("\n", labels);

        } catch (Exception e) {
            log.error("Vision API error", e);
            return "Ошибка при анализе фото: " + e.getMessage();
        }
    }
}