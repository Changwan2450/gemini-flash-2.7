package com.kh.geminibot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatLogRepository extends JpaRepository<ChatLog, Long> {

    // 1. 특정 기간 이전 로그 싹 밀어버리기 (LogCleaner용)
    @Modifying
    @Transactional
    void deleteByCreatedAtBefore(LocalDateTime expiryDate);

    // 2. 특정 유저의 가장 최근 대화 1개 가져오기 (봇이 기억하는 척 할 때)
    Optional<ChatLog> findFirstByUserIdOrderByCreatedAtDesc(String userId);

    // 3. 특정 유저의 최근 대화 5개 가져오기 (대화 문맥 유지용)
    List<ChatLog> findTop5ByUserIdOrderByCreatedAtDesc(String userId);
}