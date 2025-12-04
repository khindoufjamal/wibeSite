package com.stet.t2s.sabr.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class SagRetryConfig {

    @Bean
    public RetryTemplate sagRetryTemplate() {
        RetryTemplate template = new RetryTemplate();

        // 3 tentatives sur NoSagAvailableException
        Map<Class<? extends Throwable>, Boolean> retryable = new HashMap<>();
        retryable.put(NoSagAvailableException.class, true);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3, retryable);
        template.setRetryPolicy(retryPolicy);

        FixedBackOffPolicy backOff = new FixedBackOffPolicy();
        backOff.setBackOffPeriod(10_000L); // 10s entre les tentatives
        template.setBackOffPolicy(backOff);

        return template;  // -> nom du bean = "sagRetryTemplate"
    }
}
