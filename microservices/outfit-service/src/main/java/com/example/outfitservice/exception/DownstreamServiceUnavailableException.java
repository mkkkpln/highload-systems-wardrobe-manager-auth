package com.example.outfitservice.exception;

import lombok.Getter;

/**
 * Используется для ответа 503 при недоступности внешнего сервиса,
 * с явным указанием состояния circuit breaker.
 */
@Getter
public class DownstreamServiceUnavailableException extends RuntimeException {

    private final String downstream;
    private final String circuitBreakerName;
    private final String circuitBreakerState;

    public DownstreamServiceUnavailableException(
            String downstream,
            String circuitBreakerName,
            String circuitBreakerState,
            String message,
            Throwable cause
    ) {
        super(message, cause);
        this.downstream = downstream;
        this.circuitBreakerName = circuitBreakerName;
        this.circuitBreakerState = circuitBreakerState;
    }
}


