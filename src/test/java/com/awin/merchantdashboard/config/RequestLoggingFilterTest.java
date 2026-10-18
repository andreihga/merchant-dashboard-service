package com.awin.merchantdashboard.config;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.RequestDispatcher;
import java.util.Map;
import org.instancio.Instancio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerMapping;

class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();
    private final Logger logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void attachAppender() {
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
    }

    @Test
    void given_authenticated_request_when_filter_runs_then_logs_request_line_without_secrets() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/merchants/1001");
        request.setQueryString("access_token=secret-token");
        request.addHeader("Authorization", "Bearer secret-token");
        request.addParameter("password", "secret-password");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/v1/merchants/{merchantId}");
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of("merchantId", "1001"));

        filter.doFilter(
                request,
                new MockHttpServletResponse(),
                (servletRequest, servletResponse) -> ((MockHttpServletResponse) servletResponse).setStatus(200));

        ILoggingEvent event = appender.list.getFirst();
        assertThat(event.getLevel().toString()).isEqualTo("INFO");
        assertThat(event.getMessage()).isEqualTo("request");
        assertThat(event.getThrowableProxy()).isNull();
        assertThat(event.getKeyValuePairs())
                .anySatisfy(pair -> {
                    assertThat(pair.key).isEqualTo("method");
                    assertThat(pair.value).isEqualTo("GET");
                })
                .anySatisfy(pair -> {
                    assertThat(pair.key).isEqualTo("path");
                    assertThat(pair.value).isEqualTo("/api/v1/merchants/{merchantId}");
                })
                .anySatisfy(pair -> {
                    assertThat(pair.key).isEqualTo("status");
                    assertThat(pair.value).isEqualTo(200);
                })
                .anySatisfy(pair -> {
                    assertThat(pair.key).isEqualTo("merchantId");
                    assertThat(pair.value).isEqualTo("1001");
                });
        assertThat(event.toString()).doesNotContain("secret-token", "secret-password");
    }

    @Test
    void given_client_error_when_filter_runs_then_does_not_log_stack_trace() throws Exception {
        Throwable failure = Instancio.create(IllegalStateException.class);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/token");
        request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, failure);
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(401);

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {});

        ILoggingEvent event = appender.list.getFirst();
        assertThat(event.getLevel().toString()).isEqualTo("INFO");
        assertThat(event.getThrowableProxy()).isNull();
        assertThat(String.valueOf(event.getKeyValuePairs()))
                .doesNotContain(failure.getClass().getName());
    }

    @Test
    void given_server_error_when_filter_runs_then_logs_stack_trace() throws Exception {
        Throwable failure = Instancio.create(IllegalStateException.class);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/boom");
        request.setAttribute(DispatcherServlet.EXCEPTION_ATTRIBUTE, failure);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            ((MockHttpServletResponse) servletResponse).setStatus(500);
        });

        ILoggingEvent event = appender.list.getFirst();
        assertThat(event.getLevel().toString()).isEqualTo("ERROR");
        assertThat(event.getThrowableProxy()).isNotNull();
        assertThat(event.getThrowableProxy().getClassName())
                .isEqualTo(failure.getClass().getName());
        assertThat(event.getKeyValuePairs()).anySatisfy(pair -> {
            assertThat(pair.key).isEqualTo("path");
            assertThat(pair.value).isEqualTo("/boom");
        });
    }

    @Test
    void given_dispatched_server_error_when_filter_runs_then_uses_dispatched_exception() throws Exception {
        Throwable failure = Instancio.create(IllegalArgumentException.class);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/boom");
        request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, failure);

        filter.doFilter(request, new MockHttpServletResponse(), (servletRequest, servletResponse) -> {
            ((MockHttpServletResponse) servletResponse).setStatus(503);
        });

        ILoggingEvent event = appender.list.getFirst();
        assertThat(event.getThrowableProxy().getClassName()).isEqualTo(IllegalArgumentException.class.getName());
    }
}
