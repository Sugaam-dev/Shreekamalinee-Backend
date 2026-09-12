package com.pmrgsolution.core.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
 * Enterprise Application TimeZone Configuration.
 * Enforces unified Indian Standard Time (IST - Asia/Kolkata) across all
 * database transactions, JPA timestamps, and Jackson JSON date serializations.
 */
@Configuration
public class AppTimeZoneConfig {

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
    }
}
