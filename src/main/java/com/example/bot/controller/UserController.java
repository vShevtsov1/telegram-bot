package com.example.bot.controller;

import com.example.bot.service.UsersService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user")
public class UserController {


    @Autowired
    private UsersService usersService;
    @Value("${telegrambots.bots[0].token}")
    private String BOT_TOKEN;


    @PostMapping
    public Map<String,Object> getUserRefferalCode(@RequestParam String initData) {
        try {
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

                Number idNumber = (Number) userMap.get("id");
                userMap.put("ref",usersService.getUserReferralCode(idNumber.longValue()));

                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("status", "ok");
                userInfo.put("user", userMap);
                return userInfo;
            } else {
                return Map.of("status", "error", "message", "Invalid hash");
            }
        } catch (Exception e) {
            return Map.of("status", "error", "message", e.getMessage());
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
