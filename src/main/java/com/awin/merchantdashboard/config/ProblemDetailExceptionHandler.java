package com.awin.merchantdashboard.config;

import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Returns RFC 7807 {@code application/problem+json} for MVC exceptions (validation failures,
 * type mismatches, {@code ResponseStatusException}, and the rest of the Spring defaults).
 */
@RestControllerAdvice
class ProblemDetailExceptionHandler extends ResponseEntityExceptionHandler {}
