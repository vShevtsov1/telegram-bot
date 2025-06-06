package com.example.bot.controller;

import com.example.bot.model.CallbackData;
import com.example.bot.service.MonoService;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/callback")
public class CallbackController {

    @Autowired
    private MonoService monoService;
    @PostMapping
    public ResponseEntity<Void> handleCallback(@RequestBody CallbackData callbackData) {
        monoService.topUpProcess(callbackData);
        return ResponseEntity.ok().build();
    }
}
