package com.example.bot.controller;

import com.example.bot.entity.Orders;
import com.example.bot.entity.Product;
import com.example.bot.model.*;
import com.example.bot.service.OrdersService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/orders")
public class OrdersController {

    @Autowired
    private OrdersService ordersService;


    @PostMapping("/checkout")
    public ResponseEntity<OrderResponseDTO> checkout(@RequestBody CheckoutDTO checkoutDTO) throws Exception {




        return ordersService.checkout(checkoutDTO);
    }

    @GetMapping("/{orderId}/details")
    public ResponseEntity<List<ProductWithAccounts>> getOrderDetails(@PathVariable String orderId) {


        return ResponseEntity.ok(ordersService.getOrderDetails(orderId));
    }


    @PostMapping("/orders")
    public ResponseEntity<?> getUserOrders(@RequestParam String initData) throws Exception {
        return ordersService.getOrdersByTelegramId(initData);
    }




}
