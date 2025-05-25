package com.example.bot.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "categories")
public class Category {
    @Id
    private String id;

    private String name; // Имя категории
    private String image; // Изображение в Base64 формате

    // Конструкторы
    public Category() {}

    public Category(String name, String image) {
        this.name = name;
        this.image = image;
    }

    // Геттеры и Сеттеры
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

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}