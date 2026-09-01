package com.pmrgsolution.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Exposes local uploads folder to HTTP GET requests
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
