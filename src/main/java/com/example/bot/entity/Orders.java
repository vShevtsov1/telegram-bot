package com.example.bot.entity;

import com.example.bot.model.Account;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(value = "orders")
public class Orders {

    @Id
    private String id;


    @DBRef
    private Users users;

    private Double price;
    private LocalDateTime orderDate = LocalDateTime.now();

    private Map<String, List<Account>> credentials = new HashMap<>();

}
