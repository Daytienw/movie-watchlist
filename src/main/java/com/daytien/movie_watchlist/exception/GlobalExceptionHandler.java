package com.daytien.movie_watchlist.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static ProblemDetail problem(HttpStatus status, String detail, String description) {
        ProblemDetail errorDetail = ProblemDetail.forStatusAndDetail(status, detail);
        errorDetail.setProperty("description", description);
        return errorDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }

        ProblemDetail errorDetail = problem(HttpStatus.BAD_REQUEST, "Request validation failed",
                "One or more fields are invalid");
        errorDetail.setProperty("errors", fieldErrors);
        return errorDetail;
    }

    /**
     * Raised by @Validated constraints on request parameters, as opposed to
     * MethodArgumentNotValidException which covers @Valid request bodies.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            String path = violation.getPropertyPath().toString();
            String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
            fieldErrors.putIfAbsent(field, violation.getMessage());
        }

        ProblemDetail errorDetail = problem(HttpStatus.BAD_REQUEST, "Request validation failed",
                "One or more request parameters are invalid");
        errorDetail.setProperty("errors", fieldErrors);
        return errorDetail;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParameter(MissingServletRequestParameterException exception) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage(),
                "A required request parameter is missing");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException exception) {
        // The raw message can echo back JSON parser internals, so keep it generic.
        return problem(HttpStatus.BAD_REQUEST, "Request body is missing or malformed",
                "The request body could not be parsed, or a field holds an invalid value");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException exception) {
        return problem(HttpStatus.UNAUTHORIZED, "Invalid email or password",
                "The username or password is incorrect");
    }

    @ExceptionHandler(AccountStatusException.class)
    public ProblemDetail handleAccountStatus(AccountStatusException exception) {
        return problem(HttpStatus.FORBIDDEN, exception.getMessage(), "The account is locked");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException exception) {
        return problem(HttpStatus.FORBIDDEN, exception.getMessage(),
                "You are not authorized to access this resource");
    }

    @ExceptionHandler(SignatureException.class)
    public ProblemDetail handleInvalidSignature(SignatureException exception) {
        return problem(HttpStatus.FORBIDDEN, "Invalid authentication token",
                "The JWT signature is invalid");
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ProblemDetail handleExpiredJwt(ExpiredJwtException exception) {
        return problem(HttpStatus.FORBIDDEN, "Authentication token has expired",
                "The JWT token has expired");
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage(),
                "The requested resource was not found");
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicate(DuplicateResourceException exception) {
        return problem(HttpStatus.CONFLICT, exception.getMessage(),
                "That resource already exists");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        // Backstop for races that slip past the explicit duplicate checks.
        log.warn("Database constraint violated", exception);
        return problem(HttpStatus.CONFLICT, "That resource already exists",
                "The request conflicts with existing data");
    }

    @ExceptionHandler(OmdbException.class)
    public ProblemDetail handleOmdb(OmdbException exception) {
        return problem(HttpStatus.BAD_GATEWAY, exception.getMessage(),
                "The OMDb API could not fulfill this request");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception) {
        // Log the detail server-side; never hand an internal message to the client.
        log.error("Unhandled exception", exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred",
                "Unknown internal server error.");
    }
}
