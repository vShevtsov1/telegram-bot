package com.example.bot.model;

import com.example.bot.entity.Product;

import java.util.List;

public class ProductWithAccounts {
    private Product product;
    private List<Account> accounts;

    public ProductWithAccounts(Product product, List<Account> accounts) {
        this.product = product;
        this.accounts = accounts;
    }

    public Product getProduct() {
        return product;
    }

    public List<Account> getAccounts() {
        return accounts;
    }
}
