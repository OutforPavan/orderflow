package com.outforpavan.orderflow.orders;

import com.outforpavan.orderflow.security.ServiceSecurityConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.outforpavan.orderflow.orders", "com.outforpavan.orderflow.pricing"})
@Import(ServiceSecurityConfiguration.class)
@EnableScheduling
public class OrderApplication {
    public static void main(String[] args) { SpringApplication.run(OrderApplication.class, args); }
}
