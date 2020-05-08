package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@RestController
public class HousingSalesDataAction {

    @Value("${Pipeline.fileName}")
    private String fileName;

    @CrossOrigin
    @GetMapping("/housingSalesData")
    public String housingSalesData() throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(fileName));
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
