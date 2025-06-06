package com.example.bot.repository;

import com.example.bot.entity.Orders;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OrdersRepository extends MongoRepository<Orders, String> {

    List<Orders> findByUsers_Id(String telegramId);

}
