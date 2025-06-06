package com.example.bot.service;

import com.example.bot.entity.Invoice;
import com.example.bot.model.CreateInvoiceRequest;
import com.example.bot.model.CreateInvoiceResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.objects.MessageContext;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.telegram.abilitybots.api.objects.Locality.ALL;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;

@Component
public class TelegramBotService extends AbilityBot {

    private static final int PAGE_SIZE = 5;

    @Autowired
    private UsersService usersService;

    @Autowired
    private InvoiceService invoiceService;

    private final MonoBankClient monoBankClient;
    private final Map<Long, Boolean> awaitingTopUpAmount = new HashMap<>();

    @Autowired
    public TelegramBotService(
            MonoBankClient monoBankClient,
            @Value("${telegrambots.bots[0].username}") String username,
            @Value("${telegrambots.bots[0].token}") String token) {
        super(token, username);
        this.monoBankClient = monoBankClient;
    }

    @Override
    public long creatorId() {
        return 425373713L;
    }

    @PostConstruct
    public void init() {
        System.out.println("TelegramBotService инициализирован. Бот: " + getBotUsername());
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleTextMessage(update);
        } else if (update.hasCallbackQuery()) {
            handleCallback(update);
        } else {
            super.onUpdateReceived(update);
        }
    }

    private void handleTextMessage(Update update) {
        String text = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();

        if (awaitingTopUpAmount.getOrDefault(chatId, false)) {
            processTopUpAmount(chatId, text);
            return;
        }

        User user = update.getMessage().getFrom();
        MessageContext ctx = MessageContext.newContext(update, user, chatId, this);

        switch (text) {
            case "/start" -> start().action().accept(ctx);
            case "👤 Профіль" -> profile().action().accept(ctx);
            case "🛒 Магазин" -> sendText(chatId, "Магазин в разработке");
            default -> super.onUpdateReceived(update);
        }
    }

    private void handleCallback(Update update) {
        String data = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();

        if (data.equals("top_up_balance")) {
            awaitingTopUpAmount.put(chatId, true);
            sendText(chatId, "Будь ласка, вкажіть суму у гривнях для поповнення та надішліть її.");
            answerCallback(update);
        } else if (data.equals("top_up_history")) {
            sendTopUpHistory(chatId, 0, null);
            answerCallback(update);
        } else if (data.startsWith("top_up_history:")) {
            try {
                int page = Integer.parseInt(data.split(":")[1]);
                sendTopUpHistory(chatId, page, messageId);
                answerCallback(update);
            } catch (NumberFormatException e) {
                answerCallback(update);
            }
        } else {
            super.onUpdateReceived(update);
        }
    }

    private void answerCallback(Update update) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(update.getCallbackQuery().getId());
        answer.setShowAlert(false);
        silent.execute(answer);
    }

    private void sendTopUpHistory(Long chatId, int page, Integer messageId) {
        List<Invoice> invoices = invoiceService.findByTelegramUserId(String.valueOf(chatId));
        invoices.sort(Comparator.comparing(Invoice::getCreatedAt).reversed());

        int totalPages = (invoices.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        if (totalPages == 0) totalPages = 1;
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, invoices.size());

        StringBuilder sb = new StringBuilder();
        sb.append("💵 Історія поповнень (сторінка ").append(page + 1).append(" з ").append(totalPages).append("):\n\n");

        if (invoices.isEmpty()) {
            sb.append("Історія пуста.");
        } else {
            for (int i = start; i < end; i++) {
                Invoice inv = invoices.get(i);
                String statusIcon = getStatusIcon(inv.getStatus());
                String date = inv.getCreatedAt().toLocalDate().toString();
                sb.append(String.format("%d) %d $ — %s %s\n", i + 1, inv.getAmount(), date, statusIcon));
            }
        }

        InlineKeyboardMarkup keyboard = buildPaginationKeyboard("top_up_history", page, totalPages);

        if (messageId == null) {
            SendMessage message = SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(sb.toString())
                    .replyMarkup(keyboard)
                    .build();

            silent.execute(message);
        } else {
            var editMessage = new org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText();
            editMessage.setChatId(chatId.toString());
            editMessage.setMessageId(messageId);
            editMessage.setText(sb.toString());
            editMessage.setReplyMarkup(keyboard);
            silent.execute(editMessage);
        }
    }

    private InlineKeyboardMarkup buildPaginationKeyboard(String prefix, int page, int totalPages) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> buttons = new ArrayList<>();

        if (page > 0) {
            buttons.add(button("⬅️ Назад", prefix + ":" + (page - 1)));
        }
        if (page < totalPages - 1) {
            buttons.add(button("Вперед ➡️", prefix + ":" + (page + 1)));
        }
        if (!buttons.isEmpty()) {
            rows.add(buttons);
        }
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private void processTopUpAmount(Long chatId, String text) {
        try {
            int amount = Integer.parseInt(text.trim());

            if (amount <= 0) {
                sendText(chatId, "Введіть коректну суму (більше 0). Спробуйте ще раз.");
                return;
            }

            awaitingTopUpAmount.remove(chatId);

            CreateInvoiceRequest request = new CreateInvoiceRequest(amount * 100);
            CreateInvoiceResponse response = monoBankClient.createInvoice(request);

            Invoice invoice = new Invoice();
            invoice.setInvoiceId(response.getInvoiceId());
            invoice.setAmount(Long.valueOf(amount));
            invoice.setStatus("created");
            invoice.setTelegramUserId(String.valueOf(chatId));
            invoice.setCreatedAt(LocalDateTime.now());
            invoiceService.save(invoice);

            sendInvoiceButton(chatId, amount, response.getPageUrl());
            sendMainMenu(chatId);

        } catch (NumberFormatException e) {
            sendText(chatId, "Введіть числове значення суми.");
        } catch (TelegramApiException e) {
            throw new RuntimeException("Ошибка при отправке сообщения", e);
        }
    }

    private void sendInvoiceButton(Long chatId, int amount, String url) throws TelegramApiException {
        InlineKeyboardButton payButton = InlineKeyboardButton.builder()
                .text("Сплатити " + amount + " $")
                .url(url)
                .build();

        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
                .keyboard(List.of(List.of(payButton)))
                .build();

        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text("Ваша заявка на поповнення балансу на суму " + amount + " $ прийнято.\n" +
                        "Натисніть кнопку нижче для оплати:")
                .replyMarkup(markup)
                .build();

        execute(message);
    }

    public Ability start() {
        return Ability.builder()
                .name("start")
                .info("Головне меню")
                .locality(ALL)
                .privacy(PUBLIC)
                .action(ctx -> {
                    usersService.registerUserIfNotExists(ctx);
                    sendMenu(ctx.chatId());
                })
                .build();
    }

    public Ability profile() {
        return Ability.builder()
                .name("profile")
                .info("Показати профіль")
                .locality(ALL)
                .privacy(PUBLIC)
                .action(ctx -> {
                    BigDecimal balance = usersService.getUserBalanceByContext(ctx);

                    String profile = String.format("""
                        ❤️ Ім'я: %s
                        🔑 ID: %d
                        💰 Ваш баланс: %s $
                        """, ctx.user().getFirstName(), ctx.user().getId(), balance);

                    SendMessage message = new SendMessage();
                    message.setChatId(ctx.chatId().toString());
                    message.setText(profile);
                    message.setReplyMarkup(getProfileKeyboard());
                    silent.execute(message);
                })
                .build();
    }

    private InlineKeyboardMarkup getProfileKeyboard() {
        List<List<InlineKeyboardButton>> keyboard = List.of(
                List.of(button("💳 Поповнити баланс", "top_up_balance")),
                List.of(button("💵 Історія поповнень", "top_up_history"))
        );
        return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }

    private InlineKeyboardButton button(String text, String callbackData) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(callbackData)
                .build();
    }

    private ReplyKeyboardMarkup getMainMenuKeyboard() {
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow(List.of(
                new KeyboardButton("👤 Профіль")
        ));

        KeyboardRow row2 = new KeyboardRow(List.of(
                new KeyboardButton("ℹ️ Про магазин")
        ));

        keyboard.add(row1);
        keyboard.add(row2);

        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .oneTimeKeyboard(false)
                .build();
    }

    public void sendText(Long chatId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .build();
        silent.execute(message);
    }

    private void sendMenu(Long chatId) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text("💎 Головне меню")
                .replyMarkup(getMainMenuKeyboard())
                .build();
        silent.execute(message);
    }

    private void sendMainMenu(Long chatId) {
        sendMenu(chatId);
    }

    private String getStatusIcon(String status) {
        return switch (status.toLowerCase()) {
            case "created" -> "🕒 Очікування оплати";
            case "success" -> "✅ Оплачено";
            case "cancelled" -> "❌ Скасовано";
            case "expired" -> "⏳ Термін дії минув";
            default -> "ℹ️ " + status;
        };
    }


}
