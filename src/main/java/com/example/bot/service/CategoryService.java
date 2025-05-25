package com.example.bot.service;

import com.example.bot.entity.Category;
import com.example.bot.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    // Получить все категории
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    // Получить категорию по ID
    public Optional<Category> getCategoryById(String id) {
        return categoryRepository.findById(id);
    }

    // Создать или обновить категорию
    public Category saveCategory(Category category) {
        // Если у категории есть ID и она существует — обновляем
        if (category.getId() != null && !category.getId().equals("") && categoryRepository.existsById(category.getId())) {
            Category existingCategory = categoryRepository.findById(category.getId()).orElseThrow();
            existingCategory.setName(category.getName()); // Обновляем название
            existingCategory.setImage(category.getImage()); // Обновляем картинку
            return categoryRepository.save(existingCategory); // Сохраняем обновление
        }
        // Если ID нет, сохраняем как новую категорию
        return categoryRepository.save(category);
    }

    // Удалить категорию
    public void deleteCategory(String id) {
        categoryRepository.deleteById(id);
    }
}