package com.outforpavan.orderflow.learning;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LearningController {

    @GetMapping("/api/learning/status")
    public LearningStatus status() {
        return new LearningStatus("orderflow", "Learning Spring Boot one step at a time");
    }

    public record LearningStatus(String application, String message) {
    }
}
