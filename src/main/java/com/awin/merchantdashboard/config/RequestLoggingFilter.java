package com.awin.merchantdashboard.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerMapping;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startedAt = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            logCompleted(
                    request,
                    response,
                    Duration.ofNanos(System.nanoTime() - startedAt).toMillis());
        }
    }

    private void logCompleted(HttpServletRequest request, HttpServletResponse response, long durationMs) {
        int status = response.getStatus();
        LoggingEventBuilder event = status >= 500 ? log.atError() : log.atInfo();
        if (status >= 500) {
            Throwable serverError = serverError(request);
            if (serverError != null) {
                event.setCause(serverError);
            }
        }
        event.addKeyValue("method", request.getMethod())
                .addKeyValue("path", pathTemplate(request))
                .addKeyValue("status", status)
                .addKeyValue("durationMs", durationMs)
                .addKeyValue("merchantId", merchantId(request))
                .log("request");
    }

    private static Throwable serverError(HttpServletRequest request) {
        Throwable dispatched = exception(request.getAttribute(RequestDispatcher.ERROR_EXCEPTION));
        if (dispatched != null) {
            return dispatched;
        }
        return exception(request.getAttribute(DispatcherServlet.EXCEPTION_ATTRIBUTE));
    }

    private static Throwable exception(Object value) {
        if (value instanceof Throwable error) {
            return error;
        }
        return null;
    }

    private static String pathTemplate(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pattern instanceof String value && !value.isBlank()) {
            return value;
        }
        return request.getRequestURI();
    }

    private static String merchantId(HttpServletRequest request) {
        Object variables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (variables instanceof Map<?, ?> map) {
            Object id = map.get("merchantId");
            if (id != null) {
                return id.toString();
            }
        }
        return "";
    }
}
