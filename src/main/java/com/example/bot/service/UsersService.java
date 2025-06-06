package com.example.bot.service;

import com.example.bot.entity.Users;
import com.example.bot.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.abilitybots.api.objects.MessageContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Service
public class UsersService {

    @Autowired
    private UsersRepository usersRepository;

    public Users registerUserIfNotExists(MessageContext ctx) {
        Long telegramId = ctx.user().getId();
        String username = ctx.user().getUserName();

        Long chatId = ctx.chatId();
        return usersRepository.findByTelegramId(telegramId).orElseGet(() -> {
            Users user = new Users();
            user.setChatId(chatId);
            user.setTelegramId(telegramId);
            user.setUsername(username);
            user.setBalance(BigDecimal.ZERO);
            user.setCreatedAt(Instant.now());

            String referralCode = "TG" + telegramId;
            user.setReferralCode(referralCode);
            String[] args = ctx.arguments();
            if (args.length > 0) {
                String refCode = args[0];
                if (!refCode.equals(referralCode)) {
                    Optional<Users> referrer = usersRepository.findByReferralCode(refCode);
                    referrer.ifPresent(userReferrer -> {
                        user.setReferredBy(userReferrer.getId());
                    });
                }
            }
            return usersRepository.save(user);
        });
    }

    public BigDecimal getUserBalanceByContext(MessageContext ctx) {
        Long telegramId = ctx.user().getId();
        return usersRepository.findByTelegramId(telegramId)
                .map(Users::getBalance)
                .orElse(BigDecimal.ZERO);
    }

    public BigDecimal getUserBalanceByChatId(Long chatId) {
        return usersRepository.findByTelegramId(chatId).get().getBalance();
    }

    public String getUserReferralCode(Long chatId) {
        return usersRepository.findByTelegramId(chatId).get().getReferralCode();
    }

}
