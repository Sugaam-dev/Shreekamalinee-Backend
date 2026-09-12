package com.pmrgsolution.core.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.TimeZone;

/**
 * Enterprise Application TimeZone Pre-Initializer.
 * Runs before any DataSource, Flyway, or Hibernate connection is created,
 * ensuring the JVM and PostgreSQL JDBC driver use 'Asia/Kolkata' instead of Windows 'Asia/Calcutta'.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TimeZoneEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        System.setProperty("user.timezone", "Asia/Kolkata");
    }
}
