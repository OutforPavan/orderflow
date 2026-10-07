package com.outforpavan.orderflow.learning;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LearningController {

    private final LearningService learningService;

    public LearningController(LearningService learningService) {
        this.learningService = learningService;
    }

    @GetMapping("/api/learning/status")
    public LearningStatus status() {
        return new LearningStatus("orderflow", learningService.message());
    }

    public record LearningStatus(String application, String message) {
    }
}
