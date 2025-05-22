package com.example.bot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateInvoiceRequest {
    private int amount;
    private int ccy = 980;
    private String redirectUrl = "http://localhost:8080/";
    private String webHookUrl = "http://localhost:8080/";
    private int validity = 3600;

    public CreateInvoiceRequest(int amount) {
        this.amount = amount;
    }
}
