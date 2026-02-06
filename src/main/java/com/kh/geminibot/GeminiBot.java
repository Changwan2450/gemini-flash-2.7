package com.kh.geminibot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class GeminiBot extends TelegramLongPollingBot {

    private final ChatLogRepository chatLogRepository;
    private final WebClient webClient;
    private final String ADMIN_ID = "7627020793";

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

            String aiAnswer = getGeminiResponseWithContext(chatId, userMsg);
            sendTelegramMessage(chatId, aiAnswer);
            saveToDb(chatId, userMsg, aiAnswer);
        }
    }
    // 특정 타겟(형)에게 강제로 메시지 전송
    public void sendSystemAlarm(String message) {
        sendMarkdownMessage(ADMIN_ID, "📢 *[시스템 알림]*\n\n" + message);
    }

    private void handleCommand(String chatId, String command) {
        if (!chatId.equals(ADMIN_ID)) {
            sendTelegramMessage(chatId, "🚫 권한 없음.");
            return;
        }

        if (command.equals("/status")) {
            File root = new File("/host_root");
            long total = root.getTotalSpace() / (1024 * 1024 * 1024);
            long free = root.getFreeSpace() / (1024 * 1024 * 1024);
            String msg = String.format("🖥️ *[맥 미니 상태]*\n- 전체: %dGB\n- 여유: %dGB\n- 사용: %dGB", total, free, total - free);
            sendMarkdownMessage(chatId, msg);

        } else if (command.equals("/top")) {
            String result = executeCommand("cat /host_root/changwan/untitled/top_result.txt");
            StringBuilder sb = new StringBuilder();
            sb.append("📊 *[맥 미니 본체 실시간 점유율]*\n");
            sb.append("```\n");
            sb.append(String.format("%-7s %-7s %-15s\n", "CPU%", "MEM%", "COMMAND"));
            sb.append("-------------------------------\n");
            sb.append(result);
            sb.append("\n```");
            sendMarkdownMessage(chatId, sb.toString());

        } else if (command.equals("/ls")) {
            String result = executeCommand("ls -al /host_root/Users");
            sendTelegramMessage(chatId, "📂 [파일 목록]\n" + result);

        } else if (command.equals("/memo")) {
            chatLogRepository.findFirstByUserIdOrderByCreatedAtDesc(chatId).ifPresentOrElse(
                    lastLog -> sendTelegramMessage(chatId, "📌 마지막 질문: " + lastLog.getMessage()),
                    () -> sendTelegramMessage(chatId, "기록 없음.")
            );
        } else if (command.equals("/count")) {
            sendTelegramMessage(chatId, "📊 총 로그 수: " + chatLogRepository.count());
        } else if (command.equals("/clean")) {
            chatLogRepository.deleteByCreatedAtBefore(LocalDateTime.now().minusMonths(1));
            sendTelegramMessage(chatId, "🧹 한 달 전 로그 청소 완료.");
        } else {
            sendTelegramMessage(chatId, "🤖 명령: /status, /top, /ls, /memo, /count, /clean");
        }
    }

    private void sendMarkdownMessage(String chatId, String text) {
        try {
            SendMessage message = new SendMessage(chatId, text);
            message.setParseMode("Markdown");
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private String getGeminiResponseWithContext(String chatId, String prompt) {
        try {
            List<ChatLog> history = chatLogRepository.findTop5ByUserIdOrderByCreatedAtDesc(chatId);
            String context = history.stream()
                    .map(log -> "User: " + log.getMessage() + "\nAI: " + log.getResponse())
                    .collect(Collectors.joining("\n"));

            String finalUrl = geminiUrl.trim() + ":generateContent?key=" + geminiKey.trim();
            String systemInstruction = "너는 10년차 자바 개발자 '형'이야. 이전 문맥 참고해서 3줄 요약 대답해.";
            String fullPrompt = String.format("%s\n\n[이전 대화]\n%s\n\n현재 질문: %s", systemInstruction, context, prompt);

            Map<String, Object> body = Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", fullPrompt)))));
            Map<?, ?> response = webClient.post().uri(finalUrl).header("Content-Type", "application/json").bodyValue(body).retrieve().bodyToMono(Map.class).block();
            return extractTextFromResponse(response);
        } catch (Exception e) {
            return "🚨 제미나이 에러: " + e.getMessage();
        }
    }

    private void saveToDb(String chatId, String userMsg, String aiAnswer) {
        try {
            chatLogRepository.save(ChatLog.builder().userId(chatId).message(userMsg).response(aiAnswer).createdAt(LocalDateTime.now()).build());
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

    private String executeCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", command});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            return "🚨 에러: " + e.getMessage();
        }
    }

    private void sendTelegramMessage(String chatId, String text) {
        try {
            execute(new SendMessage(chatId, text));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}