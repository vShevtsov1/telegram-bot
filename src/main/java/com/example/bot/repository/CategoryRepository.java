package com.example.bot.repository;

import com.example.bot.entity.Category;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends MongoRepository<Category, String> {
    // Мы можем использовать стандартные CRUD операции без дополнительного кода
}