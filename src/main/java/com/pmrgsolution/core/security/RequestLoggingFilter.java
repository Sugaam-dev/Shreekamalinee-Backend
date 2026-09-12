package com.pmrgsolution.core.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest httpRequest, HttpServletResponse httpResponse, FilterChain filterChain)
            throws ServletException, IOException {
        
        String uri = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();
        
        // Log incoming requests while masking sensitive data in query parameters (password, token, secret, otp, signature)
        String queryString = httpRequest.getQueryString();
        String maskedQuery = "";
        if (queryString != null) {
            maskedQuery = queryString.replaceAll("(?i)(password|token|secret|otp|signature|utrNumber)=[^&]+", "$1=******");
        }
        
        log.info("HTTP Request: {} {}{}", method, uri, maskedQuery.isEmpty() ? "" : "?" + maskedQuery);
        
        filterChain.doFilter(httpRequest, httpResponse);
    }
}
