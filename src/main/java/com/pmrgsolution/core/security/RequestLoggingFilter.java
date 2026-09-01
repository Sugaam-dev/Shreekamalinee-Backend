package com.pmrgsolution.core.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Slf4j
@Component
public class RequestLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String uri = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();
        
        // Log incoming requests while masking sensitive data in query parameters (password, token, secret, otp, signature)
        String queryString = httpRequest.getQueryString();
        String maskedQuery = "";
        if (queryString != null) {
            maskedQuery = queryString.replaceAll("(?i)(password|token|secret|otp|signature|utrNumber)=[^&]+", "$1=******");
        }
        
        log.info("HTTP Request: {} {}{}", method, uri, maskedQuery.isEmpty() ? "" : "?" + maskedQuery);
        
        chain.doFilter(request, response);
    }
}
