package com.example.bot.service;

import com.example.bot.model.CreateInvoiceRequest;
import com.example.bot.model.CreateInvoiceResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class MonoBankClient {

    private final RestTemplate restTemplate;
    private final String baseUrl = "https://api.monobank.ua/api/merchant";
    private final String apiKey;

    public MonoBankClient(@Value("${monobank.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.restTemplate = new RestTemplate();
    }

    public CreateInvoiceResponse createInvoice(CreateInvoiceRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Token", apiKey);

        HttpEntity<CreateInvoiceRequest > entity = new HttpEntity<>(request, headers);

        ResponseEntity<CreateInvoiceResponse> response = restTemplate.exchange(
            baseUrl + "/invoice/create",
            HttpMethod.POST,
            entity,
                CreateInvoiceResponse.class
        );

        return response.getBody();
    }

}
