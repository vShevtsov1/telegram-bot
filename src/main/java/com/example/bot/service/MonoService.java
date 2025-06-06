package com.example.bot.service;

import com.example.bot.entity.Invoice;
import com.example.bot.entity.Users;
import com.example.bot.model.CallbackData;
import com.example.bot.repository.InvoiceRepository;
import com.example.bot.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.User;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class MonoService {

    @Autowired
    private InvoiceRepository invoiceRepository;
    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private TelegramBotService telegramBotService;

    public void topUpProcess(CallbackData callbackData) {
        Optional<Invoice> invoiceOpt = invoiceRepository.findById(callbackData.getInvoiceId());
        if (invoiceOpt.isEmpty()) {
            return; // Нет такого инвойса
        }

        Invoice invoice = invoiceOpt.get();
        String newStatus = callbackData.getStatus();
        String currentStatus = invoice.getStatus();

        if ("success".equals(currentStatus)) {
            return;
        }

        if ("success".equals(newStatus)) {
            invoice.setStatus(newStatus);
            invoiceRepository.save(invoice);

            Optional<Users> userOpt = usersRepository.findByTelegramId(Long.valueOf(invoice.getTelegramUserId()));
            if (userOpt.isPresent()) {
                Users user = userOpt.get();
                user.setBalance(user.getBalance().add(BigDecimal.valueOf(invoice.getAmount())));
                usersRepository.save(user);

                Long chatId = user.getChatId();
                String text = "Баланс успешно пополнен на " + invoice.getAmount() + " $.";
                telegramBotService.sendText(chatId, text);
            }
        } else {
            invoice.setStatus(newStatus);
            invoiceRepository.save(invoice);
        }
    }

}
