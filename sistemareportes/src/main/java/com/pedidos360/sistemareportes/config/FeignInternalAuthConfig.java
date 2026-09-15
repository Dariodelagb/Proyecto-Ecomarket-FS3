package com.pedidos360.sistemareportes.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignInternalAuthConfig {
    @Bean
    public RequestInterceptor internalReportKeyInterceptor(
        @Value("${pedidos360.reportes.internal-key:pedidos360-internal}") String reportKey
    ) {
        return request -> request.header("X-Report-Key", reportKey);
    }
}
