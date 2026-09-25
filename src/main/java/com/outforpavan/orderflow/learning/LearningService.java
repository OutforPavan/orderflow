package com.outforpavan.orderflow.learning;

import org.springframework.stereotype.Service;

@Service
public class LearningService {

    private final LearningProperties properties;

    public LearningService(LearningProperties properties) {
        this.properties = properties;
    }

    public String message() {
        return properties.message();
    }
}
