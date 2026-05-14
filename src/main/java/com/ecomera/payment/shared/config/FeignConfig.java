package com.ecomera.payment.shared.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {

    private static final String X_USER_ID = "X-User-Id";
    private static final String X_USER_ROLES = "X-User-Roles";

    @Bean
    public RequestInterceptor userHeaderInterceptor() {
        return (RequestTemplate template) -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                var request = attributes.getRequest();
                String userId = request.getHeader(X_USER_ID);
                String roles = request.getHeader(X_USER_ROLES);
                if (userId != null) {
                    template.header(X_USER_ID, userId);
                }
                if (roles != null) {
                    template.header(X_USER_ROLES, roles);
                }
            }
        };
    }
}
