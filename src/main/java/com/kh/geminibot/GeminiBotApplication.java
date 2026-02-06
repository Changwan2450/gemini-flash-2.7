package com.kh.geminibot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling; // 1. 임포트 추가
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
@EnableScheduling // 2. 여기! 이 도장이 있어야 @Scheduled가 작동해!
public class GeminiBotApplication {
    public static void main(String[] args) {
        SpringApplication.run(GeminiBotApplication.class, args);
    }

    @Bean
    public TelegramBotsApi telegramBotsApi(GeminiBot geminiBot) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(geminiBot);
        return botsApi;
    }
}