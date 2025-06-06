package com.example.bot.model;

import com.example.bot.entity.Category;
import lombok.Data;

@Data
public class ProductWithAvailabilityDto {
    private String id;
    private String name;
    private String description;
    private Double price;
    private String image;
    private Category category;
    private int available;


    public ProductWithAvailabilityDto() {}

    public ProductWithAvailabilityDto(String id, String name, String description, Double price, String image, Category category, int available) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.image = image;
        this.category = category;
        this.available = available;
    }

    // геттеры и сеттеры ниже
    // ...
}
