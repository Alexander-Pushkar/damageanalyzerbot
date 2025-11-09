package com.car.damageanalyzerbot.bot;

import com.car.damageanalyzerbot.service.SimpleVisionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.InputStream;
import java.net.URL;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
public class SimpleCarBot extends TelegramLongPollingBot {

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.name}")
    private String botName;

    private final SimpleVisionService visionService;

    public SimpleCarBot(SimpleVisionService visionService) {
        this.visionService = visionService;
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();

            if (message.hasText()) {
                handleTextMessage(message);
            } else if (message.hasPhoto()) {
                handlePhotoMessage(message);
            }
        }
    }

    private void handleTextMessage(Message message) {
        Long chatId = message.getChatId();
        sendMessage(chatId, "Отправь фото для анализа!");
    }

    private void handlePhotoMessage(Message message) {
        Long chatId = message.getChatId();

        try {
            sendMessage(chatId, "Анализирую фото через Google Vision API...");

            // Получаем самое качественное фото
            PhotoSize photo = message.getPhoto().stream()
                    .max(Comparator.comparing(PhotoSize::getFileSize))
                    .orElseThrow(() -> new RuntimeException("No photo found"));

            // Скачиваем фото
            byte[] imageData = downloadPhoto(photo);

            // Анализируем через Google Vision API
            String analysisResult = visionService.analyzeImage(imageData);

            // Отправляем результат
            sendMessage(chatId, analysisResult);

        } catch (Exception e) {
            log.error("Error processing photo", e);
            sendMessage(chatId, "Ошибка: " + e.getMessage());
        }
    }

    private byte[] downloadPhoto(PhotoSize photo) {
        try {
            GetFile getFile = new GetFile(photo.getFileId());
            File file = execute(getFile);
            String fileUrl = "https://api.telegram.org/file/bot" + botToken + "/" + file.getFilePath();

            try (InputStream in = new URL(fileUrl).openStream()) {
                return in.readAllBytes();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to download photo", e);
        }
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setParseMode("Markdown");

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send message", e);
        }
    }
}