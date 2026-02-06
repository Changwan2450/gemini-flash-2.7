package com.kh.geminibot;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "CHAT_LOGS") // 테이블 이름
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Oracle 12c 이상/23ai 국룰
    private Long id;

    private String userId;      // 텔레그램 유저 ID

    @Column(length = 4000)      // 질문 길게 저장
    private String message;

    @Column(length = 4000)      // 답변 길게 저장
    private String response;

    private LocalDateTime createdAt;
}