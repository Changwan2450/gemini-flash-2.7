package com.kh.geminibot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class GeminiBot extends TelegramLongPollingBot {

    private final ChatLogRepository chatLogRepository;
    private final WebClient webClient;

    @Value("${telegram.bot.token}")
    private String token;

    @Value("${telegram.bot.name}")
    private String name;

    @Value("${gemini.api.key}")
    private String geminiKey;

    @Value("${gemini.api.url}")
    private String geminiUrl;

    public GeminiBot(ChatLogRepository chatLogRepository) {
        this.chatLogRepository = chatLogRepository;
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

            if (userMsg.startsWith("/")) {
                handleCommand(chatId, userMsg);
                return;
            }

            // 기억력(Context)을 포함한 답변 생성
            String aiAnswer = getGeminiResponseWithContext(chatId, userMsg);
            sendTelegramMessage(chatId, aiAnswer);
            saveToDb(chatId, userMsg, aiAnswer);
        }
    }

    private void handleCommand(String chatId, String command) {
        if (command.equals("/memo")) {
            chatLogRepository.findFirstByUserIdOrderByCreatedAtDesc(chatId).ifPresentOrElse(
                    lastLog -> sendTelegramMessage(chatId, "📌 마지막 질문: " + lastLog.getMessage()),
                    () -> sendTelegramMessage(chatId, "기록 없음.")
            );
        } else if (command.equals("/count")) {
            sendTelegramMessage(chatId, "📊 총 로그 수: " + chatLogRepository.count());
        } else if (command.equals("/clean")) {
            chatLogRepository.deleteByCreatedAtBefore(LocalDateTime.now().minusMonths(1));
            sendTelegramMessage(chatId, "🧹 한 달 전 로그 청소 완료.");
        } else if (command.equals("/status")) {
            // [서버 상태 모니터링 기능]
            File root = new File("/host_root");
            long total = root.getTotalSpace() / (1024 * 1024 * 1024);
            long free = root.getFreeSpace() / (1024 * 1024 * 1024);
            long used = total - free;
            String msg = String.format("🖥️ [맥 미니 상태]\n- 전체: %dGB\n- 여유: %dGB\n- 사용: %dGB", total, free, total - free);
            sendTelegramMessage(chatId, msg);
        } else {
            sendTelegramMessage(chatId, "🤖 사용 가능 명령어: /memo, /count, /clean, /status");
        }
    }

    // [기억력 강화 버전] 제미나이 호출
    private String getGeminiResponseWithContext(String chatId, String prompt) {
        try {
            // DB에서 최근 대화 5개 추출
            List<ChatLog> history = chatLogRepository.findTop5ByUserIdOrderByCreatedAtDesc(chatId);

            // 과거 대화 문맥 조립 (최신순이므로 역순으로 보여주는 게 자연스러움)
            String context = history.stream()
                    .map(log -> "User: " + log.getMessage() + "\nAI: " + log.getResponse())
                    .collect(Collectors.joining("\n"));

            String finalUrl = geminiUrl.trim() + ":generateContent?key=" + geminiKey.trim();
            String systemInstruction = "너는 10년차 자바 개발자 '형'이야. 이전 대화 문맥을 참고해서 대답해. 3줄 요약 필수.";

            String fullPrompt = String.format("%s\n\n[이전 대화 내용]\n%s\n\n현재 질문: %s", systemInstruction, context, prompt);

            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", fullPrompt))))
            );

            Map<?, ?> response = webClient.post()
                    .uri(finalUrl)
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return extractTextFromResponse(response);
        } catch (Exception e) {
            return "🚨 에러: " + e.getMessage();
        }
    }

    private void saveToDb(String chatId, String userMsg, String aiAnswer) {
        try {
            chatLogRepository.save(ChatLog.builder()
                    .userId(chatId).message(userMsg).response(aiAnswer)
                    .createdAt(LocalDateTime.now()).build());
        } catch (Exception e) {
            System.err.println("🚨 DB 저장 실패: " + e.getMessage());
        }
    }

    private String extractTextFromResponse(Map<?, ?> response) {
        return Optional.ofNullable(response)
                .map(res -> (List<?>) res.get("candidates")).filter(c -> !c.isEmpty())
                .map(c -> (Map<?, ?>) c.get(0))
                .map(c -> (Map<?, ?>) c.get("content"))
                .map(c -> (List<?>) c.get("parts")).filter(p -> !p.isEmpty())
                .map(p -> (Map<?, ?>) p.get(0))
                .map(p -> (String) p.get("text"))
                .orElse("응답 오류.");
    }

    private void sendTelegramMessage(String chatId, String text) {
        try {
            execute(new SendMessage(chatId, text));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}