package com.metergate.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class ApiKeyValidationFilter implements GlobalFilter, Ordered {

    public static final String API_KEY_HEADER = "X-API-KEY";

    private final WebClient customerWebClient;
    private final WebClient limiterWebClient;

    // Injecting load-balanced WebClient builder from WebClientConfig
    public ApiKeyValidationFilter(WebClient.Builder webClientBuilder) {
        this.customerWebClient = webClientBuilder.baseUrl("http://customer-plan-service").build();
        this.limiterWebClient = webClientBuilder.baseUrl("http://rate-limiter-service").build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. Actuator health/info endpoints do not require an API Key
        if (path.startsWith("/actuator")) {
            return chain.filter(exchange);
        }

        // 2. Extract X-API-KEY header from incoming request
        String rawApiKey = request.getHeaders().getFirst(API_KEY_HEADER);
        if (rawApiKey == null || rawApiKey.isBlank()) {
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Missing X-API-KEY header. Please provide a valid API key.");
        }

        // 3. STAGE 1: Validate key against customer-plan-service
        return customerWebClient.post()
                .uri("/api/v1/customers/keys/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("apiKey", rawApiKey))
                .retrieve()
                .bodyToMono(ValidateKeyResponse.class)
                .flatMap(validationResponse -> {
                    // Check if key is active
                    if (!validationResponse.valid() || !"ACTIVE".equalsIgnoreCase(validationResponse.status())) {
                        return onError(exchange, HttpStatus.UNAUTHORIZED, "Invalid or revoked API key.");
                    }

                    // 4. STAGE 2: Call rate-limiter-service with user's plan limits
                    RateLimitRequest limiterRequest = new RateLimitRequest(
                            validationResponse.apiKeyId(),
                            validationResponse.rateLimitPerMinute(),
                            validationResponse.monthlyQuota(),
                            validationResponse.quotaPolicy()
                    );

                    return limiterWebClient.post()
                            .uri("/limiter/check")
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(limiterRequest)
                            .exchangeToMono(clientResponse -> {

                                // If Rate Limiter responded with 429 Too Many Requests
                                if (clientResponse.statusCode().equals(HttpStatus.TOO_MANY_REQUESTS)) {
                                    String retryAfter = clientResponse.headers().asHttpHeaders().getFirst("Retry-After");
                                    if (retryAfter != null) {
                                        exchange.getResponse().getHeaders().add("Retry-After", retryAfter);
                                    }
                                    return onError(exchange, HttpStatus.TOO_MANY_REQUESTS,
                                            "Rate limit exceeded. Please wait before retrying.");
                                }

                                // If Rate Limiter responded with 403 Forbidden (Quota Exceeded)
                                if (clientResponse.statusCode().equals(HttpStatus.FORBIDDEN)) {
                                    return onError(exchange, HttpStatus.FORBIDDEN,
                                            "Monthly quota exceeded for your plan. Please upgrade your plan.");
                                }

                                // If Rate Limiter responded with 200 OK -> Read body & forward
                                if (clientResponse.statusCode().is2xxSuccessful()) {
                                    return clientResponse.bodyToMono(RateLimitResponse.class)
                                            .flatMap(rateLimitResponse -> {
                                                exchange.getResponse().getHeaders().add("X-RateLimit-Limit",
                                                        String.valueOf(rateLimitResponse.limit()));
                                                exchange.getResponse().getHeaders().add("X-RateLimit-Remaining",
                                                        String.valueOf(rateLimitResponse.remaining()));
                                                return chain.filter(exchange);
                                            });
                                }

                                // Any other internal server error from rate limiter
                                return onError(exchange, HttpStatus.INTERNAL_SERVER_ERROR,
                                        "Unexpected error from rate limiting service.");
                            });
                })
                .onErrorResume(error -> {
                    System.err.println(">>> [GATEWAY ERROR IN FILTER]: " + error.getMessage());
                    error.printStackTrace();
                    return onError(exchange, HttpStatus.SERVICE_UNAVAILABLE,
                            "Authentication or rate limiting service is temporarily unavailable. Please try again later.");
                });
    }

    /**
     * Helper to write a clean JSON error response back to the client.
     */
    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String jsonError = String.format("{\"error\":\"%s\",\"message\":\"%s\",\"status\":%d}",
                status.getReasonPhrase(), message, status.value());

        byte[] bytes = jsonError.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);

        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}