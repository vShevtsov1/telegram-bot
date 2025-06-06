package com.example.bot.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document("top-up")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Invoice {
    @Id
    private String invoiceId;

    private String status;
    private Long amount;
    private String currency;
    private String telegramUserId;
    private LocalDateTime createdAt;




}
