package com.example.bot.controller;

import com.example.bot.entity.Category;
import com.example.bot.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

@Controller
@RequestMapping("/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping
    public String listCategories(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("title", "Список Категорий");
        model.addAttribute("headerTitle", "Категории");
        return "categories/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("category", new Category());
        model.addAttribute("title", "Создать Категорию");
        model.addAttribute("headerTitle", "Создание Категории");
        return "categories/form";
    }

    @PostMapping
    public String saveCategory(
            @ModelAttribute("category") Category category,
            @RequestParam("imageFile") MultipartFile imageFile) throws IOException {

        if (category.getId() == null || category.getId().trim().isEmpty()) {
            category.setId(null); // Устанавливаем null
        }
        if (imageFile != null && !imageFile.isEmpty()) {
            category.setImage(Base64.getEncoder().encodeToString(imageFile.getBytes()));
        } else {
            Category existingCategory = null;
            if (category.getId() != null) {
                existingCategory = categoryService.getCategoryById(category.getId()).orElse(null);
            }
            if (existingCategory != null) {
                category.setImage(existingCategory.getImage());
            }
        }

        categoryService.saveCategory(category);
        return "redirect:/categories";
    }

    // Показать форму редактирования
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") String id, Model model) {
        model.addAttribute("category", categoryService.getCategoryById(id).orElseThrow());
        model.addAttribute("title", "Редактировать Категорию");
        model.addAttribute("headerTitle", "Редактирование Категории");
        return "categories/form";

    }

    // Удаление категории
    @GetMapping("/delete/{id}")
    public String deleteCategory(@PathVariable("id") String id) {
        categoryService.deleteCategory(id);
        return "redirect:/categories";
    }
    @GetMapping("/api")
    @ResponseBody
    public List<Category> getAllCategoriesApi() {
        return categoryService.getAllCategories();
    }
}