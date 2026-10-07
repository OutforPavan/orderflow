package com.outforpavan.orderflow.learning;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("learning")
public record LearningProperties(@NotBlank String message) {
}
