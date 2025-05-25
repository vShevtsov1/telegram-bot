package com.example.bot.repository;

import com.example.bot.entity.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    Optional<Product> getProductById(String id);
    // Можем использовать стандартные CRUD операции
}