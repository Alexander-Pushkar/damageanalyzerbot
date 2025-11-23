package com.car.damageanalyzerbot.bot;

import com.car.damageanalyzerbot.service.SimpleVisionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.InputStream;
import java.net.URL;

@Slf4j
@Component
public class SimpleCarBot extends TelegramLongPollingBot {

    private final SimpleVisionService visionService;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.name}")
    private String botName;

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
        try {
            if (update.hasMessage()) {
                Message message = update.getMessage();
                if (message.hasText()) {
                    Long chatId = message.getChatId();
                    sendSimpleMessage(chatId, "📸 Отправь фото для анализа");
                }

                int isImage = isPhotoMessage(message); // 0-false 1-photo 2 doc
                if (isImage != 0) {
                    handlePhotoMessage(message, isImage);
                }
            }
        } catch (Exception e) {
            System.out.println("Error in onUpdateReceived: " + e.getMessage());
        }
    }

    private int isPhotoMessage(Message message) {
        // 1. Проверяем обычные фото
        if (message.getPhoto() != null && !message.getPhoto().isEmpty()) {
            return 1;
        }

        // 2. Проверяем документы-изображения
        if (message.hasDocument()) {
            Document doc = message.getDocument();
            String mimeType = doc.getMimeType();
            if (mimeType != null && mimeType.startsWith("image/")) {
                return 2;
            }
            return 0;
        }

        return 0;
    }

    private void handlePhotoMessage(Message message, int photoOrDoc) {
        Long chatId = message.getChatId();
        try {
            sendSimpleMessage(chatId, "🔍 Анализирую фото...");

            byte[] imageData = downloadImageData(message, photoOrDoc);
            String textResult = visionService.analyzeImage(imageData);

            sendSimpleMessage(chatId, textResult);
        } catch (Exception e) {
            sendSimpleMessage(chatId, "❌ Ошибка обработки фото: " + e.getMessage());
        }
    }

    private byte[] downloadImageData(Message message, int photoOrDoc) {
        try {
            // Пробуем скачать как фото
            if (photoOrDoc == 1) {
                PhotoSize photo = message.getPhoto().get(message.getPhoto().size() - 1);
                return downloadImageFile(photo.getFileId());
            }

            // Пробуем скачать как документ
            return downloadImageFile(message.getDocument().getFileId());

        } catch (Exception e) {
            throw new RuntimeException("Failed to download image: " + e.getMessage());
        }
    }

    private byte[] downloadImageFile(String fileId) throws Exception {
            GetFile getFile = new GetFile(fileId);
            File file = execute(getFile);
            try (InputStream in = new URL(file.getFileUrl(getBotToken())).openStream()) {
                return in.readAllBytes();
            }
    }

    private void sendSimpleMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.out.println("Failed to send message: " + e.getMessage());
        }
    }
}