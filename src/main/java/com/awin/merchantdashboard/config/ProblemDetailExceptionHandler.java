package com.awin.merchantdashboard.config;

import com.awin.merchantdashboard.api.model.Problem;
import java.net.URI;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Returns RFC 7807 {@code application/problem+json} for MVC exceptions (validation failures,
 * type mismatches, {@code ResponseStatusException}, and the rest of the Spring defaults),
 * using the OpenAPI {@link Problem} schema.
 */
@RestControllerAdvice
class ProblemDetailExceptionHandler extends ResponseEntityExceptionHandler {

    private static final URI ABOUT_BLANK = URI.create("about:blank");

    @Override
    protected ResponseEntity<Object> createResponseEntity(
            @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problemDetail) {
            headers = HttpHeaders.copyOf(headers);
            headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
            body = toProblem(problemDetail);
        }
        return new ResponseEntity<>(body, headers, statusCode);
    }

    private static Problem toProblem(ProblemDetail detail) {
        URI type = detail.getType() != null ? detail.getType() : ABOUT_BLANK;
        return new Problem()
                .type(type)
                .title(detail.getTitle())
                .status(detail.getStatus())
                .detail(detail.getDetail())
                .instance(detail.getInstance());
    }
}
