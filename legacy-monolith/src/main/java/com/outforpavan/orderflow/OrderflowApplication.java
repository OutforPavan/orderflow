package com.outforpavan.orderflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.outforpavan.orderflow.learning.LearningProperties;

@SpringBootApplication
@EnableConfigurationProperties(LearningProperties.class)
public class OrderflowApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderflowApplication.class, args);
	}

}
