package com.example.bot.controller;

import com.example.bot.entity.Category;
import com.example.bot.entity.Product;
import com.example.bot.model.Account;
import com.example.bot.model.ProductWithAvailabilityDto;
import com.example.bot.repository.CategoryRepository;
import com.example.bot.repository.ProductRepository;
import com.example.bot.service.ProductsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
import java.io.IOException;
import java.util.Base64;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductsService productsService;

    @Autowired
    private CategoryRepository categoryRepository;

    @GetMapping
    public String listProducts(Model model) {
        model.addAttribute("products", productRepository.findAll());
        model.addAttribute("title", "Список Товаров");
        return "products/list";
    }

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

    @GetMapping("/edit/{id}")
    public String editProduct(@PathVariable("id") String id, Model model) {
        model.addAttribute("product", productRepository.findById(id).orElseThrow());
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("title", "Редактировать Товар");
        return "products/form";
    }

    @GetMapping("/delete/{id}")
    public String deleteProduct(@PathVariable("id") String id) {
        productRepository.deleteById(id);
        return "redirect:/products";
    }

    @GetMapping("/{id}")
    public String viewProductDetails(@PathVariable("id") String id, Model model) {
        Product product = productRepository.findById(id).orElseThrow();
        model.addAttribute("product", product);
        model.addAttribute("title", "Детали товара");
        return "products/details";
    }

    @PostMapping("/{id}/add-account")
    public String addAccountToProduct(@PathVariable("id") String id,
                                      @RequestParam("username") String username,
                                      @RequestParam("password") String password) {
        Product product = productRepository.findById(id).orElseThrow();
        product.getAccounts().add(new Account(username, password));
        productRepository.save(product);
        return "redirect:/products/" + id;
    }

    @GetMapping("/{productId}/remove-account/{accountIndex}")
    public String removeAccountFromProduct(@PathVariable("productId") String productId,
                                           @PathVariable("accountIndex") int accountIndex) {
        Product product = productRepository.findById(productId).orElseThrow();
        product.getAccounts().remove(accountIndex);
        productRepository.save(product);
        return "redirect:/products/" + productId;
    }

    @GetMapping("/by-category/{categoryId}")
    public ResponseEntity<List<Product>> getProductsByCategoryId(@PathVariable String categoryId) {
        List<Product> products = productRepository.findByCategory_Id(categoryId);

        List<Product> filteredProducts = products.stream()
                .filter(product -> product.getAccounts() != null && !product.getAccounts().isEmpty())
                .collect(Collectors.toList());

        if (filteredProducts.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        filteredProducts.forEach(product -> product.setAccounts(new ArrayList<>()));

        return ResponseEntity.ok(filteredProducts);
    }


    @PostMapping("/api/by-ids")
    @ResponseBody
    public List<ProductWithAvailabilityDto> getProductsByIds(@RequestBody List<String> ids) {
        return productsService.getProductsWithAvailability(ids);
    }

}