package com.example.bot.controller;

import com.example.bot.entity.Category;
import com.example.bot.entity.Product;
import com.example.bot.model.Account;
import com.example.bot.repository.CategoryRepository;
import com.example.bot.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

@Controller
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    // Просмотр товаров
    @GetMapping
    public String listProducts(Model model) {
        model.addAttribute("products", productRepository.findAll());
        model.addAttribute("title", "Список Товаров");
        return "products/list";
    }

    // Создание нового товара
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryRepository.findAll()); // Для выбора категории
        model.addAttribute("title", "Создать Товар");
        return "products/form";
    }

    @PostMapping
    public String saveProduct(@ModelAttribute("product") Product product,
                              @RequestParam("imageFile") MultipartFile imageFile) throws IOException {
        if (product.getId() == null || product.getId().trim().isEmpty()) {
            product.setId(null); // Устанавливаем null
        }
        if (imageFile != null && !imageFile.isEmpty()) {
            product.setImage(Base64.getEncoder().encodeToString(imageFile.getBytes()));
        } else {
            Product existingProduct = null;
            if (product.getId() != null) {
                existingProduct = productRepository.getProductById((product.getId())).orElse(null);
            }
            if (existingProduct != null) {
                product.setImage(existingProduct.getImage());
            }
        }
        productRepository.save(product);
        return "redirect:/products";
    }

    // Редактирование товара
    @GetMapping("/edit/{id}")
    public String editProduct(@PathVariable("id") String id, Model model) {
        model.addAttribute("product", productRepository.findById(id).orElseThrow());
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("title", "Редактировать Товар");
        return "products/form";
    }

    // Удаление товара
    @GetMapping("/delete/{id}")
    public String deleteProduct(@PathVariable("id") String id) {
        productRepository.deleteById(id);
        return "redirect:/products";
    }

    // Просмотр деталей товара
    @GetMapping("/{id}")
    public String viewProductDetails(@PathVariable("id") String id, Model model) {
        Product product = productRepository.findById(id).orElseThrow();
        model.addAttribute("product", product);
        model.addAttribute("title", "Детали товара");
        return "products/details";
    }

    // Добавление логина и пароля к товару
    @PostMapping("/{id}/add-account")
    public String addAccountToProduct(@PathVariable("id") String id,
                                      @RequestParam("username") String username,
                                      @RequestParam("password") String password) {
        Product product = productRepository.findById(id).orElseThrow();
        product.getAccounts().add(new Account(username, password));
        productRepository.save(product);
        return "redirect:/products/" + id;
    }

    // Удаление логина и пароля из товара
    @GetMapping("/{productId}/remove-account/{accountIndex}")
    public String removeAccountFromProduct(@PathVariable("productId") String productId,
                                           @PathVariable("accountIndex") int accountIndex) {
        Product product = productRepository.findById(productId).orElseThrow();
        product.getAccounts().remove(accountIndex);
        productRepository.save(product);
        return "redirect:/products/" + productId;
    }
}