package com.musicapp.acceptance;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.kafka.KafkaContainer;

/**
 * Boots the full application on a random port against throw-away MySQL and Kafka containers.
 * The external music API is replaced by WireMock so scenarios are deterministic.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("acceptance")
public class CucumberSpringConfiguration {

    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("musicapp")
            .withUsername("musicapp")
            .withPassword("musicapp");

    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:3.8.0");

    static final WireMockServer MUSIC_API = new WireMockServer(options().dynamicPort());

    static {
        MYSQL.start();
        KAFKA.start();
        MUSIC_API.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("musicapp.providers.audius.base-url", MUSIC_API::baseUrl);
    }
}
