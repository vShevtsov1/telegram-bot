package com.example.bot.service;

import com.example.bot.entity.Invoice;
import com.example.bot.repository.InvoiceRepository;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    public void save(Invoice invoice) {

        invoiceRepository.save(invoice);
    }

    public List<Invoice> findByTelegramUserId(String telegramUserId) {
        return invoiceRepository.findByTelegramUserId(telegramUserId);
    }
}
