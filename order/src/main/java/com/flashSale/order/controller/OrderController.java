package com.flashSale.order.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {
    @GetMapping("ping")
    public String ping(){
        return "order pong";
    }
}
