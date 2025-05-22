package com.example.bot.config;

import com.example.bot.service.TelegramBotService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class TelegramBotConfig {

    private final TelegramBotService botService;

    public TelegramBotConfig(TelegramBotService botService) {
        this.botService = botService;
    }

    @Bean
    public TelegramBotsApi telegramBotsApi() throws Exception {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(botService);
        System.out.println("Бот зарегистрирован вручную.");
        return botsApi;
    }
}
