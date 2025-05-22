package com.example.bot.service;

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

import java.util.*;

import static org.telegram.abilitybots.api.objects.Locality.ALL;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;

@Component
public class TelegramBotService extends AbilityBot {

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
            case "👤 Профиль" -> profile().action().accept(ctx);
            case "🛒 Магазин" -> sendText(chatId, "Магазин в разработке");
            default -> super.onUpdateReceived(update);
        }
    }

    private void handleCallback(Update update) {
        String data = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();

        switch (data) {
            case "top_up_balance" -> {
                awaitingTopUpAmount.put(chatId, true);
                sendText(chatId, "Пожалуйста, укажите сумму в гривнах для пополнения и отправьте её.");
                answerCallback(update);
            }
            default -> super.onUpdateReceived(update);
        }
    }

    private void answerCallback(Update update) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(update.getCallbackQuery().getId());
        answer.setShowAlert(false);
        silent.execute(answer);
    }

    private void processTopUpAmount(Long chatId, String text) {
        try {
            int amount = Integer.parseInt(text.trim());

            if (amount <= 0) {
                sendText(chatId, "Введите корректную сумму (число больше 0). Попробуйте снова.");
                return;
            }

            awaitingTopUpAmount.remove(chatId);

            CreateInvoiceRequest request = new CreateInvoiceRequest(amount * 100);
            CreateInvoiceResponse response = monoBankClient.createInvoice(request);

            sendInvoiceButton(chatId, amount, response.getPageUrl());
            sendMainMenu(chatId);

        } catch (NumberFormatException e) {
            sendText(chatId, "Пожалуйста, введите числовое значение суммы.");
        } catch (TelegramApiException e) {
            throw new RuntimeException("Ошибка при отправке сообщения", e);
        }
    }

    private void sendInvoiceButton(Long chatId, int amount, String url) throws TelegramApiException {
        InlineKeyboardButton payButton = InlineKeyboardButton.builder()
                .text("Оплатить " + amount + " грн")
                .url(url)
                .build();

        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
                .keyboard(List.of(List.of(payButton)))
                .build();

        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text("Ваша заявка на пополнение баланса на сумму " + amount + " грн принята.\nНажмите кнопку ниже для оплаты:")
                .replyMarkup(markup)
                .build();

        execute(message);
    }

    public Ability start() {
        return Ability.builder()
                .name("start")
                .info("Главное меню")
                .locality(ALL)
                .privacy(PUBLIC)
                .action(ctx -> sendMenu(ctx.chatId()))
                .build();
    }

    public Ability profile() {
        return Ability.builder()
                .name("profile")
                .info("Показать профиль")
                .locality(ALL)
                .privacy(PUBLIC)
                .action(ctx -> {
                    String profile = String.format("""
                            ❤️ Имя: ^_^
                            🔑 ID: %d
                            💰 Ваш баланс: 0 $
                            """, ctx.user().getId());

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
                List.of(button("💳 Пополнить баланс", "top_up_balance")),
                List.of(button("📜 История покупок", "purchase_history")),
                List.of(button("💵 История пополнений", "top_up_history"))
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
                new KeyboardButton("🛒 Магазин"),
                new KeyboardButton("👤 Профиль")
        ));

        KeyboardRow row2 = new KeyboardRow(List.of(
                new KeyboardButton("ℹ️ О магазине")
        ));

        keyboard.add(row1);
        keyboard.add(row2);

        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .oneTimeKeyboard(false)
                .build();
    }

    private void sendText(Long chatId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .build();
        silent.execute(message);
    }

    private void sendMenu(Long chatId) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text("💎 Главное меню")
                .replyMarkup(getMainMenuKeyboard())
                .build();
        silent.execute(message);
    }

    private void sendMainMenu(Long chatId) {
        sendMenu(chatId);
    }
}
