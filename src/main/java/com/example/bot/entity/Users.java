package com.example.bot.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document("members")
public class Users {

    @Id
    private String id;

    private Long telegramId;
    private String username;
    private BigDecimal balance = BigDecimal.ZERO;
    private Instant createdAt = Instant.now();

    private String referralCode;
    private String referredBy;
}
