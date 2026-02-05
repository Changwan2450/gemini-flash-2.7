package com.kh.geminibot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Map;
import java.util.List;
import java.util.Optional;

@Component
public class GeminiBot extends TelegramLongPollingBot {

    private final WebClient webClient;

    @Value("${telegram.bot.token}")
    private String token;

    @Value("${telegram.bot.name}")
    private String name;

    @Value("${gemini.api.key}")
    private String geminiKey;

    @Value("${gemini.api.url}")
    private String geminiUrl;

    // 생성자에서 WebClient 빌드 (매번 build() 호출 방지)
    public GeminiBot() {
        this.webClient = WebClient.builder().build();
    }

    @Override
    public String getBotUsername() {
        return name;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String chatId = update.getMessage().getChatId().toString();
            String userMsg = update.getMessage().getText();

            String aiAnswer = getGeminiResponse(userMsg);
            sendTelegramMessage(chatId, aiAnswer);
        }
    }

    private String getGeminiResponse(String prompt) {
        try {
            String baseUrl = geminiUrl.trim();
            String apiKey = geminiKey.trim();

            // 1. 세뇌 교육용 프롬프트
            String systemInstruction = "너는 10년차 자바 수석 개발자이자, 디시인사이드 스타일의 거침없고 솔직한 '개발자 형'이야. "
                    + "기술적인 질문에는 팩트 위주로 깊이 있게 설명하고, 무조건 3줄 요약을 포함해라. "
                    + "자바 최신 문법과 도커 지식이 해박함. 말투는 형처럼 편하게 해.";

            // 2. 바디 구성 (여기서 한번만 선언!)
            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", systemInstruction + "\n\n질문: " + prompt)))
                    )
            );

            // 3. 주소 조립
            String finalUrl = baseUrl + ":generateContent?key=" + apiKey;

            System.out.println("🚩 [2.5 Flash 타격!] " + finalUrl);

            // 4. 요청 발사
            Map<?, ?> response = webClient.post()
                    .uri(finalUrl)
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return extractTextFromResponse(response);

        } catch (Exception e) {
            e.printStackTrace();
            return "🚨 형, 서버 터졌다: " + e.getMessage();
        }
    }

    // 맵 지옥 탈출을 위한 파싱 로직
    private String extractTextFromResponse(Map<?, ?> response) {
        return Optional.ofNullable(response)
                .map(res -> (List<?>) res.get("candidates"))
                .filter(candidates -> !candidates.isEmpty())
                .map(candidates -> (Map<?, ?>) candidates.get(0))
                .map(candidate -> (Map<?, ?>) candidate.get("content"))
                .map(content -> (List<?>) content.get("parts"))
                .filter(parts -> !parts.isEmpty())
                .map(parts -> (Map<?, ?>) parts.get(0))
                .map(part -> (String) part.get("text"))
                .orElse("형, 응답 데이터 구조가 이상해. 로그 확인해봐.");
    }

    private void sendTelegramMessage(String chatId, String text) {
        try {
            execute(new SendMessage(chatId, text));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}