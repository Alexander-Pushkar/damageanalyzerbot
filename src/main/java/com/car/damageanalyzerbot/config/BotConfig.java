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
    public TelegramBotsApi telegramBotsApi(SimpleCarBot bot) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        try {
            botsApi.registerBot(bot);
            log.info("Telegram бот успешно зарегистрирован: {}", bot.getBotUsername());
        } catch (TelegramApiException e) {
            log.error("Ошибка регистрации бота", e);
        }
        return botsApi;
    }
}