package com.example.yuimagesearchmcpserver.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/healthy")
public class healthy {
    @GetMapping("/ok")
    public String searchImage() {
        return "ok";
    }
}
