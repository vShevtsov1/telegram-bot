package com.example.bot.service;

import com.example.bot.entity.Orders;
import com.example.bot.entity.Product;
import com.example.bot.entity.Users;
import com.example.bot.model.*;
import com.example.bot.repository.OrdersRepository;
import com.example.bot.repository.ProductRepository;
import com.example.bot.repository.UsersRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrdersService {

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TelegramBotService telegramBotService;

    @Autowired
    private UsersRepository UsersRepository;
    @Value("${telegrambots.bots[0].token}")
    private String BOT_TOKEN;
    @Autowired
    private UsersRepository usersRepository;

    public ResponseEntity<OrderResponseDTO> checkout(CheckoutDTO checkoutDTO) throws Exception {
        Map<String, Object> userMap = getUserData(checkoutDTO.getInitData());

        if (!userMap.containsKey("user")) {
            return ResponseEntity.badRequest()
                    .body(new OrderResponseDTO("failed", "Невірні дані користувача"));
        }

        Map<String, Object> userInfo = (Map<String, Object>) userMap.get("user");
        Long telegramId = ((Number) userInfo.get("id")).longValue();

        Optional<Users> userOptional = usersRepository.findByTelegramId(telegramId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new OrderResponseDTO("failed", "Користувача не знайдено"));
        }
        Users user = userOptional.get();

        List<OrderItemDto> orderItems = checkoutDTO.getItems();
        List<String> productIds = orderItems.stream()
                .map(OrderItemDto::getId)
                .collect(Collectors.toList());

        List<Product> products = productRepository.findAllByIdIn(productIds);

        Map<String, List<Account>> orderCredentials = new HashMap<>();
        double totalPrice = 0.0;

        // 1. Перевірка доступності товарів і розрахунок ціни
        for (OrderItemDto orderItem : orderItems) {
            Optional<Product> productOptional = products.stream()
                    .filter(p -> p.getId().equals(orderItem.getId()))
                    .findFirst();

            if (productOptional.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new OrderResponseDTO("failed", "Товар з id " + orderItem.getId() + " не знайдено"));
            }

            Product product = productOptional.get();

            int availableQuantity = product.getAccounts() != null ? product.getAccounts().size() : 0;
            if (availableQuantity < orderItem.getQuantity()) {
                return ResponseEntity.badRequest()
                        .body(new OrderResponseDTO("failed", "Недостатньо товару з id " + orderItem.getId()));
            }

            totalPrice += product.getPrice() * orderItem.getQuantity();
        }

        BigDecimal totalPriceBD = BigDecimal.valueOf(totalPrice);

        if (user.getBalance() == null || user.getBalance().compareTo(totalPriceBD) < 0) {
            return ResponseEntity.badRequest()
                    .body(new OrderResponseDTO("failed", "Недостатньо коштів на балансі"));
        }

        // 3. Знімаємо баланс
        user.setBalance(user.getBalance().subtract(totalPriceBD));
        usersRepository.save(user);

        for (OrderItemDto orderItem : orderItems) {
            Product product = products.stream()
                    .filter(p -> p.getId().equals(orderItem.getId()))
                    .findFirst()
                    .get();

            List<Account> accountsForOrder = product.getAccounts().subList(0, orderItem.getQuantity());
            orderCredentials.put(product.getId(), accountsForOrder);

            List<Account> remainingAccounts = product.getAccounts().subList(orderItem.getQuantity(), product.getAccounts().size());
            product.setAccounts(new ArrayList<>(remainingAccounts));

            productRepository.save(product);
        }

        Orders order = new Orders();
        order.setUsers(user);
        order.setPrice(totalPrice);
        order.setCredentials(orderCredentials);
        order.setOrderDate(LocalDateTime.now());

        ordersRepository.save(order);
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("✅ *Ваше замовлення успішно оформлено!*\n\n");

        for (OrderItemDto orderItem : orderItems) {
            Product product = products.stream()
                    .filter(p -> p.getId().equals(orderItem.getId()))
                    .findFirst()
                    .get();

            messageBuilder.append("📦 *").append(product.getName()).append("* - ").append(orderItem.getQuantity()).append(" шт.\n");

            List<Account> accountsForOrder = orderCredentials.get(product.getId());
            for (Account account : accountsForOrder) {
                messageBuilder.append("   🔑 Логін: `").append(account.getUsername()).append("`\n");
                messageBuilder.append("   🔒 Пароль: `").append(account.getPassword()).append("`\n");
                messageBuilder.append("   🌐 User-Agent: `").append(account.getUserAgent()).append("`\n");

                if (account.getFile() != null && !account.getFile().isEmpty()) {
                    messageBuilder.append("   📎 Посилання на файл: [Відкрити](").append(account.getFile()).append(")\n");
                }

                messageBuilder.append("\n");
            }

        }

        String message = messageBuilder.toString();

        telegramBotService.sendText(telegramId, message);

        return ResponseEntity.ok(new OrderResponseDTO("ok", "Замовлення успішно оформлено. Номер замовлення: " + order.getId()));
    }


    public List<ProductWithAccounts> getOrderDetails(String orderId) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Замовлення не знайдено"));

        Map<String, List<Account>> productIdToAccounts = order.getCredentials();

        List<String> productIds = new ArrayList<>(productIdToAccounts.keySet());
        List<Product> products = productRepository.findAllByIdIn(productIds);

        List<ProductWithAccounts> result = new ArrayList<>();
        for (Product product : products) {
            product.setAccounts(null);
            List<Account> accounts = productIdToAccounts.get(product.getId());
            result.add(new ProductWithAccounts(product, accounts));
        }

        return result;
    }


    public ResponseEntity<?> getOrdersByTelegramId(String initData) throws Exception {

        Map<String, Object> userMap = getUserData(initData);

        if (!userMap.containsKey("user")) {
            return ResponseEntity.badRequest()
                    .body(new OrderResponseDTO("failed", "Невірні дані користувача"));
        }

        Map<String, Object> userInfo = (Map<String, Object>) userMap.get("user");
        Long telegramId = ((Number) userInfo.get("id")).longValue();

        Optional<Users> userOptional = usersRepository.findByTelegramId(telegramId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "failed", "message", "Користувача не знайдено"));
        }

        Users user = userOptional.get();
        List<Orders> orders = ordersRepository.findByUsers_Id(user.getId());

        List<Map<String, Object>> orderList = orders.stream().map(order -> {
            Map<String, Object> orderData = new HashMap<>();
            orderData.put("orderId", order.getId());
            orderData.put("orderDate", order.getOrderDate());
            orderData.put("price", order.getPrice());
            return orderData;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("status", "ok", "orders", orderList));
    }



    private Map<String,Object> getUserData(String initData) throws Exception {
        Map<String, String> parsedParams = parseInitData(initData);
        String receivedHash = parsedParams.remove("hash");

        String dataCheckString = parsedParams.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("\n"));

        byte[] secretKey = hmacSha256("WebAppData".getBytes(StandardCharsets.UTF_8), BOT_TOKEN.getBytes(StandardCharsets.UTF_8));
        String calculatedHash = bytesToHex(hmacSha256(secretKey, dataCheckString.getBytes(StandardCharsets.UTF_8)));

        if (calculatedHash.equals(receivedHash)) {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> userMap = mapper.readValue(parsedParams.get("user"), Map.class);


            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("status", "ok");
            userInfo.put("user", userMap);
            return userInfo;
        } else {
            return Map.of("status", "error", "message", "Invalid hash");
        }
    }

    private Map<String, String> parseInitData(String initData) throws Exception {
        Map<String, String> map = new HashMap<>();
        String[] pairs = initData.split("&");
        for (String pair : pairs) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                map.put(parts[0], URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
            }
        }
        return map;
    }

    private byte[] hmacSha256(byte[] key, byte[] data) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec spec = new SecretKeySpec(key, "HmacSHA256");
        hmac.init(spec);
        return hmac.doFinal(data);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }


}
