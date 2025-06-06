package com.example.bot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CallbackData {
    private String invoiceId;
    private String status;
    private String payMethod;
    private int amount;
    private int ccy;
    private int finalAmount;
    private ZonedDateTime createdDate;
    private ZonedDateTime modifiedDate;




}
