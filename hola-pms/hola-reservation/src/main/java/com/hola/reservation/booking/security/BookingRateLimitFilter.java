package com.hola.reservation.booking.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 부킹엔진 API IP 기반 Rate Limiter
 * - 슬라이딩 윈도우: windowSeconds 초당 maxRequests 건
 * - ConcurrentHashMap 기반 in-memory (단일 인스턴스용)
 */
@Slf4j
public class BookingRateLimitFilter extends OncePerRequestFilter {

    private final int maxRequests;
    private final long windowMillis;
    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public BookingRateLimitFilter(int maxRequests, int windowSeconds) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowSeconds * 1000L;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String clientIp = resolveClientIp(request);
        long now = System.currentTimeMillis();

        WindowCounter counter = counters.compute(clientIp, (key, existing) -> {
            if (existing == null || now - existing.windowStart > windowMillis) {
                return new WindowCounter(now);
            }
            return existing;
        });

        int count = counter.count.incrementAndGet();

        if (count > maxRequests) {
            log.warn("[RateLimit] IP {} 초과: {}/{} ({}초 윈도우)", clientIp, count, maxRequests, windowMillis / 1000);
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setHeader("Retry-After", String.valueOf(windowMillis / 1000));
            response.getWriter().write(
                    "{\"success\":false,\"code\":\"HOLA-4091\",\"message\":\"요청이 너무 많습니다. 잠시 후 다시 시도해주세요.\"}");
            return;
        }

        filterChain.doFilter(request, response);

        // 주기적 정리 (1000건 초과 시 만료된 엔트리 제거)
        if (counters.size() > 1000) {
            counters.entrySet().removeIf(e -> now - e.getValue().windowStart > windowMillis);
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class WindowCounter {
        final long windowStart;
        final AtomicInteger count;

        WindowCounter(long windowStart) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(0);
        }
    }
}
