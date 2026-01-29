package com.flashSale.inventory.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InventoryController {
    @GetMapping("ping")
    public String ping(){
        return "inventory pong";
    }
}
