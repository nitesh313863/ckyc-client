package com.example.ckyc.config;

import com.example.ckyc.exception.CkycUpstreamException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.ExceptionClassifierRetryPolicy;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class RetryConfig {

    @Bean
    public RetryTemplate ckycRetryTemplate(CkycProperties ckycProperties) {
        RetryTemplate template = new RetryTemplate();

        ExceptionClassifierRetryPolicy classifierPolicy = new ExceptionClassifierRetryPolicy();
        SimpleRetryPolicy retryablePolicy = new SimpleRetryPolicy(Math.max(1, ckycProperties.getRetry().getMaxAttempts()));
        NeverRetryPolicy neverRetryPolicy = new NeverRetryPolicy();

        classifierPolicy.setExceptionClassifier(throwable -> {
            if (throwable instanceof CkycUpstreamException upstreamException && upstreamException.isRetryable()) {
                return retryablePolicy;
            }
            return neverRetryPolicy;
        });

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(Math.max(1L, ckycProperties.getRetry().getInitialBackoffMs()));
        backOffPolicy.setMultiplier(Math.max(1.0d, ckycProperties.getRetry().getMultiplier()));
        backOffPolicy.setMaxInterval(Math.max(1L, ckycProperties.getRetry().getMaxBackoffMs()));

        template.setRetryPolicy(classifierPolicy);
        template.setBackOffPolicy(backOffPolicy);
        return template;
    }
}
