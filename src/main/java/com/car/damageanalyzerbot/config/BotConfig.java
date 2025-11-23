package com.car.damageanalyzerbot.config;

import com.car.damageanalyzerbot.bot.SimpleCarBot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Slf4j
@Configuration
public class BotConfig {

    @Bean
    public TelegramBotsApi telegramBotsApi(SimpleCarBot bot) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(bot);
            log.info("✅ Telegram bot registered successfully");
            return botsApi;
        } catch (TelegramApiException e) {
            log.error("❌ Failed to register Telegram bot", e);
            throw new RuntimeException("Telegram bot registration failed", e);
        }
    }
}