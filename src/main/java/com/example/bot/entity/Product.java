package com.example.bot.entity;

import com.example.bot.model.Account;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "products")
public class Product {

    @Id
    private String id;
    private String name; // Название товара
    private String description; // Описание товара
    private Double price;
    private String image; // Фото (в формате Base64)

    @DBRef
    private Category category;

    private List<Account> accounts = new ArrayList<>();

    // Конструкторы, геттеры и сеттеры
    public Product() {}

    public Product(String name, String description, String image, Category category, List<Account> accounts) {
        this.name = name;
        this.description = description;
        this.image = image;
        this.category = category;
        this.accounts = accounts;
    }

    public Product(String name, String description, Double price, String image, Category category, List<Account> accounts) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.image = image;
        this.category = category;
        this.accounts = accounts;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts;
    }
}