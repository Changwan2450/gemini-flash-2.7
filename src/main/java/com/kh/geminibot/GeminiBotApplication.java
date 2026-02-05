package com.kh.geminibot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
public class GeminiBotApplication {
    public static void main(String[] args) {
        SpringApplication.run(GeminiBotApplication.class, args);
    }
    @Bean
    public TelegramBotsApi telegramBotsApi(GeminiBot geminiBot) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(geminiBot); // 여기서 수동으로 봇을 등록해버리는 거야
        return botsApi;
    }
}