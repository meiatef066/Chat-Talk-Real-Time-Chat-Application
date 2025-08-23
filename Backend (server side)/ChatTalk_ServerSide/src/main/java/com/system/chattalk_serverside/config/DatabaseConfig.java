package com.system.chattalk_serverside.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@Slf4j
public class DatabaseConfig {

    @Bean
    public CommandLineRunner databaseConnectionTest(@Autowired DataSource dataSource, @Autowired JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                log.info("Testing database connection...");
                log.info("Database URL: {}", dataSource.getConnection().getMetaData().getURL());
                log.info("Database Product: {}", dataSource.getConnection().getMetaData().getDatabaseProductName());
                log.info("Database Version: {}", dataSource.getConnection().getMetaData().getDatabaseProductVersion());

                // Test a simple query
                String result = jdbcTemplate.queryForObject("SELECT 'Database connection successful!' as message", String.class);
                log.info("Database test result: {}", result);

            } catch (Exception e) {
                log.error("Database connection failed: {}", e.getMessage(), e);
                throw e;
            }
        };
    }
}