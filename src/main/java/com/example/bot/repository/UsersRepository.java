package com.example.bot.repository;

import com.example.bot.entity.Users;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UsersRepository extends MongoRepository<Users, String> {

    Optional<Users> findByTelegramId(Long telegramId);
    Optional<Users> findByReferralCode(String referralCode);
}
