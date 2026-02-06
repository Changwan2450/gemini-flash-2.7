package com.kh.geminibot;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling // 스케줄링 활성화
public class GithubScheduler {

    private final GeminiBot geminiBot;

    public GithubScheduler(GeminiBot geminiBot) {
        this.geminiBot = geminiBot;
    }

    // zone = "Asia/Seoul"을 추가해서 한국 시간으로 박아버리기
    // 매일 밤 11시 0분 0초에만 실행
    @Scheduled(cron = "0 0 23 * * *", zone = "Asia/Seoul")
    public void remindGithub() {
        geminiBot.sendSystemAlarm("🌿형, 오늘 잔디 심었어? 자기 전에 확인해봐." );
    }

    // 매주 월요일 아침 9시에 서버 상태 보고
    @Scheduled(cron = "0 0 9 * * MON")
    public void weeklyStatus() {
        geminiBot.sendSystemAlarm("☀️ 기분 좋은 월요일!\n서버는 현재 정상 가동 중이야. `/status`로 한 번 확인해봐.");
    }
}