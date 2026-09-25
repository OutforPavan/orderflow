package com.outforpavan.orderflow.learning;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import static org.assertj.core.api.Assertions.assertThat;

class LearningConfigurationTest {
    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void suppliesExternalConfigurationToTheService() {
        context.withPropertyValues("learning.message=Configured in a test").run(application -> {
            assertThat(application).hasNotFailed();
            assertThat(application.getBean(LearningService.class).message()).isEqualTo("Configured in a test");
        });
    }

    @Test
    void rejectsABlankRequiredMessageAtStartup() {
        context.withPropertyValues("learning.message= ").run(application -> {
            assertThat(application).hasFailed();
            assertThat(application.getStartupFailure()).hasRootCauseInstanceOf(BindValidationException.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(LearningProperties.class)
    @Import(LearningService.class)
    static class TestConfiguration {
    }
}
