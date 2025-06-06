package com.example.bot.repository;

import com.example.bot.entity.Invoice;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface InvoiceRepository extends MongoRepository<Invoice, String> {

    List<Invoice> findByTelegramUserId(String telegramUserId);

}
