package com.accessvault.security;

import io.github.bucket4j.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;  // ← Add these imports
import java.util.UUID;
import java.io.IOException;
import java.time.Duration;

@Component
public class RateLimiterFilter extends OncePerRequestFilter {
    // ▼ Add this logger ▼
    private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("AUDIT_LOGGER");
    
    private final Bucket apiBucket;
    private final Bucket exportBucket;

    public RateLimiterFilter() {
        Bandwidth apiLimit = Bandwidth.classic(10, 
            Refill.intervally(10, Duration.ofMinutes(1)));
        
        Bandwidth exportLimit = Bandwidth.classic(2,
            Refill.intervally(2, Duration.ofMinutes(1)));

        this.apiBucket = Bucket.builder().addLimit(apiLimit).build();
        this.exportBucket = Bucket.builder().addLimit(exportLimit).build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain chain)
            throws IOException, ServletException {
        
        try {
            MDC.put("requestId", UUID.randomUUID().toString());
            MDC.put("userIp", request.getRemoteAddr());
            
            Bucket bucket = request.getRequestURI().startsWith("/api/logs/export") 
                ? exportBucket 
                : apiBucket;

            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            
            if (!probe.isConsumed()) {
                AUDIT_LOGGER.warn("Rate limit exceeded for {} from IP {}", 
                    request.getRequestURI(), 
                    request.getRemoteAddr());  // ← Enhanced log
                MDC.put("rateLimit", "true");
                response.setStatus(429);
                response.getWriter().write("Too many requests - try again later");
                return;
            }
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}