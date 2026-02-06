import com.kh.geminibot.ChatLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class LogCleaner {
    @Autowired
    private ChatLogRepository repository;

    // 매일 새벽 3시에 한 달 전 로그 삭제
    @Scheduled(cron = "0 0 3 * * *")
    public void deleteOldLogs() {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        repository.deleteByCreatedAtBefore(oneMonthAgo);
        System.out.println("🧹 오래된 로그 청소 완료!");
    }
}