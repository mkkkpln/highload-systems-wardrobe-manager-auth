package com.example.wardrobeservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@SpringBootApplication(exclude = HibernateJpaAutoConfiguration.class)
@EnableDiscoveryClient
@EnableR2dbcRepositories
public class WardrobeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WardrobeServiceApplication.class, args);
    }
}
