package com.outforpavan.orderflow.learning;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LearningDependencyInjectionTest {

    @Test
    void failsToStartWhenTheRequiredServiceIsNotRegistered() {
        try (var context = new AnnotationConfigApplicationContext()) {
            // Deliberately register only the controller to demonstrate a missing dependency.
            context.register(LearningController.class);

            assertThatThrownBy(context::refresh)
                    .isInstanceOf(UnsatisfiedDependencyException.class)
                    .hasRootCauseInstanceOf(NoSuchBeanDefinitionException.class)
                    .hasMessageContaining("LearningService");
        }
    }
}
