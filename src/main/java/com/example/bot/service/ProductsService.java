package com.example.bot.service;

import com.example.bot.entity.Product;
import com.example.bot.model.ProductWithAvailabilityDto;
import com.example.bot.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductsService {

    @Autowired
    private ProductRepository productRepository;

    public List<ProductWithAvailabilityDto> getProductsWithAvailability(List<String> ids) {
        List<Product> products = productRepository.findAllById(ids);

        return products.stream().map(product -> new ProductWithAvailabilityDto(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getImage(),
                product.getCategory(),
                product.getAccounts() != null ? product.getAccounts().size() : 0
        )).collect(Collectors.toList());
    }
}
